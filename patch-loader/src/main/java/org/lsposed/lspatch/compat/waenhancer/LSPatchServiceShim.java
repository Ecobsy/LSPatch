package org.lsposed.lspatch.compat.waenhancer;

import android.content.Context;
import android.util.Log;

import org.lsposed.lspatch.compat.LSPatchCompat;
import org.lsposed.lspatch.compat.LSPatchBridge;

/**
 * WaEnhancer LSPatch Service compatibility
 * 
 * This class provides the exact service APIs that WaEnhancer expects
 * for module detection and functionality testing.
 */
public class LSPatchServiceShim {
    
    private static final String TAG = "WaEnhancer-Service";
    
    /**
     * Check if WaEnhancer is functional in LSPatch environment
     */
    public static boolean isWaEnhancerFunctional() {
        try {
            // Test 1: Check if XposedBridge is working
            try {
                de.robv.android.xposed.XposedBridge.log("WaEnhancer functionality test");
            } catch (Exception e) {
                Log.w(TAG, "XposedBridge not functional: " + e.getMessage());
                return false;
            }
            
            // Test 2: Check if we can access WhatsApp core classes
            if (!canHookWhatsAppClasses()) {
                Log.w(TAG, "Cannot access WhatsApp classes for hooking");
                return false;
            }
            
            // Test 3: Check LSPatch compatibility
            if (!LSPatchCompat.validateLSPatchIntegrity()) {
                Log.w(TAG, "LSPatch integrity validation failed");
                return false;
            }
            
            // Test 4: Check bridge functionality
            if (LSPatchCompat.isLSPatchEnvironment() && !LSPatchBridge.isInitialized()) {
                Log.w(TAG, "LSPatch bridge not initialized");
                return false;
            }
            
            Log.i(TAG, "WaEnhancer functionality verified");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking WaEnhancer functionality: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if we can hook WhatsApp classes
     */
    private static boolean canHookWhatsAppClasses() {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                Context context = LSPatchCompat.getCurrentContext();
                if (context != null) {
                    cl = context.getClassLoader();
                }
            }
            
            if (cl == null) {
                return false;
            }
            
            // Try to access critical WhatsApp classes
            String[] testClasses = {
                "com.whatsapp.HomeActivity",
                "com.whatsapp.Main",
                "com.whatsapp.Conversation"
            };
            
            for (String className : testClasses) {
                try {
                    Class<?> clazz = cl.loadClass(className);
                    if (clazz != null) {
                        Log.d(TAG, "Successfully accessed WhatsApp class: " + className);
                        return true; // If we can access at least one core class, we're good
                    }
                } catch (ClassNotFoundException e) {
                    // Class not found, try next
                    continue;
                }
            }
            
            Log.w(TAG, "Could not access any WhatsApp core classes");
            return false;
            
        } catch (Exception e) {
            Log.w(TAG, "Error testing WhatsApp class access: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get current context (WaEnhancer API)
     */
    public static Context getCurrentContext() {
        return LSPatchCompat.getCurrentContext();
    }
    
    /**
     * Check if we're in WhatsApp context
     */
    public static boolean isInWhatsAppContext() {
        Context context = getCurrentContext();
        if (context == null) {
            return false;
        }
        
        String packageName = context.getPackageName();
        return "com.whatsapp".equals(packageName) || "com.whatsapp.w4b".equals(packageName);
    }
    
    /**
     * Check if LSPatch dex injection is present
     */
    public static boolean isLSPatchDexInjectionPresent() {
        try {
            // Enhanced LSPatch detection methods for WaEnhancer compatibility
            
            // Method 1: Check for enhanced LSPatch system properties
            String[] enhancedDexProperties = {
                "lspatch.initialized",
                "lspatch.bridge.active", 
                "lspatch.services.active",
                "lspatch.noroot",
                "lspatch.version",
                "lspatch.dex.injected",
                "lspatch.loader.injected", 
                "lspatch.hook.enabled",
                "lspatch.module.loaded",
                "lspatch.xposed.compatible",
                "ro.lspatch.enabled"
            };
            
            for (String prop : enhancedDexProperties) {
                String value = System.getProperty(prop);
                if ("true".equals(value) || (value != null && !value.isEmpty())) {
                    Log.d(TAG, "LSPatch detected via enhanced property: " + prop + " = " + value);
                    return true;
                }
            }
            
            // Method 2: Check for LSPatch-specific thread names (enhanced)
            java.util.Set<Thread> allThreads = Thread.getAllStackTraces().keySet();
            for (Thread thread : allThreads) {
                String threadName = thread.getName();
                if (threadName != null && (threadName.contains("LSPatch") || 
                                         threadName.contains("lspatch") ||
                                         threadName.contains("LSP-") ||
                                         threadName.contains("XposedBridge"))) {
                    Log.d(TAG, "LSPatch detected via enhanced thread: " + threadName);
                    return true;
                }
            }
            
            // Method 3: Check for LSPatch classes in classpath
            try {
                Class.forName("org.lsposed.lspatch.loader.LSPApplication");
                Log.d(TAG, "LSPatch detected via LSPApplication class");
                return true;
            } catch (ClassNotFoundException ignored) {}
            
            try {
                Class.forName("org.lsposed.lspatch.compat.LSPatchBridge");
                Log.d(TAG, "LSPatch detected via LSPatchBridge class");
                return true;
            } catch (ClassNotFoundException ignored) {}
            
            try {
                Class.forName("org.lsposed.lspatch.service.LSPatchServiceManager");
                Log.d(TAG, "LSPatch detected via LSPatchServiceManager class");
                return true;
            } catch (ClassNotFoundException ignored) {}
            
            // Method 4: Check for LSPatch bridge initialization
            try {
                Class<?> bridgeClass = Class.forName("org.lsposed.lspatch.compat.LSPatchBridge");
                java.lang.reflect.Method isInitializedMethod = bridgeClass.getMethod("isInitialized");
                Boolean initialized = (Boolean) isInitializedMethod.invoke(null);
                if (Boolean.TRUE.equals(initialized)) {
                    Log.d(TAG, "LSPatch detected via bridge initialization check");
                    return true;
                }
            } catch (Exception ignored) {}
            
            // Method 5: Check for XposedBridge with LSPatch modifications
            try {
                Class<?> xposedBridgeClass = Class.forName("de.robv.android.xposed.XposedBridge");
                // Check if XposedBridge has been initialized by LSPatch
                java.lang.reflect.Field disableHooksField = xposedBridgeClass.getDeclaredField("disableHooks");
                disableHooksField.setAccessible(true);
                boolean disableHooks = disableHooksField.getBoolean(null);
                if (!disableHooks) {
                    Log.d(TAG, "LSPatch detected via XposedBridge hooks enabled");
                    return true;
                }
            } catch (Exception ignored) {}
            
            // Method 6: Check for LSPatch file markers
            try {
                android.content.Context context = getActivityThreadContext();
                if (context != null) {
                    // Check for LSPatch cache directory
                    java.io.File lspatchCache = new java.io.File(context.getCacheDir(), "lspatch");
                    if (lspatchCache.exists()) {
                        Log.d(TAG, "LSPatch detected via cache directory");
                        return true;
                    }
                    
                    // Check for LSPatch logs directory
                    java.io.File lspatchLogs = new java.io.File(context.getCacheDir(), "lspatch_logs");
                    if (lspatchLogs.exists()) {
                        Log.d(TAG, "LSPatch detected via logs directory");
                        return true;
                    }
                    
                    // Check for original APK asset
                    try {
                        context.getAssets().open("lspatch/origin.apk");
                        Log.d(TAG, "LSPatch detected via origin APK asset");
                        return true;
                    } catch (java.io.IOException ignored) {}
                }
            } catch (Exception ignored) {}
            
            // Method 7: Check for LSPatch service manager
            try {
                Class<?> serviceManagerClass = Class.forName("org.lsposed.lspatch.service.LSPatchServiceManager");
                java.lang.reflect.Method getInstanceMethod = serviceManagerClass.getMethod("getInstance");
                Object instance = getInstanceMethod.invoke(null);
                java.lang.reflect.Method isInitializedMethod = instance.getClass().getMethod("isInitialized");
                Boolean initialized = (Boolean) isInitializedMethod.invoke(instance);
                if (Boolean.TRUE.equals(initialized)) {
                    Log.d(TAG, "LSPatch detected via service manager");
                    return true;
                }
            } catch (Exception ignored) {}
            
            return false;
            
        } catch (Exception e) {
            Log.w(TAG, "Error in enhanced LSPatch detection: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get current context using ActivityThread reflection
     */
    private static android.content.Context getActivityThreadContext() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            java.lang.reflect.Method currentActivityThreadMethod = 
                activityThreadClass.getMethod("currentActivityThread");
            Object activityThread = currentActivityThreadMethod.invoke(null);
            
            java.lang.reflect.Method getApplicationMethod = 
                activityThreadClass.getMethod("getApplication");
            return (android.content.Context) getApplicationMethod.invoke(activityThread);
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get LSPatch configuration value
     */
    public static String getLSPatchConfig(String key) {
        return LSPatchCompat.getLSPatchConfig(key);
    }
    
    /**
     * Check if LSPatch class is available
     */
    public static boolean isLSPatchClassAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Exception e) {
            Log.w(TAG, "Error checking class availability: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get current package name
     */
    public static String getCurrentPackageName() {
        Context context = getCurrentContext();
        return context != null ? context.getPackageName() : null;
    }
    
    /**
     * Log service status
     */
    public static void logServiceStatus() {
        Log.i(TAG, "=== WaEnhancer LSPatch Service Status ===");
        Log.i(TAG, "LSPatch Environment: " + LSPatchCompat.isLSPatchEnvironment());
        Log.i(TAG, "LSPatch Mode: " + LSPatchCompat.getCurrentMode());
        Log.i(TAG, "WhatsApp Context: " + isInWhatsAppContext());
        Log.i(TAG, "Package Name: " + getCurrentPackageName());
        Log.i(TAG, "WaEnhancer Functional: " + isWaEnhancerFunctional());
        Log.i(TAG, "Dex Injection Present: " + isLSPatchDexInjectionPresent());
        Log.i(TAG, "Bridge Initialized: " + LSPatchBridge.isInitialized());
        Log.i(TAG, "Service Available: " + LSPatchCompat.isLSPatchServiceAvailable());
        Log.i(TAG, "=======================================");
    }
}
