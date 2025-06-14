package org.lsposed.lspatch.loader;

import android.app.ActivityThread;
import android.app.LoadedApk;
import android.content.res.XResources;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class LSPLoader {
    private static final String TAG = "LSPLoader";
    
    public static void initModules(LoadedApk loadedApk) {
        XposedInit.loadedPackagesInProcess.add(loadedApk.getPackageName());
        XResources.setPackageNameForResDir(loadedApk.getPackageName(), loadedApk.getResDir());
        XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam(
                XposedBridge.sLoadedPackageCallbacks);
        lpparam.packageName = loadedApk.getPackageName();
        lpparam.processName = ActivityThread.currentProcessName();
        lpparam.classLoader = loadedApk.getClassLoader();
        lpparam.appInfo = loadedApk.getApplicationInfo();
        lpparam.isFirstApplication = true;
        XC_LoadPackage.callAll(lpparam);
        
        // Check if we should load embedded WaEnhancer
        initEmbeddedWaEnhancer(loadedApk, lpparam);
    }
    
    /**
     * Initialize embedded WaEnhancer if configured for WhatsApp
     */
    private static void initEmbeddedWaEnhancer(LoadedApk loadedApk, XC_LoadPackage.LoadPackageParam lpparam) {
        String packageName = loadedApk.getPackageName();
        
        // Only load for WhatsApp packages
        if (!"com.whatsapp".equals(packageName) && !"com.whatsapp.w4b".equals(packageName)) {
            return;
        }
        
        try {
            // Check if WaEnhancer is embedded in the APK
            ClassLoader classLoader = loadedApk.getClassLoader();
            Class<?> waenhancerClass = classLoader.loadClass("com.wmods.wppenhacer.WppXposed");
            
            android.util.Log.i(TAG, "Found embedded WaEnhancer, initializing...");
            
            // Create instance of WaEnhancer
            @SuppressWarnings("deprecation")
            Object waenhancerInstance = waenhancerClass.newInstance();
            
            // Call the handleLoadPackage method
            java.lang.reflect.Method handleLoadPackageMethod = waenhancerClass.getMethod(
                "handleLoadPackage", XC_LoadPackage.LoadPackageParam.class);
            handleLoadPackageMethod.invoke(waenhancerInstance, lpparam);
            
            android.util.Log.i(TAG, "WaEnhancer initialized successfully");
            
        } catch (ClassNotFoundException e) {
            // WaEnhancer not embedded, this is normal
            android.util.Log.d(TAG, "WaEnhancer not found in embedded modules");
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to initialize embedded WaEnhancer: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
