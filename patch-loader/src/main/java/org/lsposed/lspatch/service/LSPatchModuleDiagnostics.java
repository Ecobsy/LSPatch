package org.lsposed.lspatch.service;

import android.content.Context;
import android.util.Log;

import org.lsposed.lspd.models.Module;
import org.lsposed.lspd.service.ILSPApplicationService;

import java.util.List;

/**
 * Enhanced module diagnostics service for LSPatch
 * 
 * This service provides comprehensive diagnostics and compatibility checking
 * specifically designed for advanced modules like WaEnhancer that require
 * deep integration with LSPatch services and WhatsApp context verification.
 */
public class LSPatchModuleDiagnostics {
    
    private static final String TAG = "LSPatch-Diagnostics";
    
    // Known module packages that require special handling
    private static final String WAENHANCER_PACKAGE = "com.wmods.wppenhacer";
    private static final String WHATSAPP_PACKAGE = "com.whatsapp";
    private static final String WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b";
    
    private final Context context;
    private final ILSPApplicationService localService;
    private final ILSPApplicationService remoteService;
    
    public LSPatchModuleDiagnostics(Context context, 
                                   ILSPApplicationService localService,
                                   ILSPApplicationService remoteService) {
        this.context = context;
        this.localService = localService;
        this.remoteService = remoteService;
    }
    
    /**
     * Comprehensive diagnostic report for WaEnhancer compatibility
     */
    public String generateWaEnhancerCompatibilityReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== WaEnhancer LSPatch Compatibility Report ===\n\n");
        
        // Environment Detection
        report.append("1. Environment Detection:\n");
        report.append("   LSPatch Mode: ").append(detectLSPatchMode()).append("\n");
        report.append("   WhatsApp Context: ").append(isInWhatsAppContext()).append("\n");
        report.append("   System Architecture: ").append(System.getProperty("os.arch")).append("\n");
        report.append("   Android API Level: ").append(android.os.Build.VERSION.SDK_INT).append("\n\n");
        
        // Service Availability
        report.append("2. Service Availability:\n");
        report.append("   LocalApplicationService: ").append(localService != null ? "Available" : "Not Available").append("\n");
        report.append("   RemoteApplicationService: ").append(remoteService != null ? "Available" : "Not Available").append("\n");
        
        // Test service functionality
        if (localService != null) {
            report.append("   Local Service Health: ").append(testServiceHealth(localService, "Local")).append("\n");
        }
        if (remoteService != null) {
            report.append("   Remote Service Health: ").append(testServiceHealth(remoteService, "Remote")).append("\n");
        }
        report.append("\n");
        
        // Module Detection
        report.append("3. Module Detection:\n");
        ModuleDetectionResult waEnhancerResult = detectWaEnhancerModule();
        report.append("   WaEnhancer Detected: ").append(waEnhancerResult.detected).append("\n");
        report.append("   Detection Method: ").append(waEnhancerResult.detectionMethod).append("\n");
        if (waEnhancerResult.moduleInfo != null) {
            report.append("   APK Path: ").append(waEnhancerResult.moduleInfo.apkPath).append("\n");
            report.append("   App ID: ").append(waEnhancerResult.moduleInfo.appId).append("\n");
        }
        report.append("\n");
        
        // API Verification
        report.append("4. Required API Verification:\n");
        report.append(verifyRequiredAPIs());
        
        // LSPatch Compatibility Layer Status
        report.append("5. LSPatch Compatibility Layer:\n");
        report.append(verifyCompatibilityLayer());
        
        // System Properties
        report.append("6. System Properties:\n");
        report.append(getLSPatchSystemProperties());
        
        // WaEnhancer Specific Tests
        report.append("7. WaEnhancer Specific Tests:\n");
        report.append(runWaEnhancerTests());
        
        // Recommendations
        report.append("8. Recommendations:\n");
        report.append(generateRecommendations(waEnhancerResult));
        
        return report.toString();
    }
    
    private String detectLSPatchMode() {
        String mode = System.getProperty("lspatch.mode");
        if (mode != null) {
            return mode;
        }
        
        // Fallback detection
        if (localService != null && remoteService == null) {
            return "embedded (detected)";
        } else if (remoteService != null && localService == null) {
            return "manager (detected)";
        } else if (localService != null && remoteService != null) {
            return "hybrid (both services available)";
        } else {
            return "unknown (no services available)";
        }
    }
    
    private boolean isInWhatsAppContext() {
        if (context == null) return false;
        
        String packageName = context.getPackageName();
        return WHATSAPP_PACKAGE.equals(packageName) || 
               WHATSAPP_BUSINESS_PACKAGE.equals(packageName);
    }
    
    private String testServiceHealth(ILSPApplicationService service, String serviceName) {
        try {
            // Test basic functionality
            boolean logMuted = service.isLogMuted();
            List<Module> modules = service.getLegacyModulesList();
            String prefsPath = service.getPrefsPath("test");
            
            if (modules != null && prefsPath != null) {
                return "Healthy (" + modules.size() + " modules)";
            } else {
                return "Degraded (partial functionality)";
            }
        } catch (Exception e) {
            return "Failed (" + e.getMessage() + ")";
        }
    }
    
    private ModuleDetectionResult detectWaEnhancerModule() {
        ModuleDetectionResult result = new ModuleDetectionResult();
        
        // Method 1: Check through LocalApplicationService
        if (localService != null) {
            try {
                List<Module> modules = localService.getLegacyModulesList();
                for (Module module : modules) {
                    if (WAENHANCER_PACKAGE.equals(module.packageName)) {
                        result.detected = true;
                        result.detectionMethod = "LocalApplicationService modules list";
                        result.moduleInfo = module;
                        return result;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error checking local service for WaEnhancer: " + e.getMessage());
            }
        }
        
        // Method 2: Check through RemoteApplicationService
        if (remoteService != null) {
            try {
                List<Module> modules = remoteService.getLegacyModulesList();
                for (Module module : modules) {
                    if (WAENHANCER_PACKAGE.equals(module.packageName)) {
                        result.detected = true;
                        result.detectionMethod = "RemoteApplicationService modules list";
                        result.moduleInfo = module;
                        return result;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error checking remote service for WaEnhancer: " + e.getMessage());
            }
        }
        
        // Method 3: Check class availability (for embedded modules)
        if (isWaEnhancerClassesLoaded()) {
            result.detected = true;
            result.detectionMethod = "Class availability check";
            return result;
        }
        
        result.detected = false;
        result.detectionMethod = "Not found through any method";
        return result;
    }
    
    private boolean isWaEnhancerClassesLoaded() {
        String[] waEnhancerClasses = {
            "com.wmods.wppenhacer.xposed.core.WppCore",
            "com.wmods.wppenhacer.xposed.core.FeatureLoader",
            "com.wmods.wppenhacer.xposed.core.LSPatchCompat",
            "com.wmods.wppenhacer.xposed.core.LSPatchService",
            "com.wmods.wppenhacer.xposed.core.LSPatchBridge",
            "com.wmods.wppenhacer.xposed.core.LSPatchPreferences",
            "com.wmods.wppenhacer.xposed.core.LSPatchHookWrapper",
            "com.wmods.wppenhacer.xposed.core.LSPatchFeatureValidator"
        };
        
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = this.getClass().getClassLoader();
        }
        
        int loadedCount = 0;
        for (String className : waEnhancerClasses) {
            try {
                Class<?> clazz = cl.loadClass(className);
                if (clazz != null) {
                    loadedCount++;
                    Log.d(TAG, "Found WaEnhancer class: " + className);
                }
            } catch (ClassNotFoundException e) {
                // Class not found, continue
            }
        }
        
        return loadedCount >= 2; // At least 2 core classes should be loaded
    }
    
    private String verifyRequiredAPIs() {
        StringBuilder apis = new StringBuilder();
        
        // Test getLegacyModulesList
        apis.append("   getLegacyModulesList(): ");
        try {
            ILSPApplicationService activeService = localService != null ? localService : remoteService;
            if (activeService != null) {
                List<Module> modules = activeService.getLegacyModulesList();
                apis.append("✓ Working (").append(modules.size()).append(" modules)\n");
            } else {
                apis.append("✗ No service available\n");
            }
        } catch (Exception e) {
            apis.append("✗ Failed (").append(e.getMessage()).append(")\n");
        }
        
        // Test getPrefsPath
        apis.append("   getPrefsPath(): ");
        try {
            ILSPApplicationService activeService = localService != null ? localService : remoteService;
            if (activeService != null) {
                String path = activeService.getPrefsPath("test.package");
                apis.append("✓ Working (").append(path).append(")\n");
            } else {
                apis.append("✗ No service available\n");
            }
        } catch (Exception e) {
            apis.append("✗ Failed (").append(e.getMessage()).append(")\n");
        }
        
        // Test context access
        apis.append("   Context Access: ");
        if (context != null) {
            apis.append("✓ Available (").append(context.getPackageName()).append(")\n");
        } else {
            apis.append("✗ Not available\n");
        }
        
        apis.append("\n");
        return apis.toString();
    }
    
    private String getLSPatchSystemProperties() {
        StringBuilder props = new StringBuilder();
        
        String[] lspatchProps = {
            "lspatch.mode", "lspatch.embedded", "lspatch.manager", 
            "lspatch.local", "lspatch.remote", "lspatch.version"
        };
        
        for (String prop : lspatchProps) {
            String value = System.getProperty(prop);
            props.append("   ").append(prop).append(": ");
            if (value != null) {
                props.append(value).append("\n");
            } else {
                props.append("(not set)\n");
            }
        }
        
        props.append("\n");
        return props.toString();
    }
    
    private String verifyCompatibilityLayer() {
        StringBuilder compat = new StringBuilder();
        
        // Test LSPatch compatibility classes
        compat.append("   LSPatchCompat: ");
        try {
            Class<?> compatClass = Class.forName("org.lsposed.lspatch.compat.LSPatchCompat");
            compat.append("✓ Available\n");
        } catch (ClassNotFoundException e) {
            compat.append("✗ Not found\n");
        }
        
        compat.append("   LSPatchBridge: ");
        try {
            Class<?> bridgeClass = Class.forName("org.lsposed.lspatch.compat.LSPatchBridge");
            compat.append("✓ Available\n");
        } catch (ClassNotFoundException e) {
            compat.append("✗ Not found\n");
        }
        
        compat.append("   LSPatchPreferences: ");
        try {
            Class<?> prefsClass = Class.forName("org.lsposed.lspatch.compat.LSPatchPreferences");
            compat.append("✓ Available\n");
        } catch (ClassNotFoundException e) {
            compat.append("✗ Not found\n");
        }
        
        compat.append("   LSPatchHookWrapper: ");
        try {
            Class<?> hookClass = Class.forName("org.lsposed.lspatch.compat.LSPatchHookWrapper");
            compat.append("✓ Available\n");
        } catch (ClassNotFoundException e) {
            compat.append("✗ Not found\n");
        }
        
        compat.append("   LSPatchFeatureValidator: ");
        try {
            Class<?> validatorClass = Class.forName("org.lsposed.lspatch.compat.LSPatchFeatureValidator");
            compat.append("✓ Available\n");
        } catch (ClassNotFoundException e) {
            compat.append("✗ Not found\n");
        }
        
        compat.append("\n");
        return compat.toString();
    }
    
    private String runWaEnhancerTests() {
        StringBuilder tests = new StringBuilder();
        
        // Test WaEnhancer shim classes
        tests.append("   WaEnhancer Shims:\n");
        
        String[] shimClasses = {
            "org.lsposed.lspatch.compat.waenhancer.LSPatchCompatShim",
            "org.lsposed.lspatch.compat.waenhancer.LSPatchBridgeShim", 
            "org.lsposed.lspatch.compat.waenhancer.LSPatchPreferencesShim",
            "org.lsposed.lspatch.compat.waenhancer.LSPatchServiceShim"
        };
        
        int availableShims = 0;
        for (String className : shimClasses) {
            String shimName = className.substring(className.lastIndexOf('.') + 1);
            tests.append("     ").append(shimName).append(": ");
            try {
                Class.forName(className);
                tests.append("✓\n");
                availableShims++;
            } catch (ClassNotFoundException e) {
                tests.append("✗\n");
            }
        }
        
        tests.append("   Available Shims: ").append(availableShims).append("/").append(shimClasses.length).append("\n");
        
        // Test WhatsApp context detection
        tests.append("   WhatsApp Context Detection: ");
        if (isInWhatsAppContext()) {
            tests.append("✓ Detected\n");
        } else {
            tests.append("⚠ Not in WhatsApp context\n");
        }
        
        // Test XposedBridge functionality
        tests.append("   XposedBridge Test: ");
        try {
            de.robv.android.xposed.XposedBridge.log("LSPatch diagnostics test");
            tests.append("✓ Functional\n");
        } catch (Exception e) {
            tests.append("✗ Failed (" + e.getMessage() + ")\n");
        }
        
        tests.append("\n");
        return tests.toString();
    }
    
    private String generateRecommendations(ModuleDetectionResult waEnhancerResult) {
        StringBuilder recommendations = new StringBuilder();
        
        if (!waEnhancerResult.detected) {
            recommendations.append("   • WaEnhancer module not detected\n");
            recommendations.append("   • Verify module is properly installed and enabled\n");
            recommendations.append("   • Check LSPatch manager configuration\n");
        } else {
            recommendations.append("   • WaEnhancer module detected successfully\n");
            
            if ("embedded".equals(detectLSPatchMode())) {
                recommendations.append("   • Running in embedded mode - optimal compatibility\n");
            } else {
                recommendations.append("   • Running in manager mode - some limitations may apply\n");
                recommendations.append("   • Consider embedded mode for full feature compatibility\n");
            }
        }
        
        if (!isInWhatsAppContext()) {
            recommendations.append("   • ⚠ Not running in WhatsApp context\n");
            recommendations.append("   • WaEnhancer requires WhatsApp application context to function\n");
        }
        
        if (localService == null && remoteService == null) {
            recommendations.append("   • ✗ No LSPatch services available\n");
            recommendations.append("   • Check LSPatch installation and initialization\n");
        }
        
        return recommendations.toString();
    }
    
    /**
     * Generate a simple health check status
     */
    public HealthStatus getHealthStatus() {
        HealthStatus status = new HealthStatus();
        
        // Check basic requirements
        status.lspatchDetected = (localService != null || remoteService != null);
        status.whatsappContext = isInWhatsAppContext();
        status.waenhancerDetected = detectWaEnhancerModule().detected;
        
        // Overall health
        if (status.lspatchDetected && status.whatsappContext && status.waenhancerDetected) {
            status.overall = "Excellent";
        } else if (status.lspatchDetected && status.waenhancerDetected) {
            status.overall = "Good";
        } else if (status.lspatchDetected) {
            status.overall = "Limited";
        } else {
            status.overall = "Poor";
        }
        
        return status;
    }
    
    public static class ModuleDetectionResult {
        public boolean detected = false;
        public String detectionMethod = "";
        public Module moduleInfo = null;
    }
    
    public static class HealthStatus {
        public boolean lspatchDetected = false;
        public boolean whatsappContext = false;
        public boolean waenhancerDetected = false;
        public String overall = "Unknown";
    }
}
