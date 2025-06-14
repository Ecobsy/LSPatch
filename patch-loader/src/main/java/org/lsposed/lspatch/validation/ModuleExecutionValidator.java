package org.lsposed.lspatch.validation;

import android.content.Context;
import android.util.Log;

import org.lsposed.lspd.service.ILSPApplicationService;
import org.lsposed.lspd.models.Module;

import java.util.List;
import java.util.ArrayList;

/**
 * Validador de ejecución de módulos para LSPatch
 * Verifica que los módulos puedan ejecutarse correctamente en el entorno LSPatch
 */
public class ModuleExecutionValidator {
    
    private static final String TAG = "LSPatch-Validator";
    
    /**
     * Valida la ejecución de módulos en el entorno LSPatch
     */
    public static ValidationResult validateModuleExecution(Context context, 
                                                          ILSPApplicationService service, 
                                                          List<Module> modules) {
        ValidationResult result = new ValidationResult();
        result.totalModules = modules.size();
        
        try {
            Log.i(TAG, "Starting module execution validation for " + modules.size() + " modules");
            
            for (Module module : modules) {
                ModuleValidationInfo info = validateSingleModule(context, service, module);
                result.moduleResults.add(info);
                
                if (info.isValid) {
                    result.validModules++;
                } else {
                    result.invalidModules++;
                }
            }
            
            // Validar entorno general
            validateEnvironment(context, result);
            
            Log.i(TAG, "Module validation completed: " + result.getSummary());
            
        } catch (Exception e) {
            Log.e(TAG, "Error during module validation: " + e.getMessage());
            result.validationError = e.getMessage();
        }
        
        return result;
    }
    
    /**
     * Valida un módulo individual
     */
    private static ModuleValidationInfo validateSingleModule(Context context, 
                                                            ILSPApplicationService service, 
                                                            Module module) {
        ModuleValidationInfo info = new ModuleValidationInfo();
        info.packageName = module.packageName;
        info.apkPath = module.apkPath;
        
        try {
            // Verificar que el APK existe
            java.io.File apkFile = new java.io.File(module.apkPath);
            if (!apkFile.exists()) {
                info.errors.add("APK file does not exist: " + module.apkPath);
                return info;
            }
            
            // Verificar que PreLoadedApk está disponible
            if (module.file == null) {
                info.errors.add("PreLoadedApk is null");
                return info;
            }
            
            // Verificar DEX files
            if (module.file.preLoadedDexes == null || module.file.preLoadedDexes.isEmpty()) {
                info.errors.add("No DEX files loaded");
                return info;
            }
            info.dexCount = module.file.preLoadedDexes.size();
            
            // Verificar clases de módulo
            if (module.file.moduleClassNames == null || module.file.moduleClassNames.isEmpty()) {
                info.errors.add("No module classes found");
                return info;
            }
            info.classCount = module.file.moduleClassNames.size();
            
            // Verificar que las clases pueden cargarse
            int loadedClasses = 0;
            for (String className : module.file.moduleClassNames) {
                try {
                    Class.forName(className);
                    loadedClasses++;
                } catch (ClassNotFoundException e) {
                    info.warnings.add("Class not found: " + className);
                }
            }
            info.loadedClassCount = loadedClasses;
            
            // Validar interfaces Xposed
            validateXposedInterfaces(module, info);
            
            // Si llegamos hasta aquí sin errores críticos, el módulo es válido
            info.isValid = info.errors.isEmpty() && loadedClasses > 0;
            
            if (info.isValid) {
                Log.d(TAG, "Module " + module.packageName + " validation: PASSED");
            } else {
                Log.w(TAG, "Module " + module.packageName + " validation: FAILED - " + info.errors);
            }
            
        } catch (Exception e) {
            info.errors.add("Validation exception: " + e.getMessage());
            Log.e(TAG, "Error validating module " + module.packageName + ": " + e.getMessage());
        }
        
        return info;
    }
    
    /**
     * Valida interfaces Xposed del módulo
     */
    private static void validateXposedInterfaces(Module module, ModuleValidationInfo info) {
        try {
            for (String className : module.file.moduleClassNames) {
                try {
                    Class<?> moduleClass = Class.forName(className);
                    
                    // Verificar IXposedHookLoadPackage
                    if (implementsInterface(moduleClass, "de.robv.android.xposed.IXposedHookLoadPackage")) {
                        info.implementedInterfaces.add("IXposedHookLoadPackage");
                    }
                    
                    // Verificar IXposedHookInitPackageResources
                    if (implementsInterface(moduleClass, "de.robv.android.xposed.IXposedHookInitPackageResources")) {
                        info.implementedInterfaces.add("IXposedHookInitPackageResources");
                    }
                    
                    // Verificar IXposedHookZygoteInit
                    if (implementsInterface(moduleClass, "de.robv.android.xposed.IXposedHookZygoteInit")) {
                        info.implementedInterfaces.add("IXposedHookZygoteInit");
                    }
                    
                } catch (ClassNotFoundException e) {
                    // Clase no encontrada, ya registrado en validación anterior
                }
            }
            
        } catch (Exception e) {
            info.warnings.add("Error validating Xposed interfaces: " + e.getMessage());
        }
    }
    
    /**
     * Verifica si una clase implementa una interfaz específica
     */
    private static boolean implementsInterface(Class<?> clazz, String interfaceName) {
        try {
            Class<?> interfaceClass = Class.forName(interfaceName);
            return interfaceClass.isAssignableFrom(clazz);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Valida el entorno general de LSPatch
     */
    private static void validateEnvironment(Context context, ValidationResult result) {
        try {
            // Verificar XposedBridge
            try {
                Class.forName("de.robv.android.xposed.XposedBridge");
                result.environmentChecks.put("XposedBridge", "Available");
            } catch (ClassNotFoundException e) {
                result.environmentChecks.put("XposedBridge", "Not Available");
            }
            
            // Verificar LSPatch classes
            try {
                Class.forName("org.lsposed.lspatch.loader.LSPApplication");
                result.environmentChecks.put("LSPApplication", "Available");
            } catch (ClassNotFoundException e) {
                result.environmentChecks.put("LSPApplication", "Not Available");
            }
            
            // Verificar sistema de servicios
            try {
                Class.forName("org.lsposed.lspatch.service.LSPatchServiceManager");
                result.environmentChecks.put("ServiceManager", "Available");
            } catch (ClassNotFoundException e) {
                result.environmentChecks.put("ServiceManager", "Not Available");
            }
            
            // Verificar propiedades del sistema
            String lspatchInit = System.getProperty("lspatch.initialized");
            result.environmentChecks.put("LSPatch Initialized", lspatchInit != null ? lspatchInit : "false");
            
            String xposedCompat = System.getProperty("lspatch.xposed.compatible");
            result.environmentChecks.put("Xposed Compatible", xposedCompat != null ? xposedCompat : "false");
            
        } catch (Exception e) {
            result.environmentChecks.put("Environment Validation Error", e.getMessage());
        }
    }
    
    /**
     * Resultado de validación de módulos
     */
    public static class ValidationResult {
        public int totalModules = 0;
        public int validModules = 0;
        public int invalidModules = 0;
        public List<ModuleValidationInfo> moduleResults = new ArrayList<>();
        public java.util.Map<String, String> environmentChecks = new java.util.HashMap<>();
        public String validationError = null;
        
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Module Validation Summary:\n");
            sb.append("Total modules: ").append(totalModules).append("\n");
            sb.append("Valid modules: ").append(validModules).append("\n");
            sb.append("Invalid modules: ").append(invalidModules).append("\n");
            sb.append("Success rate: ").append(String.format("%.1f", (validModules * 100.0 / Math.max(totalModules, 1)))).append("%\n");
            
            if (validationError != null) {
                sb.append("Validation error: ").append(validationError).append("\n");
            }
            
            sb.append("\nEnvironment checks:\n");
            for (java.util.Map.Entry<String, String> entry : environmentChecks.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            
            return sb.toString();
        }
    }
    
    /**
     * Información de validación de un módulo individual
     */
    public static class ModuleValidationInfo {
        public String packageName;
        public String apkPath;
        public boolean isValid = false;
        public int dexCount = 0;
        public int classCount = 0;
        public int loadedClassCount = 0;
        public List<String> implementedInterfaces = new ArrayList<>();
        public List<String> errors = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
        
        @Override
        public String toString() {
            return "ModuleValidationInfo{" +
                "packageName='" + packageName + '\'' +
                ", isValid=" + isValid +
                ", dexCount=" + dexCount +
                ", classCount=" + classCount +
                ", loadedClassCount=" + loadedClassCount +
                ", interfaces=" + implementedInterfaces +
                ", errors=" + errors.size() +
                ", warnings=" + warnings.size() +
                '}';
        }
    }
}
