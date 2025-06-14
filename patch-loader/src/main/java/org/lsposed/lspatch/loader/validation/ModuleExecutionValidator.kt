package org.lsposed.lspatch.loader.validation

import android.content.Context
import android.util.Log
import org.lsposed.lspd.models.Module
import org.lsposed.lspd.service.ILSPApplicationService

/**
 * Module execution validator for LSPatch
 * Ensures that embedded modules can be properly executed in patched applications
 */
class ModuleExecutionValidator {
    
    companion object {
        private const val TAG = "LSPatch-ModuleValidator"
        
        /**
         * Validate that modules can be executed in the current context
         */
        fun validateModuleExecution(
            context: Context,
            service: ILSPApplicationService,
            modules: List<Module>
        ): ValidationResult {
            val result = ValidationResult()
            
            try {
                Log.i(TAG, "Starting module execution validation for ${modules.size} modules")
                
                // 1. Validate LSPatch environment
                if (!validateLSPatchEnvironment(context)) {
                    result.addError("LSPatch environment not properly initialized")
                    return result
                }
                
                // 2. Validate service availability
                if (!validateServiceAvailability(service)) {
                    result.addError("LSPatch service not available or not functional")
                    return result
                }
                
                // 3. Validate each module
                for (module in modules) {
                    val moduleResult = validateModule(context, module)
                    result.mergeResult(moduleResult)
                }
                
                // 4. Validate Xposed bridge functionality
                if (!validateXposedBridge()) {
                    result.addWarning("XposedBridge functionality may be limited")
                }
                
                // 5. Validate hook capabilities
                if (!validateHookCapabilities()) {
                    result.addWarning("Hook capabilities may be limited")
                }
                
                Log.i(TAG, "Module validation completed: ${result.isValid()}")
                result.addInfo("Validation completed for ${modules.size} modules")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during module validation: ${e.message}")
                result.addError("Validation failed with exception: ${e.message}")
            }
            
            return result
        }
        
        /**
         * Validate LSPatch environment
         */
        private fun validateLSPatchEnvironment(context: Context): Boolean {
            try {
                // Check system properties
                val lspatchInitialized = System.getProperty("lspatch.initialized") == "true"
                val lspatchMode = System.getProperty("lspatch.mode")
                
                Log.d(TAG, "LSPatch initialized: $lspatchInitialized, mode: $lspatchMode")
                
                if (!lspatchInitialized) {
                    Log.w(TAG, "LSPatch not properly initialized")
                    return false
                }
                
                // Check LSPatch classes availability
                val requiredClasses = listOf(
                    "org.lsposed.lspatch.loader.LSPApplication",
                    "org.lsposed.lspatch.loader.LSPLoader",
                    "de.robv.android.xposed.XposedBridge"
                )
                
                for (className in requiredClasses) {
                    try {
                        Class.forName(className)
                        Log.d(TAG, "Required class found: $className")
                    } catch (e: ClassNotFoundException) {
                        Log.e(TAG, "Required class not found: $className")
                        return false
                    }
                }
                
                return true
                
            } catch (e: Exception) {
                Log.e(TAG, "Error validating LSPatch environment: ${e.message}")
                return false
            }
        }
        
        /**
         * Validate service availability
         */
        private fun validateServiceAvailability(service: ILSPApplicationService): Boolean {
            try {
                // Test basic service operations
                val modules = service.legacyModulesList
                val logMuted = service.isLogMuted
                val prefsPath = service.getPrefsPath("test")
                
                Log.d(TAG, "Service validation - modules: ${modules?.size}, prefsPath: $prefsPath")
                
                return modules != null && prefsPath != null
                
            } catch (e: Exception) {
                Log.e(TAG, "Error validating service: ${e.message}")
                return false
            }
        }
        
        /**
         * Validate individual module
         */
        private fun validateModule(context: Context, module: Module): ValidationResult {
            val result = ValidationResult()
            
            try {
                Log.d(TAG, "Validating module: ${module.packageName}")
                
                // Check module file existence and integrity
                if (module.apkPath == null || module.apkPath.isEmpty()) {
                    result.addError("Module ${module.packageName}: APK path is null or empty")
                    return result
                }
                
                val moduleFile = java.io.File(module.apkPath)
                if (!moduleFile.exists()) {
                    result.addError("Module ${module.packageName}: APK file does not exist at ${module.apkPath}")
                    return result
                }
                
                if (!moduleFile.canRead()) {
                    result.addError("Module ${module.packageName}: APK file is not readable")
                    return result
                }
                
                // Check module loading data
                if (module.file == null) {
                    result.addWarning("Module ${module.packageName}: PreLoadedApk is null")
                } else {
                    // Validate module classes
                    if (module.file.moduleClassNames.isEmpty()) {
                        result.addWarning("Module ${module.packageName}: No module classes found")
                    } else {
                        Log.d(TAG, "Module ${module.packageName} has ${module.file.moduleClassNames.size()} classes")
                        result.addInfo("Module ${module.packageName}: ${module.file.moduleClassNames.size()} classes loaded")
                    }
                    
                    // Validate DEX files
                    if (module.file.preLoadedDexes.isEmpty()) {
                        result.addError("Module ${module.packageName}: No DEX files loaded")
                    } else {
                        Log.d(TAG, "Module ${module.packageName} has ${module.file.preLoadedDexes.size()} DEX files")
                        result.addInfo("Module ${module.packageName}: ${module.file.preLoadedDexes.size()} DEX files loaded")
                    }
                }
                
                result.addInfo("Module ${module.packageName}: Basic validation passed")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error validating module ${module.packageName}: ${e.message}")
                result.addError("Module ${module.packageName}: Validation failed - ${e.message}")
            }
            
            return result
        }
        
        /**
         * Validate Xposed bridge functionality
         */
        private fun validateXposedBridge(): Boolean {
            try {
                // Test XposedBridge availability and basic functionality
                val bridgeClass = Class.forName("de.robv.android.xposed.XposedBridge")
                
                // Test log method
                val logMethod = bridgeClass.getMethod("log", String::class.java)
                logMethod.invoke(null, "LSPatch module validation test")
                
                Log.d(TAG, "XposedBridge validation passed")
                return true
                
            } catch (e: Exception) {
                Log.w(TAG, "XposedBridge validation failed: ${e.message}")
                return false
            }
        }
        
        /**
         * Validate hook capabilities
         */
        private fun validateHookCapabilities(): Boolean {
            try {
                // Test hook-related classes availability
                val requiredHookClasses = listOf(
                    "de.robv.android.xposed.XC_MethodHook",
                    "de.robv.android.xposed.XposedHelpers",
                    "de.robv.android.xposed.callbacks.XC_LoadPackage"
                )
                
                for (className in requiredHookClasses) {
                    Class.forName(className)
                }
                
                Log.d(TAG, "Hook capabilities validation passed")
                return true
                
            } catch (e: Exception) {
                Log.w(TAG, "Hook capabilities validation failed: ${e.message}")
                return false
            }
        }
    }
    
    /**
     * Validation result container
     */
    class ValidationResult {
        private val errors = mutableListOf<String>()
        private val warnings = mutableListOf<String>()
        private val info = mutableListOf<String>()
        
        fun addError(message: String) {
            errors.add(message)
            Log.e(TAG, "Validation Error: $message")
        }
        
        fun addWarning(message: String) {
            warnings.add(message)
            Log.w(TAG, "Validation Warning: $message")
        }
        
        fun addInfo(message: String) {
            info.add(message)
            Log.i(TAG, "Validation Info: $message")
        }
        
        fun mergeResult(other: ValidationResult) {
            errors.addAll(other.errors)
            warnings.addAll(other.warnings)
            info.addAll(other.info)
        }
        
        fun isValid(): Boolean = errors.isEmpty()
        
        fun hasWarnings(): Boolean = warnings.isNotEmpty()
        
        fun getErrors(): List<String> = errors.toList()
        fun getWarnings(): List<String> = warnings.toList()
        fun getInfo(): List<String> = info.toList()
        
        fun getSummary(): String {
            return buildString {
                appendLine("=== Module Execution Validation Summary ===")
                appendLine("Status: ${if (isValid()) "VALID" else "INVALID"}")
                appendLine("Errors: ${errors.size}")
                appendLine("Warnings: ${warnings.size}")
                appendLine("Info: ${info.size}")
                appendLine()
                
                if (errors.isNotEmpty()) {
                    appendLine("ERRORS:")
                    errors.forEach { appendLine("  • $it") }
                    appendLine()
                }
                
                if (warnings.isNotEmpty()) {
                    appendLine("WARNINGS:")
                    warnings.forEach { appendLine("  • $it") }
                    appendLine()
                }
                
                if (info.isNotEmpty()) {
                    appendLine("INFO:")
                    info.forEach { appendLine("  • $it") }
                }
            }
        }
    }
}
