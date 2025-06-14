package org.lsposed.lspatch.loader;

import static org.lsposed.lspatch.share.Constants.ORIGINAL_APK_ASSET_PATH;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.Signature;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;

import com.google.gson.JsonSyntaxException;

import org.lsposed.lspatch.loader.util.XLog;
import org.lsposed.lspatch.share.Constants;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class SigBypass {

    private static final String TAG = "LSPatch-SigBypass";
    private static final Map<String, String> signatures = new HashMap<>();

    private static void replaceSignature(Context context, PackageInfo packageInfo) {
        // Use modern SigningInfo API for Android P+ (API 28+)
        boolean hasSignature = packageInfo.signingInfo != null;
        
        // For compatibility, also check legacy signatures field using reflection to avoid deprecation warnings
        if (!hasSignature) {
            try {
                // Use reflection to access deprecated signatures field
                java.lang.reflect.Field signaturesField = PackageInfo.class.getDeclaredField("signatures");
                signaturesField.setAccessible(true);
                android.content.pm.Signature[] legacySignatures = (android.content.pm.Signature[]) signaturesField.get(packageInfo);
                hasSignature = legacySignatures != null && legacySignatures.length > 0;
            } catch (Exception e) {
                // Ignore reflection errors
            }
        }
        
        if (hasSignature) {
            String packageName = packageInfo.packageName;
            String replacement = signatures.get(packageName);
            if (replacement == null && !signatures.containsKey(packageName)) {
                try {
                    var metaData = context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData;
                    String encoded = null;
                    if (metaData != null) encoded = metaData.getString("lspatch");
                    if (encoded != null) {
                        var json = new String(Base64.decode(encoded, Base64.DEFAULT), StandardCharsets.UTF_8);
                        try {
                            var patchConfig = new JSONObject(json);
                            replacement = patchConfig.getString("originalSignature");
                        } catch (JSONException e) {
                            Log.w(TAG, "fail to get originalSignature", e);
                        }
                    }
                } catch (PackageManager.NameNotFoundException | JsonSyntaxException ignored) {
                }
                signatures.put(packageName, replacement);
            }
            if (replacement != null) {
                // Prefer modern SigningInfo API first
                if (packageInfo.signingInfo != null) {
                    XLog.d(TAG, "Replace signature info for `" + packageName + "` (modern method)");
                    Signature[] signaturesArray = packageInfo.signingInfo.getApkContentsSigners();
                    if (signaturesArray != null && signaturesArray.length > 0) {
                        signaturesArray[0] = new Signature(replacement);
                    }
                }
                // Fallback to legacy signatures field using reflection to avoid deprecation warnings
                else {
                    try {
                        java.lang.reflect.Field signaturesField = PackageInfo.class.getDeclaredField("signatures");
                        signaturesField.setAccessible(true);
                        android.content.pm.Signature[] legacySignatures = (android.content.pm.Signature[]) signaturesField.get(packageInfo);
                        if (legacySignatures != null && legacySignatures.length > 0) {
                            XLog.d(TAG, "Replace signature info for `" + packageName + "` (legacy method)");
                            legacySignatures[0] = new Signature(replacement);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to replace signature using reflection", e);
                    }
                }
            }
        }
    }

    private static void hookPackageParser(Context context) {
        XposedBridge.hookAllMethods(PackageParser.class, "generatePackageInfo", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                var packageInfo = (PackageInfo) param.getResult();
                if (packageInfo == null) return;
                replaceSignature(context, packageInfo);
            }
        });
    }

    private static void proxyPackageInfoCreator(Context context) {
        Parcelable.Creator<PackageInfo> originalCreator = PackageInfo.CREATOR;
        Parcelable.Creator<PackageInfo> proxiedCreator = new Parcelable.Creator<>() {
            @Override
            public PackageInfo createFromParcel(Parcel source) {
                PackageInfo packageInfo = originalCreator.createFromParcel(source);
                replaceSignature(context, packageInfo);
                return packageInfo;
            }

            @Override
            public PackageInfo[] newArray(int size) {
                return originalCreator.newArray(size);
            }
        };
        XposedHelpers.setStaticObjectField(PackageInfo.class, "CREATOR", proxiedCreator);
        try {
            Map<?, ?> mCreators = (Map<?, ?>) XposedHelpers.getStaticObjectField(Parcel.class, "mCreators");
            mCreators.clear();
        } catch (NoSuchFieldError ignore) {
        } catch (Throwable e) {
            Log.w(TAG, "fail to clear Parcel.mCreators", e);
        }
        try {
            Map<?, ?> sPairedCreators = (Map<?, ?>) XposedHelpers.getStaticObjectField(Parcel.class, "sPairedCreators");
            sPairedCreators.clear();
        } catch (NoSuchFieldError ignore) {
        } catch (Throwable e) {
            Log.w(TAG, "fail to clear Parcel.sPairedCreators", e);
        }
    }

    static void doSigBypass(Context context, int sigBypassLevel) throws IOException {
        if (sigBypassLevel >= Constants.SIGBYPASS_LV_PM) {
            hookPackageParser(context);
            proxyPackageInfoCreator(context);
        }
        if (sigBypassLevel >= Constants.SIGBYPASS_LV_PM_OPENAT) {
            String cacheApkPath;
            try (ZipFile sourceFile = new ZipFile(context.getPackageResourcePath())) {
                cacheApkPath = context.getCacheDir() + "/lspatch/origin/" + sourceFile.getEntry(ORIGINAL_APK_ASSET_PATH).getCrc() + ".apk";
            }
            org.lsposed.lspd.nativebridge.SigBypass.enableOpenatHook(context.getPackageResourcePath(), cacheApkPath);
        }
    }
}
