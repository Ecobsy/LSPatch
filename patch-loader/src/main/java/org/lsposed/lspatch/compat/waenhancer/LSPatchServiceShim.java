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
            // Check for LSPatch-specific system properties that indicate dex injection
            String[] dexProperties = {
                "lspatch.dex.injected",
                "lspatch.loader.injected", 
                "lspatch.hook.enabled",
                "lspatch.module.loaded"
            };
            
            for (String prop : dexProperties) {
                if ("true".equals(System.getProperty(prop))) {
                    Log.d(TAG, "LSPatch dex injection detected via property: " + prop);
                    return true;
                }
            }
            
            // Check for LSPatch-specific thread names
            java.util.Set<Thread> allThreads = Thread.getAllStackTraces().keySet();
            for (Thread thread : allThreads) {
                String threadName = thread.getName();
                if (threadName != null && (threadName.contains("LSPatch") || 
                                         threadName.contains("lspatch"))) {
                    Log.d(TAG, "LSPatch dex injection detected via thread: " + threadName);
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            Log.w(TAG, "Error checking LSPatch dex injection: " + e.getMessage());
            return false;
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
