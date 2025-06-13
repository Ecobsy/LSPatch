package org.lsposed.lspatch.compat.waenhancer;

import android.content.Context;
import android.util.Log;

import org.lsposed.lspatch.compat.LSPatchCompat;
import org.lsposed.lspatch.compat.LSPatchBridge;
import org.lsposed.lspatch.compat.LSPatchPreferences;

/**
 * WaEnhancer compatibility shim for LSPatch
 * 
 * This class provides the exact APIs that WaEnhancer expects to find
 * in LSPatch environments, ensuring full compatibility with the module.
 */
public class LSPatchCompatShim {
    
    private static final String TAG = "WaEnhancer-LSPatch";
    
    /**
     * Check if running in LSPatch environment (WaEnhancer API)
     */
    public static boolean isLSPatchEnvironment() {
        return LSPatchCompat.isLSPatchEnvironment();
    }
    
    /**
     * Get current LSPatch mode (WaEnhancer API)
     */
    public static LSPatchMode getCurrentMode() {
        LSPatchCompat.LSPatchMode mode = LSPatchCompat.getCurrentMode();
        switch (mode) {
            case LSPATCH_EMBEDDED:
                return LSPatchMode.LSPATCH_EMBEDDED;
            case LSPATCH_MANAGER:
                return LSPatchMode.LSPATCH_MANAGER;
            default:
                return LSPatchMode.CLASSIC_XPOSED;
        }
    }
    
    /**
     * Check if LSPatch service is available (WaEnhancer API)
     */
    public static boolean isLSPatchServiceAvailable() {
        return LSPatchCompat.isLSPatchServiceAvailable();
    }
    
    /**
     * Check if feature is available (WaEnhancer API)
     */
    public static boolean isFeatureAvailable(String feature) {
        return LSPatchCompat.isFeatureAvailable(feature);
    }
    
    /**
     * Initialize LSPatch compatibility (WaEnhancer API)
     */
    public static void init() {
        Context context = LSPatchCompat.getCurrentContext();
        if (context != null) {
            LSPatchCompat.init(context);
        }
    }
    
    /**
     * Get current context (WaEnhancer API)
     */
    public static Context getCurrentContext() {
        return LSPatchCompat.getCurrentContext();
    }
    
    /**
     * Check if bridge is initialized (WaEnhancer API)
     */
    public static boolean isBridgeInitialized() {
        return LSPatchBridge.isInitialized();
    }
    
    /**
     * Validate LSPatch integrity (WaEnhancer API)
     */
    public static boolean validateLSPatchIntegrity() {
        return LSPatchCompat.validateLSPatchIntegrity();
    }
    
    /**
     * Log compatibility information (WaEnhancer API)
     */
    public static void logCompatibilityInfo() {
        LSPatchCompat.logCompatibilityInfo();
    }
    
    /**
     * LSPatch modes that WaEnhancer expects
     */
    public enum LSPatchMode {
        CLASSIC_XPOSED,
        LSPATCH_EMBEDDED,
        LSPATCH_MANAGER
    }
}
