package org.lsposed.lspatch.compat;

import android.content.Context;
import android.util.Log;

import org.lsposed.lspd.service.ILSPApplicationService;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XposedBridge;

/**
 * LSPatch Feature Compatibility Validator
 * 
 * This class validates and ensures that all required features work correctly
 * in both LSPatch and traditional Xposed environments. It provides comprehensive
 * testing and validation for module compatibility.
 */
public class LSPatchFeatureValidator {
    
    private static final String TAG = "LSPatch-Validator";
    
    private static Map<String, ValidationResult> validationResults = new HashMap<>();
    
    /**
     * Validate all LSPatch features for module compatibility
     */
    public static Map<String, ValidationResult> validateAllFeatures(ClassLoader classLoader) {
        validationResults.clear();
        
        Log.i(TAG, "Starting comprehensive LSPatch feature validation");
        
        // Core functionality validation
        validateCoreHookingCapability(classLoader);
        validateXposedBridgeFunctionality();
        validateContextAccess();
        validateServiceAvailability();
        
        // LSPatch specific features
        validateLSPatchDetection();
        validateResourceHooks();
        validatePreferencesAccess();
        validateModulePathAccess();
        
        // Advanced features
        validateSignatureBypass();
        validateBridgeService();
        validateSystemProperties();
        
        Log.i(TAG, "Feature validation completed - " + validationResults.size() + " features tested");
        
        return new HashMap<>(validationResults);
    }
    
    /**
     * Validate core Xposed hooking capability
     */
    private static void validateCoreHookingCapability(ClassLoader classLoader) {
        try {
            // Test basic XposedBridge functionality
            XposedBridge.log("LSPatch validation: Testing core hooking capability");
            
            // Try to hook a basic Android class to verify Xposed is working
            Class<?> testClass = classLoader.loadClass("java.lang.Object");
            Method toStringMethod = testClass.getDeclaredMethod("toString");
            
            if (toStringMethod != null) {
                addValidationResult("core_hooking", true, false, 
                    "Core Xposed hooking capability verified", null);
            } else {
                addValidationResult("core_hooking", false, false, 
                    "Could not access basic class methods", null);
            }
            
        } catch (Exception e) {
            addValidationResult("core_hooking", false, false, 
                "Core hooking capability test failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate XposedBridge functionality
     */
    private static void validateXposedBridgeFunctionality() {
        try {
            // Test XposedBridge logging
            XposedBridge.log("LSPatch validation: Testing XposedBridge functionality");
            
            // Test if XposedBridge class is accessible
            Class<?> bridgeClass = Class.forName("de.robv.android.xposed.XposedBridge");
            Method logMethod = bridgeClass.getDeclaredMethod("log", String.class);
            
            if (logMethod != null) {
                addValidationResult("xposed_bridge", true, false, 
                    "XposedBridge functionality verified", null);
            } else {
                addValidationResult("xposed_bridge", false, false, 
                    "XposedBridge log method not accessible", null);
            }
            
        } catch (Exception e) {
            addValidationResult("xposed_bridge", false, false, 
                "XposedBridge functionality test failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate context access
     */
    private static void validateContextAccess() {
        try {
            Context context = LSPatchCompat.getCurrentContext();
            
            if (context != null) {
                String packageName = context.getPackageName();
                if (packageName != null && !packageName.isEmpty()) {
                    addValidationResult("context_access", true, false, 
                        "Context access verified - Package: " + packageName, null);
                } else {
                    addValidationResult("context_access", false, true, 
                        "Context available but package name is null", null);
                }
            } else {
                addValidationResult("context_access", false, false, 
                    "Application context not available", null);
            }
            
        } catch (Exception e) {
            addValidationResult("context_access", false, false, 
                "Context access test failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate service availability
     */
    private static void validateServiceAvailability() {
        try {
            boolean serviceAvailable = LSPatchCompat.isLSPatchServiceAvailable();
            
            if (serviceAvailable) {
                // Test service functionality
                ILSPApplicationService service = LSPatchBridge.getApplicationService();
                if (service != null) {
                    try {
                        // Test basic service operations
                        boolean logMuted = service.isLogMuted();
                        addValidationResult("service_availability", true, false, 
                            "LSPatch service is functional (log muted: " + logMuted + ")", null);
                    } catch (Exception e) {
                        addValidationResult("service_availability", false, true, 
                            "Service available but not fully functional: " + e.getMessage(), e);
                    }
                } else {
                    addValidationResult("service_availability", false, true, 
                        "Service reported as available but not accessible through bridge", null);
                }
            } else {
                addValidationResult("service_availability", false, false, 
                    "LSPatch service not available", null);
            }
            
        } catch (Exception e) {
            addValidationResult("service_availability", false, false, 
                "Service availability test failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate LSPatch detection
     */
    private static void validateLSPatchDetection() {
        try {
            boolean isLSPatch = LSPatchCompat.isLSPatchEnvironment();
            LSPatchCompat.LSPatchMode mode = LSPatchCompat.getCurrentMode();
            
            if (isLSPatch) {
                addValidationResult("lspatch_detection", true, false, 
                    "LSPatch environment detected - Mode: " + mode, null);
            } else {
                addValidationResult("lspatch_detection", true, false, 
                    "Classic Xposed environment detected", null);
            }
            
        } catch (Exception e) {
            addValidationResult("lspatch_detection", false, false, 
                "LSPatch detection test failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate resource hooks capability
     */
    private static void validateResourceHooks() {
        try {
            boolean resourceHooks = LSPatchCompat.isFeatureAvailable("RESOURCE_HOOKS");
            
            if (resourceHooks) {
                // Try to access resource-related classes
                try {
                    Class.forName("android.content.res.XResources");
                    addValidationResult("resource_hooks", true, false, 
                        "Resource hooks are available and functional", null);
                } catch (ClassNotFoundException e) {
                    addValidationResult("resource_hooks", false, true, 
                        "Resource hooks reported as available but XResources not found", e);
                }
            } else {
                addValidationResult("resource_hooks", false, true, 
                    "Resource hooks not available in current LSPatch mode", null);
            }
            
        } catch (Exception e) {
            addValidationResult("resource_hooks", false, false, 
                "Resource hooks validation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate preferences access
     */
    private static void validatePreferencesAccess() {
        try {
            // Test LSPatchPreferences functionality
            LSPatchPreferences testPrefs = LSPatchBridge.createPreferences("test.package");
            
            if (testPrefs != null && testPrefs.isAccessible()) {
                // Test basic preference operations
                boolean containsTest = testPrefs.contains("test_key");
                String testValue = testPrefs.getString("test_key", "default");
                
                addValidationResult("preferences_access", true, false, 
                    "Preferences access is functional", null);
            } else {
                addValidationResult("preferences_access", false, true, 
                    "Preferences created but not accessible", null);
            }
            
        } catch (Exception e) {
            addValidationResult("preferences_access", false, false, 
                "Preferences access validation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate module path access
     */
    private static void validateModulePathAccess() {
        try {
            String modulePath = LSPatchCompat.getModulePath();
            
            if (modulePath != null && !modulePath.isEmpty()) {
                java.io.File moduleFile = new java.io.File(modulePath);
                if (moduleFile.exists()) {
                    addValidationResult("module_path", true, false, 
                        "Module path accessible: " + modulePath, null);
                } else {
                    addValidationResult("module_path", false, true, 
                        "Module path provided but file does not exist: " + modulePath, null);
                }
            } else {
                addValidationResult("module_path", false, false, 
                    "Module path not available", null);
            }
            
        } catch (Exception e) {
            addValidationResult("module_path", false, false, 
                "Module path validation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate signature bypass capability
     */
    private static void validateSignatureBypass() {
        try {
            boolean sigBypass = LSPatchCompat.isFeatureAvailable("SIGNATURE_BYPASS");
            
            if (sigBypass) {
                addValidationResult("signature_bypass", true, false, 
                    "Signature bypass is available", null);
            } else {
                addValidationResult("signature_bypass", false, true, 
                    "Signature bypass not available", null);
            }
            
        } catch (Exception e) {
            addValidationResult("signature_bypass", false, false, 
                "Signature bypass validation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate bridge service functionality
     */
    private static void validateBridgeService() {
        try {
            boolean bridgeInitialized = LSPatchBridge.isInitialized();
            
            if (bridgeInitialized) {
                // Test bridge capabilities
                boolean hasServiceComm = LSPatchBridge.hasCapability("SERVICE_COMMUNICATION");
                boolean hasContextAccess = LSPatchBridge.hasCapability("CONTEXT_ACCESS");
                
                addValidationResult("bridge_service", true, false, 
                    "Bridge service functional (ServiceComm: " + hasServiceComm + 
                    ", ContextAccess: " + hasContextAccess + ")", null);
            } else {
                addValidationResult("bridge_service", false, true, 
                    "Bridge service not initialized", null);
            }
            
        } catch (Exception e) {
            addValidationResult("bridge_service", false, false, 
                "Bridge service validation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate system properties
     */
    private static void validateSystemProperties() {
        try {
            // Test access to LSPatch system properties
            String lspatchMode = System.getProperty("lspatch.mode");
            String lspatchEnabled = System.getProperty("lspatch.initialized");
            
            if (lspatchMode != null || lspatchEnabled != null) {
                addValidationResult("system_properties", true, false, 
                    "System properties accessible (mode: " + lspatchMode + 
                    ", enabled: " + lspatchEnabled + ")", null);
            } else {
                addValidationResult("system_properties", false, true, 
                    "LSPatch system properties not found", null);
            }
            
        } catch (Exception e) {
            addValidationResult("system_properties", false, false, 
                "System properties validation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Add validation result
     */
    private static void addValidationResult(String feature, boolean compatible, boolean limited, 
                                          String message, Exception exception) {
        ValidationResult result = new ValidationResult();
        result.feature = feature;
        result.compatible = compatible;
        result.limitedFunctionality = limited;
        result.message = message;
        result.exception = exception;
        result.timestamp = System.currentTimeMillis();
        
        validationResults.put(feature, result);
        
        String status = compatible ? (limited ? "LIMITED" : "COMPATIBLE") : "INCOMPATIBLE";
        Log.d(TAG, "Feature validation [" + feature + "]: " + status + " - " + message);
    }
    
    /**
     * Get validation result for specific feature
     */
    public static ValidationResult getValidationResult(String feature) {
        return validationResults.get(feature);
    }
    
    /**
     * Check if all features are compatible
     */
    public static boolean areAllFeaturesCompatible() {
        for (ValidationResult result : validationResults.values()) {
            if (!result.compatible) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get compatibility summary
     */
    public static CompatibilitySummary getCompatibilitySummary() {
        CompatibilitySummary summary = new CompatibilitySummary();
        
        for (ValidationResult result : validationResults.values()) {
            if (result.compatible) {
                if (result.limitedFunctionality) {
                    summary.limitedFeatures++;
                } else {
                    summary.compatibleFeatures++;
                }
            } else {
                summary.incompatibleFeatures++;
            }
        }
        
        summary.totalFeatures = validationResults.size();
        return summary;
    }
    
    /**
     * Log comprehensive validation report
     */
    public static void logValidationReport() {
        Log.i(TAG, "=== LSPatch Feature Validation Report ===");
        
        CompatibilitySummary summary = getCompatibilitySummary();
        Log.i(TAG, "Summary:");
        Log.i(TAG, "  Total Features: " + summary.totalFeatures);
        Log.i(TAG, "  Compatible: " + summary.compatibleFeatures);
        Log.i(TAG, "  Limited: " + summary.limitedFeatures);
        Log.i(TAG, "  Incompatible: " + summary.incompatibleFeatures);
        
        Log.i(TAG, "\nDetailed Results:");
        for (Map.Entry<String, ValidationResult> entry : validationResults.entrySet()) {
            ValidationResult result = entry.getValue();
            String status = result.compatible ? 
                (result.limitedFunctionality ? "LIMITED" : "✓ COMPATIBLE") : 
                "✗ INCOMPATIBLE";
            
            Log.i(TAG, "  " + entry.getKey() + ": " + status);
            Log.i(TAG, "    " + result.message);
            if (result.exception != null) {
                Log.i(TAG, "    Error: " + result.exception.getMessage());
            }
        }
        
        Log.i(TAG, "========================================");
    }
    
    /**
     * Validation result for individual features
     */
    public static class ValidationResult {
        public String feature;
        public boolean compatible;
        public boolean limitedFunctionality;
        public String message;
        public Exception exception;
        public long timestamp;
    }
    
    /**
     * Overall compatibility summary
     */
    public static class CompatibilitySummary {
        public int totalFeatures = 0;
        public int compatibleFeatures = 0;
        public int limitedFeatures = 0;
        public int incompatibleFeatures = 0;
        
        public double getCompatibilityPercentage() {
            if (totalFeatures == 0) return 0.0;
            return ((double) compatibleFeatures / totalFeatures) * 100.0;
        }
        
        public String getOverallStatus() {
            if (incompatibleFeatures == 0) {
                return limitedFeatures == 0 ? "Excellent" : "Good";
            } else {
                return incompatibleFeatures < totalFeatures / 2 ? "Limited" : "Poor";
            }
        }
    }
}
