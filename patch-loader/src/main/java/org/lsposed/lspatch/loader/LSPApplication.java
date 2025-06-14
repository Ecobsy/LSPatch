package org.lsposed.lspatch.loader;

import static org.lsposed.lspatch.share.Constants.CONFIG_ASSET_PATH;
import static org.lsposed.lspatch.share.Constants.ORIGINAL_APK_ASSET_PATH;

import android.app.ActivityThread;
import android.app.LoadedApk;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.os.Build;
import android.os.RemoteException;
import android.system.Os;
import android.util.Log;

import org.lsposed.lspatch.loader.util.FileUtils;
import org.lsposed.lspatch.loader.util.XLog;
import org.lsposed.lspatch.service.LocalApplicationService;
import org.lsposed.lspatch.service.RemoteApplicationService;
import org.lsposed.lspatch.compat.LSPatchCompat;
import org.lsposed.lspatch.compat.LSPatchBridge;
import org.lsposed.lspatch.compat.LSPatchFeatureValidator;
import org.lsposed.lspd.core.Startup;
import org.lsposed.lspd.service.ILSPApplicationService;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import de.robv.android.xposed.XposedHelpers;
import hidden.HiddenApiBridge;

/**
 * Created by Windysha
 */
@SuppressWarnings("unused")
public class LSPApplication {

    private static final String TAG = "LSPatch";
    private static final int FIRST_APP_ZYGOTE_ISOLATED_UID = 90000;
    private static final int PER_USER_RANGE = 100000;

    private static ActivityThread activityThread;
    private static LoadedApk stubLoadedApk;
    private static LoadedApk appLoadedApk;

    private static JSONObject config;

    public static boolean isIsolated() {
        return (android.os.Process.myUid() % PER_USER_RANGE) >= FIRST_APP_ZYGOTE_ISOLATED_UID;
    }

    public static void onLoad() throws RemoteException, IOException {
        if (isIsolated()) {
            XLog.d(TAG, "Skip isolated process");
            return;
        }
        activityThread = ActivityThread.currentActivityThread();
        var context = createLoadedApkWithContext();
        if (context == null) {
            XLog.e(TAG, "Error when creating context");
            return;
        }

        Log.d(TAG, "Initialize service client");
        ILSPApplicationService service;
        ILSPApplicationService localService = null;
        ILSPApplicationService remoteService = null;
        
        if (config.optBoolean("useManager")) {
            Log.i(TAG, "Using RemoteApplicationService (Manager mode)");
            service = new RemoteApplicationService(context);
            remoteService = service;
        } else {
            Log.i(TAG, "Using LocalApplicationService (Embedded mode)");
            service = new LocalApplicationService(context);
            localService = service;
        }

        // Initialize enhanced diagnostics service
        initializeDiagnostics(context, localService, remoteService);

        // Initialize LSPatch compatibility layer
        initializeLSPatchCompatibility(context, service);
        
        // Validate module execution capabilities
        validateModuleExecution(context, service);

        disableProfile(context);
        Startup.initXposed(false, ActivityThread.currentProcessName(), context.getApplicationInfo().dataDir, service);
        Startup.bootstrapXposed();
        
        // Initialize LSPatch bridge after Xposed bootstrap
        initializeLSPatchBridge(context, service);
        
        // WARN: Since it uses `XResource`, the following class should not be initialized
        // before forkPostCommon is invoke. Otherwise, you will get failure of XResources
        Log.i(TAG, "Load modules");
        LSPLoader.initModules(appLoadedApk);
        Log.i(TAG, "Modules initialized");

        // Generate compatibility report for debugging
        generateCompatibilityReport(context, localService, remoteService);

        switchAllClassLoader();
        SigBypass.doSigBypass(context, config.optInt("sigBypassLevel"));

        Log.i(TAG, "LSPatch bootstrap completed");
    }

    /**
     * Initialize enhanced diagnostics for module compatibility
     */
    private static void initializeDiagnostics(Context context, 
                                            ILSPApplicationService localService,
                                            ILSPApplicationService remoteService) {
        try {
            // Log enhanced initialization information
            Log.i(TAG, "=== LSPatch Enhanced Diagnostics ===");
            Log.i(TAG, "Process: " + ActivityThread.currentProcessName());
            Log.i(TAG, "Package: " + context.getPackageName());
            Log.i(TAG, "LSPatch Mode: " + (config.optBoolean("useManager") ? "Manager" : "Embedded"));
            Log.i(TAG, "API Level: " + android.os.Build.VERSION.SDK_INT);
            Log.i(TAG, "Architecture: " + System.getProperty("os.arch"));
            
            // Set comprehensive system properties for module detection
            setEnhancedSystemProperties();
            
            Log.i(TAG, "Diagnostics initialization completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during diagnostics initialization: " + e.getMessage());
        }
    }

    /**
     * Set enhanced system properties for better module detection
     */
    private static void setEnhancedSystemProperties() {
        try {
            // Set LSPatch detection properties
            System.setProperty("lspatch.initialized", "true");
            System.setProperty("lspatch.bootstrap", "completed");
            System.setProperty("lspatch.process", ActivityThread.currentProcessName());
            
            if (config.optBoolean("useManager")) {
                System.setProperty("lspatch.mode", "manager");
                System.setProperty("lspatch.manager", "true");
                System.setProperty("lspatch.remote", "true");
            } else {
                System.setProperty("lspatch.mode", "embedded");
                System.setProperty("lspatch.embedded", "true");
                System.setProperty("lspatch.local", "true");
            }
            
            // Set signature bypass level for module reference
            int sigBypassLevel = config.optInt("sigBypassLevel", 0);
            System.setProperty("lspatch.sigbypass.level", String.valueOf(sigBypassLevel));
            
            // Set debuggable flag
            boolean debuggable = config.optBoolean("debuggable", false);
            System.setProperty("lspatch.debuggable", String.valueOf(debuggable));
            
            Log.d(TAG, "Enhanced system properties set for module detection");
            
        } catch (Exception e) {
            Log.w(TAG, "Could not set all enhanced system properties: " + e.getMessage());
        }
    }

    /**
     * Generate compatibility report for advanced modules like WaEnhancer
     */
    private static void generateCompatibilityReport(Context context,
                                                   ILSPApplicationService localService,
                                                   ILSPApplicationService remoteService) {
        try {
            // Import diagnostics service
            var diagnostics = new org.lsposed.lspatch.service.LSPatchModuleDiagnostics(
                context, localService, remoteService);
            
            // Generate comprehensive report
            String report = diagnostics.generateWaEnhancerCompatibilityReport();
            
            // Log report in chunks to avoid truncation
            String[] lines = report.split("\n");
            Log.i(TAG, "=== COMPATIBILITY REPORT START ===");
            for (String line : lines) {
                Log.i(TAG, line);
            }
            Log.i(TAG, "=== COMPATIBILITY REPORT END ===");
            
            // Log health status
            var healthStatus = diagnostics.getHealthStatus();
            Log.i(TAG, "LSPatch Health Status: " + healthStatus.overall);
            Log.i(TAG, "- LSPatch Detected: " + healthStatus.lspatchDetected);
            Log.i(TAG, "- WhatsApp Context: " + healthStatus.whatsappContext);
            Log.i(TAG, "- WaEnhancer Detected: " + healthStatus.waenhancerDetected);
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating compatibility report: " + e.getMessage());
        }
    }

    /**
     * Initialize LSPatch compatibility layer for advanced modules
     */
    private static void initializeLSPatchCompatibility(Context context, ILSPApplicationService service) {
        try {
            Log.i(TAG, "Initializing LSPatch compatibility layer");
            
            // Initialize compatibility detection and features
            LSPatchCompat.init(context);
            
            // Log compatibility information for debugging
            LSPatchCompat.logCompatibilityInfo();
            
            Log.i(TAG, "LSPatch compatibility layer initialized");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing LSPatch compatibility: " + e.getMessage());
        }
    }

    /**
     * Initialize LSPatch bridge for module integration
     */
    private static void initializeLSPatchBridge(Context context, ILSPApplicationService service) {
        try {
            Log.i(TAG, "Initializing LSPatch bridge");
            
            // Initialize bridge for advanced module support
            boolean bridgeInitialized = LSPatchBridge.initialize(context, service);
            
            if (bridgeInitialized) {
                Log.i(TAG, "LSPatch bridge initialized successfully");
                
                // Log bridge status for debugging
                LSPatchBridge.logBridgeStatus();
                
                // Run feature validation for comprehensive compatibility checking
                runFeatureValidation(context.getClassLoader());
                
            } else {
                Log.w(TAG, "LSPatch bridge initialization failed");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing LSPatch bridge: " + e.getMessage());
        }
    }

    /**
     * Validate module execution capabilities
     */
    private static void validateModuleExecution(Context context, ILSPApplicationService service) {
        try {
            Log.i(TAG, "Validating module execution capabilities");
            
            // Get modules list for validation
            var modules = service.getLegacyModulesList();
            if (modules == null || modules.isEmpty()) {
                Log.w(TAG, "No modules found for validation");
                return;
            }
            
            Log.i(TAG, "Validating " + modules.size() + " modules");
            
            // Use reflection to call the Kotlin validator
            try {
                Class<?> validatorClass = Class.forName("org.lsposed.lspatch.loader.validation.ModuleExecutionValidator");
                var companionField = validatorClass.getField("Companion");
                var companion = companionField.get(null);
                
                var validateMethod = companion.getClass().getMethod("validateModuleExecution", 
                    android.content.Context.class, 
                    org.lsposed.lspd.service.ILSPApplicationService.class, 
                    java.util.List.class);
                
                var result = validateMethod.invoke(companion, context, service, modules);
                
                // Get validation summary
                var getSummaryMethod = result.getClass().getMethod("getSummary");
                var summary = (String) getSummaryMethod.invoke(result);
                
                Log.i(TAG, "Module validation result:");
                String[] lines = summary.split("\n");
                for (String line : lines) {
                    Log.i(TAG, line);
                }
                
            } catch (Exception reflectionError) {
                Log.w(TAG, "Could not use Kotlin validator, using Java fallback: " + reflectionError.getMessage());
                validateModulesJavaFallback(context, service, modules);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error during module validation: " + e.getMessage());
        }
    }
    
    /**
     * Java fallback validation for modules
     */
    private static void validateModulesJavaFallback(Context context, ILSPApplicationService service, java.util.List<org.lsposed.lspd.models.Module> modules) {
        Log.i(TAG, "Using Java fallback module validation");
        
        int validModules = 0;
        int invalidModules = 0;
        
        for (var module : modules) {
            try {
                // Basic validation
                if (module.apkPath == null || module.apkPath.isEmpty()) {
                    Log.w(TAG, "Module " + module.packageName + ": Invalid APK path");
                    invalidModules++;
                    continue;
                }
                
                var moduleFile = new java.io.File(module.apkPath);
                if (!moduleFile.exists()) {
                    Log.w(TAG, "Module " + module.packageName + ": APK file does not exist");
                    invalidModules++;
                    continue;
                }
                
                if (module.file == null) {
                    Log.w(TAG, "Module " + module.packageName + ": PreLoadedApk is null");
                    invalidModules++;
                    continue;
                }
                
                if (module.file.preLoadedDexes.isEmpty()) {
                    Log.w(TAG, "Module " + module.packageName + ": No DEX files loaded");
                    invalidModules++;
                    continue;
                }
                
                Log.i(TAG, "Module " + module.packageName + ": Validation passed (" + 
                      module.file.preLoadedDexes.size() + " DEX files, " + 
                      module.file.moduleClassNames.size() + " classes)");
                validModules++;
                
            } catch (Exception e) {
                Log.e(TAG, "Error validating module " + module.packageName + ": " + e.getMessage());
                invalidModules++;
            }
        }
        
        Log.i(TAG, "Module validation summary: " + validModules + " valid, " + invalidModules + " invalid");
        
        // Test XposedBridge functionality
        try {
            de.robv.android.xposed.XposedBridge.log("LSPatch module validation test");
            Log.i(TAG, "XposedBridge functionality test: PASSED");
        } catch (Exception e) {
            Log.w(TAG, "XposedBridge functionality test: FAILED - " + e.getMessage());
        }
    }

    /**
     * Run comprehensive feature validation for module compatibility
     */
    private static void runFeatureValidation(ClassLoader classLoader) {
        try {
            Log.i(TAG, "Running LSPatch feature validation");
            
            // Validate all LSPatch features
            var validationResults = LSPatchFeatureValidator.validateAllFeatures(classLoader);
            
            // Log validation report
            LSPatchFeatureValidator.logValidationReport();
            
            // Get compatibility summary
            var summary = LSPatchFeatureValidator.getCompatibilitySummary();
            Log.i(TAG, "Feature compatibility: " + summary.getOverallStatus() + 
                " (" + String.format("%.1f", summary.getCompatibilityPercentage()) + "% compatible)");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during feature validation: " + e.getMessage());
        }
    }

    private static Context createLoadedApkWithContext() {
        try {
            var mBoundApplication = XposedHelpers.getObjectField(activityThread, "mBoundApplication");

            stubLoadedApk = (LoadedApk) XposedHelpers.getObjectField(mBoundApplication, "info");
            var appInfo = (ApplicationInfo) XposedHelpers.getObjectField(mBoundApplication, "appInfo");
            var compatInfo = (CompatibilityInfo) XposedHelpers.getObjectField(mBoundApplication, "compatInfo");
            var baseClassLoader = stubLoadedApk.getClassLoader();

            try (var is = baseClassLoader.getResourceAsStream(CONFIG_ASSET_PATH)) {
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                config = new JSONObject(streamReader.lines().collect(Collectors.joining()));
            } catch (Throwable e) {
                Log.e(TAG, "Failed to parse config file", e);
                return null;
            }
            Log.i(TAG, "Use manager: " + config.optBoolean("useManager"));
            Log.i(TAG, "Signature bypass level: " + config.optInt("sigBypassLevel"));

            Path originPath = Paths.get(appInfo.dataDir, "cache/lspatch/origin/");
            Path cacheApkPath;
            try (ZipFile sourceFile = new ZipFile(appInfo.sourceDir)) {
                cacheApkPath = originPath.resolve(sourceFile.getEntry(ORIGINAL_APK_ASSET_PATH).getCrc() + ".apk");
            }

            appInfo.sourceDir = cacheApkPath.toString();
            appInfo.publicSourceDir = cacheApkPath.toString();
            if (config.has("appComponentFactory")) {
                appInfo.appComponentFactory = config.optString("appComponentFactory");
            }

            if (!Files.exists(cacheApkPath)) {
                Log.i(TAG, "Extract original apk");
                FileUtils.deleteFolderIfExists(originPath);
                Files.createDirectories(originPath);
                try (InputStream is = baseClassLoader.getResourceAsStream(ORIGINAL_APK_ASSET_PATH)) {
                    Files.copy(is, cacheApkPath);
                }
            }
            cacheApkPath.toFile().setWritable(false);

            var mPackages = (Map<?, ?>) XposedHelpers.getObjectField(activityThread, "mPackages");
            mPackages.remove(appInfo.packageName);
            appLoadedApk = activityThread.getPackageInfoNoCheck(appInfo, compatInfo);
            XposedHelpers.setObjectField(mBoundApplication, "info", appLoadedApk);

            var activityClientRecordClass = XposedHelpers.findClass("android.app.ActivityThread$ActivityClientRecord", ActivityThread.class.getClassLoader());
            var fixActivityClientRecord = (BiConsumer<Object, Object>) (k, v) -> {
                if (activityClientRecordClass.isInstance(v)) {
                    var pkgInfo = XposedHelpers.getObjectField(v, "packageInfo");
                    if (pkgInfo == stubLoadedApk) {
                        Log.d(TAG, "fix loadedapk from ActivityClientRecord");
                        XposedHelpers.setObjectField(v, "packageInfo", appLoadedApk);
                    }
                }
            };
            var mActivities = (Map<?, ?>) XposedHelpers.getObjectField(activityThread, "mActivities");
            mActivities.forEach(fixActivityClientRecord);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    var mLaunchingActivities = (Map<?, ?>) XposedHelpers.getObjectField(activityThread, "mLaunchingActivities");
                    mLaunchingActivities.forEach(fixActivityClientRecord);
                }
            } catch (Throwable ignored) {
            }
            Log.i(TAG, "hooked app initialized: " + appLoadedApk);

            var context = (Context) XposedHelpers.callStaticMethod(Class.forName("android.app.ContextImpl"), "createAppContext", activityThread, stubLoadedApk);
            if (config.has("appComponentFactory")) {
                try {
                    context.getClassLoader().loadClass(appInfo.appComponentFactory);
                } catch (ClassNotFoundException e) { // This will happen on some strange shells like 360
                    Log.w(TAG, "Original AppComponentFactory not found: " + appInfo.appComponentFactory);
                    appInfo.appComponentFactory = null;
                }
            }
            return context;
        } catch (Throwable e) {
            Log.e(TAG, "createLoadedApk", e);
            return null;
        }
    }

    public static void disableProfile(Context context) {
        final ArrayList<String> codePaths = new ArrayList<>();
        var appInfo = context.getApplicationInfo();
        var pkgName = context.getPackageName();
        if (appInfo == null) return;
        if ((appInfo.flags & ApplicationInfo.FLAG_HAS_CODE) != 0) {
            codePaths.add(appInfo.sourceDir);
        }
        if (appInfo.splitSourceDirs != null) {
            Collections.addAll(codePaths, appInfo.splitSourceDirs);
        }

        if (codePaths.isEmpty()) {
            // If there are no code paths there's no need to setup a profile file and register with
            // the runtime,
            return;
        }

        var profileDir = HiddenApiBridge.Environment_getDataProfilesDePackageDirectory(appInfo.uid / PER_USER_RANGE, pkgName);

        var attrs = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("r--------"));

        for (int i = codePaths.size() - 1; i >= 0; i--) {
            String splitName = i == 0 ? null : appInfo.splitNames[i - 1];
            File curProfileFile = new File(profileDir, splitName == null ? "primary.prof" : splitName + ".split.prof").getAbsoluteFile();
            Log.d(TAG, "Processing " + curProfileFile.getAbsolutePath());
            try {
                if (!curProfileFile.exists()) {
                    Files.createFile(curProfileFile.toPath(), attrs);
                    continue;
                }
                if (!curProfileFile.canWrite() && Files.size(curProfileFile.toPath()) == 0) {
                    Log.d(TAG, "Skip profile " + curProfileFile.getAbsolutePath());
                    continue;
                }
                if (curProfileFile.exists() && !curProfileFile.delete()) {
                    try (var writer = new FileOutputStream(curProfileFile)) {
                        Log.d(TAG, "Failed to delete, try to clear content " + curProfileFile.getAbsolutePath());
                    } catch (Throwable e) {
                        Log.e(TAG, "Failed to delete and clear profile file " + curProfileFile.getAbsolutePath(), e);
                    }
                    Os.chmod(curProfileFile.getAbsolutePath(), 00400);
                }
            } catch (Throwable e) {
                Log.e(TAG, "Failed to disable profile file " + curProfileFile.getAbsolutePath(), e);
            }
        }
    }

    private static void switchAllClassLoader() {
        var fields = LoadedApk.class.getDeclaredFields();
        for (Field field : fields) {
            if (field.getType() == ClassLoader.class) {
                var obj = XposedHelpers.getObjectField(appLoadedApk, field.getName());
                XposedHelpers.setObjectField(stubLoadedApk, field.getName(), obj);
            }
        }
    }
}
