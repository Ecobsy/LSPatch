package org.lsposed.lspatch.compat;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import org.lsposed.lspd.service.ILSPApplicationService;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XposedBridge;

/**
 * LSPatch Compatibility Layer
 * 
 * This class provides compatibility APIs that modules like WaEnhancer expect
 * from LSPatch environments. It serves as a bridge between LSPatch's internal
 * implementation and external module expectations.
 */
public class LSPatchCompat {
    
    private static final String TAG = "LSPatch-Compat";
    
    // LSPatch specific constants that modules expect
    private static final String LSPATCH_LOADER_CLASS = "org.lsposed.lspatch.loader.LSPApplication";
    private static final String LSPATCH_METALOADER_CLASS = "org.lsposed.lspatch.metaloader.LSPAppComponentFactoryStub";
    private static final String LSPATCH_SERVICE_CLASS = "org.lsposed.lspd.service.ILSPApplicationService";
    private static final String LSPATCH_BRIDGE_CLASS = "org.lsposed.lspd.core.Startup";
    private static final String LSPATCH_XPOSED_INIT = "org.lsposed.lspatch.loader.LSPLoader";
    
    private static Boolean isLSPatchEnvironment = null;
    private static LSPatchMode currentMode = null;
    private static Context applicationContext = null;
    
    public enum LSPatchMode {
        CLASSIC_XPOSED,           // Traditional Xposed with root
        LSPATCH_EMBEDDED,         // LSPatch with embedded modules
        LSPATCH_MANAGER           // LSPatch with manager
    }
    
    /**
     * Initialize LSPatch compatibility layer
     */
    public static void init(Context context) {
        applicationContext = context;
        // Trigger environment detection
        isLSPatchEnvironment();
        
        if (isLSPatchEnvironment) {
            Log.i(TAG, "Initializing LSPatch compatibility layer");
            
            // Apply LSPatch specific optimizations
            optimizeForLSPatch();
            
            // Set system properties for better integration
            setSystemProperties();
            
            Log.i(TAG, "LSPatch compatibility layer initialized - Mode: " + getCurrentMode());
        } else {
            Log.d(TAG, "LSPatch not detected, using classic Xposed mode");
        }
    }
    
    /**
     * Check if current environment is LSPatch
     */
    public static boolean isLSPatchEnvironment() {
        if (isLSPatchEnvironment != null) {
            return isLSPatchEnvironment;
        }
        
        isLSPatchEnvironment = detectLSPatchEnvironment();
        if (isLSPatchEnvironment) {
            currentMode = detectLSPatchMode();
        } else {
            currentMode = LSPatchMode.CLASSIC_XPOSED;
        }
        
        return isLSPatchEnvironment;
    }
    
    /**
     * Get the current LSPatch mode
     */
    public static LSPatchMode getCurrentMode() {
        if (currentMode == null) {
            isLSPatchEnvironment(); // Trigger detection
        }
        return currentMode != null ? currentMode : LSPatchMode.CLASSIC_XPOSED;
    }
    
    /**
     * Check if LSPatch service is available
     */
    public static boolean isLSPatchServiceAvailable() {
        if (!isLSPatchEnvironment()) {
            return false;
        }
        
        try {
            // Check if service classes are available
            return isClassAvailable(LSPATCH_SERVICE_CLASS);
        } catch (Exception e) {
            Log.w(TAG, "Error checking service availability: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if a specific LSPatch feature is available
     */
    public static boolean isFeatureAvailable(String feature) {
        if (!isLSPatchEnvironment()) {
            return true; // All features available in classic Xposed
        }
        
        switch (feature) {
            case "RESOURCE_HOOKS":
                return getCurrentMode() != LSPatchMode.LSPATCH_MANAGER;
            case "SYSTEM_SERVER_HOOKS":
                return false; // LSPatch doesn't support system server hooks
            case "SIGNATURE_BYPASS":
                return true; // LSPatch has built-in signature bypass
            case "BRIDGE_SERVICE":
                return getCurrentMode() == LSPatchMode.LSPATCH_EMBEDDED;
            case "XPOSED_BRIDGE":
                return isClassAvailable("de.robv.android.xposed.XposedBridge");
            case "MODULE_PATH":
                return true; // Module path handling is available
            case "PREFERENCES":
                return true; // Preferences handling is available
            default:
                return true;
        }
    }
    
    /**
     * Check if application is patched by LSPatch
     */
    public static boolean isApplicationPatched(Context context) {
        if (context == null) {
            context = applicationContext;
        }
        
        if (context == null) {
            return false;
        }
        
        try {
            ApplicationInfo appInfo = context.getApplicationInfo();
            
            // Check for LSPatch signatures in the APK
            if (appInfo.sourceDir != null) {
                File apkFile = new File(appInfo.sourceDir);
                if (apkFile.exists()) {
                    // Check for LSPatch markers
                    return hasLSPatchMarkers(appInfo);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking if application is patched: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get the module path for resource loading
     */
    public static String getModulePath() {
        if (applicationContext != null) {
            return applicationContext.getApplicationInfo().sourceDir;
        }
        return null;
    }
    
    /**
     * Validate LSPatch environment integrity
     */
    public static boolean validateLSPatchIntegrity() {
        if (!isLSPatchEnvironment()) {
            return false;
        }
        
        boolean integrity = true;
        
        // Check core LSPatch components
        if (!isClassAvailable(LSPATCH_LOADER_CLASS)) {
            Log.w(TAG, "LSPatch loader class not available");
            integrity = false;
        }
        
        // Check if basic Xposed functionality works
        try {
            XposedBridge.log("LSPatch integrity test");
        } catch (Exception e) {
            Log.w(TAG, "XposedBridge not functional: " + e.getMessage());
            integrity = false;
        }
        
        return integrity;
    }
    
    /**
     * Get current application context
     */
    public static Context getCurrentContext() {
        if (applicationContext != null) {
            return applicationContext;
        }
        
        // Try to get context through ActivityThread
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentApplicationMethod = activityThreadClass.getDeclaredMethod("currentApplication");
            Object application = currentApplicationMethod.invoke(null);
            
            if (application instanceof Context) {
                applicationContext = (Context) application;
                return applicationContext;
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not get context through ActivityThread: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get LSPatch configuration value
     */
    public static String getLSPatchConfig(String key) {
        String value = System.getProperty("lspatch." + key);
        if (value != null) {
            return value;
        }
        
        // Fallback to system properties without prefix
        return System.getProperty(key);
    }
    
    // Private helper methods
    
    private static boolean detectLSPatchEnvironment() {
        // Primary detection: Check for LSPatch loader classes
        if (isClassAvailable(LSPATCH_LOADER_CLASS) ||
            isClassAvailable(LSPATCH_METALOADER_CLASS)) {
            return true;
        }
        
        // Secondary detection: Check for LSPatch service classes
        if (isClassAvailable(LSPATCH_SERVICE_CLASS) ||
            isClassAvailable(LSPATCH_BRIDGE_CLASS)) {
            return true;
        }
        
        // Tertiary detection: Check for LSPatch system properties
        try {
            String lspatchMarker = System.getProperty("lspatch.initialized");
            if ("true".equals(lspatchMarker)) {
                return true;
            }
        } catch (Exception e) {
            // Ignore property access errors
        }
        
        // Check for LSPatch threads
        Set<Thread> allThreads = Thread.getAllStackTraces().keySet();
        for (Thread thread : allThreads) {
            String threadName = thread.getName();
            if (threadName != null && (threadName.contains("LSPatch") || 
                                     threadName.contains("lspatch"))) {
                return true;
            }
        }
        
        return false;
    }
    
    private static LSPatchMode detectLSPatchMode() {
        // Check system properties first
        String mode = System.getProperty("lspatch.mode");
        if ("manager".equals(mode)) {
            return LSPatchMode.LSPATCH_MANAGER;
        } else if ("embedded".equals(mode)) {
            return LSPatchMode.LSPATCH_EMBEDDED;
        }
        
        // Fallback detection based on available services
        if (isClassAvailable("org.lsposed.lspatch.service.RemoteApplicationService")) {
            return LSPatchMode.LSPATCH_MANAGER;
        } else if (isClassAvailable("org.lsposed.lspatch.service.LocalApplicationService")) {
            return LSPatchMode.LSPATCH_EMBEDDED;
        }
        
        return LSPatchMode.LSPATCH_EMBEDDED; // Default assumption
    }
    
    private static boolean isClassAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Exception e) {
            Log.d(TAG, "Error checking class availability: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean hasLSPatchMarkers(ApplicationInfo appInfo) {
        try {
            // Check for LSPatch specific files in the APK
            String dataDir = appInfo.dataDir;
            if (dataDir != null) {
                File lspatchDir = new File(dataDir, "cache/lspatch");
                if (lspatchDir.exists()) {
                    return true;
                }
            }
            
            // Check for LSPatch metadata
            if (appInfo.metaData != null) {
                return appInfo.metaData.containsKey("lspatch.embedded") ||
                       appInfo.metaData.containsKey("lspatch.version");
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking LSPatch markers: " + e.getMessage());
        }
        
        return false;
    }
    
    private static void optimizeForLSPatch() {
        Log.i(TAG, "Applying LSPatch optimizations");
        
        // Enable debug mode for better error reporting
        try {
            Class<?> debugClass = Class.forName("de.robv.android.xposed.XposedBridge");
            Method setDebugMethod = debugClass.getDeclaredMethod("setDebugMode", boolean.class);
            setDebugMethod.setAccessible(true);
            setDebugMethod.invoke(null, true);
        } catch (Exception e) {
            Log.d(TAG, "Could not set debug mode: " + e.getMessage());
        }
    }
    
    private static void setSystemProperties() {
        try {
            // Set LSPatch detection properties
            System.setProperty("lspatch.compat.enabled", "true");
            System.setProperty("lspatch.compat.version", "1.0");
            
            // Set mode-specific properties
            LSPatchMode mode = getCurrentMode();
            System.setProperty("lspatch.compat.mode", mode.name());
            
            if (mode == LSPatchMode.LSPATCH_MANAGER) {
                System.setProperty("lspatch.manager", "true");
                System.setProperty("lspatch.remote", "true");
            } else if (mode == LSPatchMode.LSPATCH_EMBEDDED) {
                System.setProperty("lspatch.embedded", "true");
                System.setProperty("lspatch.local", "true");
            }
            
            Log.d(TAG, "LSPatch compatibility system properties set");
            
        } catch (Exception e) {
            Log.w(TAG, "Could not set all compatibility properties: " + e.getMessage());
        }
    }
    
    /**
     * Log compatibility information for debugging
     */
    public static void logCompatibilityInfo() {
        Log.i(TAG, "=== LSPatch Compatibility Info ===");
        Log.i(TAG, "LSPatch Environment: " + isLSPatchEnvironment());
        Log.i(TAG, "Current Mode: " + getCurrentMode());
        Log.i(TAG, "Service Available: " + isLSPatchServiceAvailable());
        Log.i(TAG, "Resource Hooks: " + isFeatureAvailable("RESOURCE_HOOKS"));
        Log.i(TAG, "System Server Hooks: " + isFeatureAvailable("SYSTEM_SERVER_HOOKS"));
        Log.i(TAG, "Signature Bypass: " + isFeatureAvailable("SIGNATURE_BYPASS"));
        Log.i(TAG, "Bridge Service: " + isFeatureAvailable("BRIDGE_SERVICE"));
        Log.i(TAG, "XposedBridge: " + isFeatureAvailable("XPOSED_BRIDGE"));
        Log.i(TAG, "=====================================");
    }
}
