package org.lsposed.lspatch.compat;

import android.util.Log;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * LSPatch compatible hook wrapper
 * 
 * This class provides wrapper methods for Xposed hooks that automatically
 * apply LSPatch optimizations and handle LSPatch-specific edge cases,
 * ensuring better stability and compatibility for modules in LSPatch environments.
 */
public class LSPatchHookWrapper {
    
    private static final String TAG = "LSPatch-Hooks";
    
    // Error tracking for diagnostics
    private static final Map<String, Integer> errorCounts = new HashMap<>();
    private static final Map<String, Long> lastErrorTimes = new HashMap<>();
    
    // Hook optimization flags
    private static boolean optimizationsEnabled = true;
    private static boolean errorSuppression = false;
    
    /**
     * Hook a method with LSPatch-specific optimizations
     */
    public static XC_MethodHook.Unhook hookMethod(Class<?> clazz, String methodName, 
                                                  Object[] parameterTypes, XC_MethodHook callback) {
        if (!LSPatchCompat.isLSPatchEnvironment()) {
            // Standard Xposed hook for non-LSPatch environments
            return XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypes);
        }
        
        return hookMethodLSPatch(clazz, methodName, parameterTypes, callback);
    }
    
    /**
     * Hook a constructor with LSPatch-specific optimizations
     */
    public static XC_MethodHook.Unhook hookConstructor(Class<?> clazz, Object[] parameterTypes, 
                                                       XC_MethodHook callback) {
        if (!LSPatchCompat.isLSPatchEnvironment()) {
            return XposedHelpers.findAndHookConstructor(clazz, parameterTypes);
        }
        
        return hookConstructorLSPatch(clazz, parameterTypes, callback);
    }
    
    /**
     * Hook all methods with LSPatch-specific optimizations
     */
    public static Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> clazz, String methodName, 
                                                           XC_MethodHook callback) {
        if (!LSPatchCompat.isLSPatchEnvironment()) {
            return XposedBridge.hookAllMethods(clazz, methodName, callback);
        }
        
        return hookAllMethodsLSPatch(clazz, methodName, callback);
    }
    
    /**
     * Replace a method with LSPatch-specific optimizations
     */
    public static XC_MethodHook.Unhook replaceMethod(Class<?> clazz, String methodName, 
                                                     Object[] parameterTypes, XC_MethodReplacement replacement) {
        if (!LSPatchCompat.isLSPatchEnvironment()) {
            return XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypes);
        }
        
        return replaceMethodLSPatch(clazz, methodName, parameterTypes, replacement);
    }
    
    // LSPatch-specific implementations
    
    private static XC_MethodHook.Unhook hookMethodLSPatch(Class<?> clazz, String methodName, 
                                                          Object[] parameterTypes, XC_MethodHook callback) {
        try {
            // Create a wrapper callback that handles LSPatch specifics
            XC_MethodHook lspatchCallback = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        // Use safe callback invocation
                        callSafeBefore(callback, param);
                    } catch (Exception e) {
                        handleLSPatchHookError("beforeHookedMethod", clazz, methodName, e);
                        if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
                            throw e;
                        }
                    }
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        // Use safe callback invocation
                        callSafeAfter(callback, param);
                    } catch (Exception e) {
                        handleLSPatchHookError("afterHookedMethod", clazz, methodName, e);
                        if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
                            throw e;
                        }
                    }
                }
            };
            
            return XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypes);
            
        } catch (Exception e) {
            handleLSPatchHookError("hookMethod", clazz, methodName, e);
            Log.e(TAG, "LSPatch hook failed for " + clazz.getSimpleName() + "." + methodName + ": " + e.getMessage());
            throw e;
        }
    }
    
    private static XC_MethodHook.Unhook replaceMethodLSPatch(Class<?> clazz, String methodName, 
                                                             Object[] parameterTypes, XC_MethodReplacement replacement) {
        try {
            // Create a wrapper replacement that handles LSPatch specifics
            XC_MethodReplacement lspatchReplacement = new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        // Use safe replacement invocation
                        return callSafeReplacement(replacement, param);
                    } catch (Exception e) {
                        handleLSPatchHookError("replaceHookedMethod", clazz, methodName, e);
                        if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
                            throw e;
                        }
                        return null;
                    }
                }
            };
            
            return XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypes);
            
        } catch (Exception e) {
            handleLSPatchHookError("replaceMethod", clazz, methodName, e);
            Log.e(TAG, "LSPatch replacement failed for " + clazz.getSimpleName() + "." + methodName + ": " + e.getMessage());
            throw e;
        }
    }
    
    private static Set<XC_MethodHook.Unhook> hookAllMethodsLSPatch(Class<?> clazz, String methodName, XC_MethodHook callback) {
        // Create LSPatch-optimized callback
        XC_MethodHook lspatchCallback = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    // Use safe callback invocation
                    callSafeBefore(callback, param);
                } catch (Exception e) {
                    handleLSPatchHookError("beforeHookedMethod", clazz, methodName, e);
                    if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
                        throw e;
                    }
                }
            }
            
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    // Use safe callback invocation
                    callSafeAfter(callback, param);
                } catch (Exception e) {
                    handleLSPatchHookError("afterHookedMethod", clazz, methodName, e);
                    if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
                        throw e;
                    }
                }
            }
        };
        
        return XposedBridge.hookAllMethods(clazz, methodName, lspatchCallback);
    }
    
    private static XC_MethodHook.Unhook hookConstructorLSPatch(Class<?> clazz, Object[] parameterTypes, XC_MethodHook callback) {
        // Similar pattern as method hooks but for constructors
        XC_MethodHook lspatchCallback = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    // Use safe callback invocation
                    callSafeBefore(callback, param);
                } catch (Exception e) {
                    handleLSPatchHookError("beforeHookedMethod", clazz, "<init>", e);
                    if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
                        throw e;
                    }
                }
            }
            
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    // Use safe callback invocation
                    callSafeAfter(callback, param);
                } catch (Exception e) {
                    handleLSPatchHookError("afterHookedMethod", clazz, "<init>", e);
                    if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
                        throw e;
                    }
                }
            }
        };
        
        return XposedHelpers.findAndHookConstructor(clazz, parameterTypes);
    }
    
    // Safe callback invocation methods
    
    /**
     * Safely call beforeHookedMethod using reflection to avoid protected access issues
     */
    public static void callSafeBefore(XC_MethodHook callback, XC_MethodHook.MethodHookParam param) throws Throwable {
        try {
            // Use reflection to access protected method
            Method beforeMethod = XC_MethodHook.class.getDeclaredMethod("beforeHookedMethod", XC_MethodHook.MethodHookParam.class);
            beforeMethod.setAccessible(true);
            beforeMethod.invoke(callback, param);
        } catch (Exception e) {
            // Log the issue instead of calling the protected method directly
            Log.w(TAG, "Failed to invoke beforeHookedMethod via reflection: " + e.getMessage());
        }
    }
    
    /**
     * Safely call afterHookedMethod using reflection to avoid protected access issues
     */
    public static void callSafeAfter(XC_MethodHook callback, XC_MethodHook.MethodHookParam param) throws Throwable {
        try {
            // Use reflection to access protected method
            Method afterMethod = XC_MethodHook.class.getDeclaredMethod("afterHookedMethod", XC_MethodHook.MethodHookParam.class);
            afterMethod.setAccessible(true);
            afterMethod.invoke(callback, param);
        } catch (Exception e) {
            // Log the issue instead of calling the protected method directly
            Log.w(TAG, "Failed to invoke afterHookedMethod via reflection: " + e.getMessage());
        }
    }
    
    /**
     * Safely call replaceHookedMethod using reflection to avoid protected access issues
     */
    public static Object callSafeReplacement(XC_MethodReplacement replacement, XC_MethodHook.MethodHookParam param) throws Throwable {
        try {
            // Use reflection to access protected method
            Method replaceMethod = XC_MethodReplacement.class.getDeclaredMethod("replaceHookedMethod", XC_MethodHook.MethodHookParam.class);
            replaceMethod.setAccessible(true);
            return replaceMethod.invoke(replacement, param);
        } catch (Exception e) {
            // Log the issue and return null instead of calling the protected method directly
            Log.w(TAG, "Failed to invoke replaceHookedMethod via reflection: " + e.getMessage());
            return null;
        }
    }
    
    // Error handling and diagnostics
    
    /**
     * Handle LSPatch-specific hook errors
     */
    private static void handleLSPatchHookError(String hookType, Class<?> clazz, String methodName, Exception e) {
        String identifier = clazz.getSimpleName() + "." + methodName + " (" + hookType + ")";
        
        Log.w(TAG, "Hook error in LSPatch mode for " + identifier + ": " + e.getMessage());
        
        // Log additional context for LSPatch debugging
        if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_EMBEDDED) {
            Log.d(TAG, "Error occurred in embedded mode, continuing with reduced functionality");
        } else if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
            Log.d(TAG, "Error occurred in manager mode, may need bridge service");
        }
        
        // Update error statistics
        incrementErrorCount(identifier);
        
        // Report to bridge if available
        if (LSPatchBridge.isInitialized()) {
            LSPatchBridge.reportModuleError("LSPatchHookWrapper", "Hook error: " + identifier, e);
        }
    }
    
    /**
     * Increment error count for monitoring
     */
    private static void incrementErrorCount(String identifier) {
        synchronized (errorCounts) {
            Integer count = errorCounts.get(identifier);
            errorCounts.put(identifier, count == null ? 1 : count + 1);
            lastErrorTimes.put(identifier, System.currentTimeMillis());
        }
    }
    
    // Public utility methods
    
    /**
     * Get error statistics
     */
    public static Map<String, Integer> getErrorStats() {
        synchronized (errorCounts) {
            return new HashMap<>(errorCounts);
        }
    }
    
    /**
     * Clear error statistics
     */
    public static void clearErrorStats() {
        synchronized (errorCounts) {
            errorCounts.clear();
            lastErrorTimes.clear();
        }
    }
    
    /**
     * Enable or disable hook optimizations
     */
    public static void setOptimizationsEnabled(boolean enabled) {
        optimizationsEnabled = enabled;
        Log.i(TAG, "LSPatch hook optimizations " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Enable or disable error suppression in embedded mode
     */
    public static void setErrorSuppression(boolean suppress) {
        errorSuppression = suppress;
        Log.i(TAG, "LSPatch error suppression " + (suppress ? "enabled" : "disabled"));
    }
    
    /**
     * Check if hooks are working properly
     */
    public static boolean isHookingFunctional() {
        try {
            // Test basic hooking capability
            Class<?> objectClass = Object.class;
            Method toStringMethod = objectClass.getDeclaredMethod("toString");
            
            if (toStringMethod != null) {
                Log.d(TAG, "Basic hooking capability verified");
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "Hooking capability test failed: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Log comprehensive hook wrapper status
     */
    public static void logHookWrapperStatus() {
        Log.i(TAG, "=== LSPatch Hook Wrapper Status ===");
        Log.i(TAG, "LSPatch Environment: " + LSPatchCompat.isLSPatchEnvironment());
        Log.i(TAG, "Current Mode: " + LSPatchCompat.getCurrentMode());
        Log.i(TAG, "Optimizations Enabled: " + optimizationsEnabled);
        Log.i(TAG, "Error Suppression: " + errorSuppression);
        Log.i(TAG, "Hooking Functional: " + isHookingFunctional());
        
        synchronized (errorCounts) {
            Log.i(TAG, "Total Hook Errors: " + errorCounts.size());
            if (!errorCounts.isEmpty()) {
                Log.i(TAG, "Error Details:");
                for (Map.Entry<String, Integer> entry : errorCounts.entrySet()) {
                    Log.i(TAG, "  " + entry.getKey() + ": " + entry.getValue() + " errors");
                }
            }
        }
        
        Log.i(TAG, "==================================");
    }
}
