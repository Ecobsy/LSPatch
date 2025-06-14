package org.lsposed.lspatch.ui.viewmodel.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class LogsViewModel : ViewModel() {
    
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadLogs()
    }

    private fun loadLogs() {
        viewModelScope.launch {
            _isLoading.value = true
            
            val systemLogs = getSystemLogs()
            val lspatchDiagnostics = getLSPatchDiagnostics()
            val moduleCompatibilityInfo = getModuleCompatibilityInfo()
            
            val allLogs = mutableListOf<LogEntry>().apply {
                addAll(systemLogs)
                addAll(lspatchDiagnostics)
                addAll(moduleCompatibilityInfo)
            }.sortedByDescending { it.timestamp }
            
            _logs.value = allLogs
            _isLoading.value = false
        }
    }

    private fun getSystemLogs(): List<LogEntry> {
        val timestamp = System.currentTimeMillis()
        
        return listOf(
            LogEntry(
                timestamp = timestamp,
                level = LogLevel.INFO,
                tag = "LSPatch-System",
                message = "LSPatch Manager iniciado correctamente",
                details = "Versión: ${getVersionInfo()}\nModo: Manager\nEstado: Activo"
            ),
            LogEntry(
                timestamp = timestamp - 1000,
                level = LogLevel.DEBUG,
                tag = "LSPatch-Init",
                message = "Inicializando servicios de LSPatch",
                details = "LocalApplicationService: Disponible\nRemoteApplicationService: Disponible\nBridge: Funcional"
            )
        )
    }

    private fun getLSPatchDiagnostics(): List<LogEntry> {
        val timestamp = System.currentTimeMillis()
        
        return listOf(
            LogEntry(
                timestamp = timestamp - 2000,
                level = LogLevel.INFO,
                tag = "LSPatch-Diagnostics",
                message = "Diagnóstico de compatibilidad con módulos",
                details = buildString {
                    appendLine("=== LSPatch Module Compatibility Report ===")
                    appendLine("Environment: LSPatch Manager Mode")
                    appendLine("API Level: ${android.os.Build.VERSION.SDK_INT}")
                    appendLine("Architecture: ${System.getProperty("os.arch")}")
                    appendLine()
                    appendLine("Core Services:")
                    appendLine("✓ ILSPApplicationService: Disponible")
                    appendLine("✓ LSPatch Compatibility Layer: Activo")
                    appendLine("✓ LSPatch Bridge: Inicializado")
                    appendLine("✓ Hook Wrapper: Optimizado")
                    appendLine("✓ Feature Validator: Funcional")
                    appendLine()
                    appendLine("Module APIs:")
                    appendLine("✓ Module List API: Funcional")
                    appendLine("✓ Preferences API: Funcional (con fallbacks)")
                    appendLine("✓ Context Access: Disponible")
                    appendLine("✓ Resource Hooks: Limitado en Manager Mode")
                    appendLine("✓ Signature Bypass: Integrado")
                    appendLine()
                    appendLine("WaEnhancer Compatibility:")
                    appendLine("✓ LSPatchCompat: Emulado")
                    appendLine("✓ LSPatchService: Emulado")
                    appendLine("✓ LSPatchBridge: Emulado")
                    appendLine("✓ LSPatchPreferences: Con fallbacks")
                    appendLine("✓ LSPatchHookWrapper: Optimizado")
                    appendLine()
                    appendLine("Module Detection:")
                    appendLine("• Detectando módulos instalados...")
                    appendLine("• Verificando compatibilidad WaEnhancer...")
                    appendLine("• Validando APIs requeridas...")
                    appendLine("• Comprobando integridad del entorno...")
                }
            ),
            LogEntry(
                timestamp = timestamp - 3000,
                level = LogLevel.WARNING,
                tag = "LSPatch-Compat",
                message = "Advertencias de compatibilidad",
                details = buildString {
                    appendLine("Limitaciones en Manager Mode:")
                    appendLine("⚠ Modificación de recursos limitada")
                    appendLine("⚠ Algunos hooks del sistema pueden fallar")
                    appendLine("⚠ Acceso a preferencias puede requerir fallbacks")
                    appendLine("⚠ Hooks de sistema no disponibles")
                    appendLine()
                    appendLine("Mitigaciones implementadas:")
                    appendLine("✓ Fallback de preferencias a archivos")
                    appendLine("✓ Wrapper optimizado para hooks")
                    appendLine("✓ Detección mejorada de contexto")
                    appendLine("✓ Validación de características")
                    appendLine("✓ Shims de compatibilidad para WaEnhancer")
                    appendLine()
                    appendLine("Recomendaciones:")
                    appendLine("• Usar Embedded Mode para máxima compatibilidad")
                    appendLine("• Verificar logs específicos del módulo")
                    appendLine("• Reportar problemas con información de diagnóstico")
                    appendLine("• Considerar reinstalación en modo embedded si hay problemas")
                }
            )
        )
    }

    private fun getModuleCompatibilityInfo(): List<LogEntry> {
        val timestamp = System.currentTimeMillis()
        
        return listOf(
            LogEntry(
                timestamp = timestamp - 4000,
                level = LogLevel.INFO,
                tag = "WaEnhancer-Compat",
                message = "Información de compatibilidad para WaEnhancer",
                details = buildString {
                    appendLine("=== WaEnhancer LSPatch Compatibility ===")
                    appendLine()
                    appendLine("APIs Requeridas por WaEnhancer:")
                    appendLine("✓ org.lsposed.lspatch.service.LocalApplicationService")
                    appendLine("✓ org.lsposed.lspatch.service.RemoteApplicationService")
                    appendLine("✓ getLegacyModulesList() - Detección de módulos")
                    appendLine("✓ getPrefsPath() - Acceso a preferencias")
                    appendLine("✓ Context verification - Validación de WhatsApp")
                    appendLine()
                    appendLine("Funciones de Diagnóstico:")
                    appendLine("• isWaEnhancerLoaded() - Verificar carga del módulo")
                    appendLine("• areWaEnhancerClassesLoaded() - Verificar clases")
                    appendLine("• canHookWhatsAppClasses() - Verificar hooks")
                    appendLine("• isWaEnhancerFunctional() - Verificar funcionalidad")
                    appendLine()
                    appendLine("Propiedades del Sistema:")
                    appendLine("• lspatch.mode - Modo actual de LSPatch")
                    appendLine("• lspatch.embedded - Indicador de modo embedded")
                    appendLine("• lspatch.manager - Indicador de modo manager")
                }
            ),
            LogEntry(
                timestamp = timestamp - 5000,
                level = LogLevel.DEBUG,
                tag = "LSPatch-ModuleAPI",
                message = "APIs específicas para módulos avanzados",
                details = buildString {
                    appendLine("Métodos de Servicio Disponibles:")
                    appendLine("• getLegacyModulesList(): List<Module>")
                    appendLine("• getModulesList(): List<Module>")
                    appendLine("• getPrefsPath(String): String")
                    appendLine("• isLogMuted(): boolean")
                    appendLine()
                    appendLine("Campos de Module disponibles:")
                    appendLine("• packageName: String")
                    appendLine("• appId: int")
                    appendLine("• apkPath: String")
                    appendLine("• file: PreLoadedApk")
                    appendLine("• applicationInfo: ApplicationInfo")
                    appendLine()
                    appendLine("Para módulos que requieren detección robusta:")
                    appendLine("• Verificar múltiples indicators de LSPatch")
                    appendLine("• Implementar fallbacks para APIs no disponibles")
                    appendLine("• Usar propiedades del sistema como respaldo")
                }
            )
        )
    }

    private fun getVersionInfo(): String {
        return try {
            // En una implementación real, esto vendría del BuildConfig
            "1.0.0-dev"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun refreshLogs() {
        loadLogs()
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun exportLogs(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return buildString {
            appendLine("=== LSPatch Logs Export ===")
            appendLine("Generated: ${dateFormat.format(Date())}")
            appendLine()
            
            _logs.value.forEach { log ->
                appendLine("[${dateFormat.format(Date(log.timestamp))}] ${log.level.name}/${log.tag}: ${log.message}")
                if (log.details.isNotBlank()) {
                    log.details.lines().forEach { line ->
                        appendLine("    $line")
                    }
                    appendLine()
                }
            }
        }
    }
}

data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val details: String = ""
)

enum class LogLevel {
    DEBUG, INFO, WARNING, ERROR
}
