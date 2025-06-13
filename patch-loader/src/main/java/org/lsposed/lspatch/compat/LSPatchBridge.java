package org.lsposed.lspatch.compat;

import android.content.Context;
import android.util.Log;

import org.lsposed.lspd.service.ILSPApplicationService;

/**
 * LSPatch Bridge for enhanced module integration
 * 
 * This class provides bridge functionality between modules and LSPatch services,
 * enabling advanced features like service communication, resource handling,
 * and hook optimization for LSPatch environments.
 */
public class LSPatchBridge {
    
    private static final String TAG = "LSPatch-Bridge";
    
    private static boolean initialized = false;
    private static Context applicationContext;
    private static ILSPApplicationService applicationService;
    
    /**
     * Initialize the LSPatch bridge
     */
    public static boolean initialize(Context context, ILSPApplicationService service) {
        if (initialized) {
            return true;
        }
        
        try {
            applicationContext = context;
            applicationService = service;
            
            // Setup hook optimizations
            setupHookOptimizations();
            
            // Initialize compatibility layer
            LSPatchCompat.init(context);
            
            initialized = true;
            Log.i(TAG, "LSPatch Bridge initialized successfully");
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize LSPatch Bridge: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if bridge is initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Get application context
     */
    public static Context getApplicationContext() {
        return applicationContext;
    }
    
    /**
     * Get application service
     */
    public static ILSPApplicationService getApplicationService() {
        return applicationService;
    }
    
    /**
     * Create LSPatch compatible preferences
     */
    public static LSPatchPreferences createPreferences(String packageName, String prefsName) {
        return new LSPatchPreferences(packageName, prefsName, applicationContext, applicationService);
    }
    
    /**
     * Create LSPatch compatible preferences with default name
     */
    public static LSPatchPreferences createPreferences(String packageName) {
        return new LSPatchPreferences(packageName, applicationContext, applicationService);
    }
    
    /**
     * Check if a specific LSPatch capability is available
     */
    public static boolean hasCapability(String capability) {
        switch (capability) {
            case "RESOURCE_HOOKS":
                return LSPatchCompat.isFeatureAvailable("RESOURCE_HOOKS");
            case "SYSTEM_HOOKS":
                return LSPatchCompat.isFeatureAvailable("SYSTEM_SERVER_HOOKS");
            case "SIGNATURE_BYPASS":
                return LSPatchCompat.isFeatureAvailable("SIGNATURE_BYPASS");
            case "SERVICE_COMMUNICATION":
                return applicationService != null;
            case "CONTEXT_ACCESS":
                return applicationContext != null;
            case "PREFERENCES":
                return LSPatchCompat.isFeatureAvailable("PREFERENCES");
            case "MODULE_PATH":
                return LSPatchCompat.isFeatureAvailable("MODULE_PATH");
            default:
                return false;
        }
    }
    
    /**
     * Get LSPatch runtime information
     */
    public static LSPatchInfo getLSPatchInfo() {
        LSPatchInfo info = new LSPatchInfo();
        
        info.isLSPatchEnvironment = LSPatchCompat.isLSPatchEnvironment();
        info.lspatchMode = LSPatchCompat.getCurrentMode();
        info.serviceAvailable = LSPatchCompat.isLSPatchServiceAvailable();
        info.bridgeInitialized = initialized;
        
        if (applicationContext != null) {
            info.packageName = applicationContext.getPackageName();
            info.dataDirectory = applicationContext.getDataDir().getAbsolutePath();
        }
        
        info.modulePath = LSPatchCompat.getModulePath();
        
        return info;
    }
    
    /**
     * Log comprehensive bridge status
     */
    public static void logBridgeStatus() {
        Log.i(TAG, "=== LSPatch Bridge Status ===");
        Log.i(TAG, "Bridge Initialized: " + initialized);
        Log.i(TAG, "Context Available: " + (applicationContext != null));
        Log.i(TAG, "Service Available: " + (applicationService != null));
        
        if (initialized) {
            LSPatchInfo info = getLSPatchInfo();
            Log.i(TAG, "LSPatch Environment: " + info.isLSPatchEnvironment);
            Log.i(TAG, "LSPatch Mode: " + info.lspatchMode);
            Log.i(TAG, "Package: " + info.packageName);
            Log.i(TAG, "Module Path: " + info.modulePath);
            
            Log.i(TAG, "Capabilities:");
            Log.i(TAG, "  - Resource Hooks: " + hasCapability("RESOURCE_HOOKS"));
            Log.i(TAG, "  - System Hooks: " + hasCapability("SYSTEM_HOOKS"));
            Log.i(TAG, "  - Signature Bypass: " + hasCapability("SIGNATURE_BYPASS"));
            Log.i(TAG, "  - Service Communication: " + hasCapability("SERVICE_COMMUNICATION"));
            Log.i(TAG, "  - Context Access: " + hasCapability("CONTEXT_ACCESS"));
            Log.i(TAG, "  - Preferences: " + hasCapability("PREFERENCES"));
            Log.i(TAG, "  - Module Path: " + hasCapability("MODULE_PATH"));
        }
        
        Log.i(TAG, "============================");
    }
    
    /**
     * Handle bridge service operations for modules
     */
    public static boolean handleServiceOperation(String operation, Object... params) {
        if (!initialized || applicationService == null) {
            Log.w(TAG, "Cannot handle service operation - bridge not initialized or service unavailable");
            return false;
        }
        
        try {
            switch (operation) {
                case "getModulesList":
                    return applicationService.getLegacyModulesList() != null;
                    
                case "getPrefsPath":
                    if (params.length > 0 && params[0] instanceof String) {
                        return applicationService.getPrefsPath((String) params[0]) != null;
                    }
                    return false;
                    
                case "isLogMuted":
                    return !applicationService.isLogMuted();
                    
                default:
                    Log.w(TAG, "Unknown service operation: " + operation);
                    return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling service operation " + operation + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Provide error reporting capabilities for modules
     */
    public static void reportModuleError(String moduleName, String error, Throwable throwable) {
        Log.e(TAG, "Module Error [" + moduleName + "]: " + error);
        if (throwable != null) {
            Log.e(TAG, "Module Error Stack Trace:", throwable);
        }
        
        // Store error for diagnostics
        try {
            String errorProperty = "lspatch.module.error." + moduleName;
            System.setProperty(errorProperty, error);
        } catch (Exception e) {
            // Ignore property setting errors
        }
    }
    
    /**
     * Get module error history
     */
    public static String getModuleErrors(String moduleName) {
        try {
            return System.getProperty("lspatch.module.error." + moduleName);
        } catch (Exception e) {
            return null;
        }
    }
    
    // Private helper methods
    
    private static void setupHookOptimizations() {
        Log.d(TAG, "Setting up hook optimizations for LSPatch");
        
        try {
            // Set hook priorities for better stability in LSPatch
            System.setProperty("lspatch.hook.priority", "high");
            System.setProperty("lspatch.hook.verify", "true");
            System.setProperty("lspatch.bridge.optimized", "true");
            
            // Apply memory optimizations
            System.setProperty("lspatch.memory.conservative", "true");
            
        } catch (Exception e) {
            Log.d(TAG, "Could not set all optimization properties: " + e.getMessage());
        }
    }
    
    /**
     * LSPatch runtime information container
     */
    public static class LSPatchInfo {
        public boolean isLSPatchEnvironment = false;
        public LSPatchCompat.LSPatchMode lspatchMode = LSPatchCompat.LSPatchMode.CLASSIC_XPOSED;
        public boolean serviceAvailable = false;
        public boolean bridgeInitialized = false;
        public String packageName = null;
        public String dataDirectory = null;
        public String modulePath = null;
        
        @Override
        public String toString() {
            return "LSPatchInfo{" +
                    "isLSPatchEnvironment=" + isLSPatchEnvironment +
                    ", lspatchMode=" + lspatchMode +
                    ", serviceAvailable=" + serviceAvailable +
                    ", bridgeInitialized=" + bridgeInitialized +
                    ", packageName='" + packageName + '\'' +
                    ", dataDirectory='" + dataDirectory + '\'' +
                    ", modulePath='" + modulePath + '\'' +
                    '}';
        }
    }
}
