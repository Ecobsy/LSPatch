package org.lsposed.lspatch.compat.waenhancer;

import android.content.Context;
import android.util.Log;

import org.lsposed.lspatch.compat.LSPatchBridge;

/**
 * WaEnhancer LSPatch Bridge compatibility
 * 
 * This class provides the exact bridge APIs that WaEnhancer expects
 * for service communication and capability checking.
 */
public class LSPatchBridgeShim {
    
    private static final String TAG = "WaEnhancer-Bridge";
    
    /**
     * Check if bridge is initialized (WaEnhancer API)
     */
    public static boolean isInitialized() {
        return LSPatchBridge.isInitialized();
    }
    
    /**
     * Get application context (WaEnhancer API)
     */
    public static Context getContext() {
        return LSPatchBridge.getApplicationContext();
    }
    
    /**
     * Check capability (WaEnhancer API)
     */
    public static boolean hasCapability(String capability) {
        return LSPatchBridge.hasCapability(capability);
    }
    
    /**
     * Create preferences (WaEnhancer API)
     */
    public static LSPatchPreferencesShim createPreferences(String packageName) {
        return LSPatchPreferencesShim.create(packageName);
    }
    
    /**
     * Create preferences with custom name (WaEnhancer API)
     */
    public static LSPatchPreferencesShim createPreferences(String packageName, String prefsName) {
        return LSPatchPreferencesShim.create(packageName, prefsName);
    }
    
    /**
     * Log bridge status (WaEnhancer API)
     */
    public static void logStatus() {
        LSPatchBridge.logBridgeStatus();
    }
    
    /**
     * Handle service operation (WaEnhancer API)
     */
    public static boolean handleServiceOperation(String operation, Object... params) {
        return LSPatchBridge.handleServiceOperation(operation, params);
    }
    
    /**
     * Report module error (WaEnhancer API)
     */
    public static void reportError(String moduleName, String error, Throwable throwable) {
        LSPatchBridge.reportModuleError(moduleName, error, throwable);
    }
    
    /**
     * Get module errors (WaEnhancer API)
     */
    public static String getModuleErrors(String moduleName) {
        return LSPatchBridge.getModuleErrors(moduleName);
    }
}
