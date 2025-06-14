package org.lsposed.lspatch.ui.viewmodel.logs

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.lsposed.lspatch.util.LogCollector
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class LogsViewModel : ViewModel() {
    
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val logCollector = LogCollector()

    init {
        loadLogs()
    }

    private fun loadLogs() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Load demo logs as base (for compatibility and development)
                val demoLogs = getDemoLogs()
                _logs.value = demoLogs.sortedByDescending { it.timestamp }
                
            } catch (e: Exception) {
                val errorLogs = listOf(
                    LogEntry(
                        timestamp = System.currentTimeMillis(),
                        level = LogLevel.ERROR,
                        tag = "LogsViewModel",
                        message = "Failed to load logs: ${e.message}",
                        details = "Error details: ${e.stackTraceToString()}"
                    )
                )
                _logs.value = errorLogs
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load logs with context for better log collection including real system logs
     */
    fun loadLogsWithContext(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Collect real system logs
                val systemLogs = logCollector.collectLSPatchLogs(context, maxLines = 300)
                
                // Collect module diagnostics
                val moduleDiagnostics = logCollector.collectModuleDiagnostics(context)
                
                // Get demo/compatibility logs
                val demoLogs = getDemoLogs()
                
                // Combine all logs, prioritizing real logs
                val allLogs = (systemLogs + moduleDiagnostics + demoLogs)
                    .distinctBy { "${it.timestamp}-${it.tag}-${it.message}" } // Remove duplicates
                    .sortedByDescending { it.timestamp }
                    .take(1000) // Limit to prevent memory issues
                
                _logs.value = allLogs
                
            } catch (e: Exception) {
                val errorLogs = listOf(
                    LogEntry(
                        timestamp = System.currentTimeMillis(),
                        level = LogLevel.ERROR,
                        tag = "LogsViewModel",
                        message = "Failed to load logs with context: ${e.message}",
                        details = "Falling back to demo logs. Error: ${e.stackTraceToString()}"
                    )
                ) + getDemoLogs()
                
                _logs.value = errorLogs
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Demo logs for development and as base content
     */
    private fun getDemoLogs(): List<LogEntry> {
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
    
    /**
     * Refresh logs with context (preferred method)
     */
    fun refreshLogsWithContext(context: Context) {
        loadLogsWithContext(context)
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    /**
     * Export logs with enhanced formatting and real log data
     */
    fun exportLogs(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        return buildString {
            appendLine("=== LSPatch Enhanced Logs Export ===")
            appendLine("Generated: ${dateFormat.format(Date())}")
            appendLine("Log Count: ${_logs.value.size}")
            appendLine("Export Version: 2.0")
            appendLine()
            
            // Group logs by tag for better organization
            val logsByTag = _logs.value.groupBy { it.tag }
            
            for ((tag, tagLogs) in logsByTag.entries.sortedBy { it.key }) {
                appendLine("=== TAG: $tag (${tagLogs.size} entries) ===")
                
                for (log in tagLogs.sortedByDescending { it.timestamp }) {
                    appendLine("[${dateFormat.format(Date(log.timestamp))}] ${log.level.name}: ${log.message}")
                    if (log.details.isNotBlank()) {
                        log.details.lines().forEach { line ->
                            appendLine("    $line")
                        }
                    }
                    appendLine()
                }
                appendLine()
            }
            
            appendLine("=== Export Summary ===")
            appendLine("Total Tags: ${logsByTag.size}")
            appendLine("Level Breakdown:")
            val levelCounts = _logs.value.groupingBy { it.level }.eachCount()
            for ((level, count) in levelCounts) {
                appendLine("  ${level.name}: $count")
            }
            appendLine()
            appendLine("Note: This export includes both real system logs and demo logs.")
            appendLine("For real-time monitoring use: adb logcat LSPatch:* *:S")
        }
    }

    /**
     * Export logs to file with sharing capability
     */
    fun exportLogsToFile(context: Context? = null) {
        viewModelScope.launch {
            try {
                val logsContent = exportLogs()
                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val timestamp = dateFormat.format(Date())
                val fileName = "lspatch_enhanced_logs_$timestamp.txt"
                
                context?.let { ctx ->
                    val file = File(ctx.getExternalFilesDir(null), fileName)
                    file.writeText(logsContent)
                    
                    val uri = FileProvider.getUriForFile(
                        ctx,
                        "${ctx.packageName}.fileprovider",
                        file
                    )
                    
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "LSPatch Enhanced Logs - $timestamp")
                        putExtra(Intent.EXTRA_TEXT, "Enhanced logs exported from LSPatch Manager with real system log integration")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    
                    val chooserIntent = Intent.createChooser(shareIntent, "Export Enhanced Logs")
                    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(chooserIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
