package org.lsposed.lspatch.service

import android.util.Log
import org.lsposed.lspd.service.ILSPApplicationService
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Enhanced log service for capturing module logs from LSPatch applications
 * This service captures logs from modules running in patched applications
 * and makes them available for export via the LSPatch manager
 */
class LSPatchLogService private constructor() {
    
    companion object {
        private const val TAG = "LSPatch-LogService"
        private const val MAX_LOG_ENTRIES = 1000
        private const val LOG_FILE_PREFIX = "lspatch_module_logs"
        
        @Volatile
        private var INSTANCE: LSPatchLogService? = null
        
        fun getInstance(): LSPatchLogService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LSPatchLogService().also { INSTANCE = it }
            }
        }
    }
    
    data class ModuleLogEntry(
        val timestamp: Long,
        val level: String,
        val tag: String,
        val message: String,
        val moduleName: String,
        val processName: String,
        val throwable: Throwable? = null
    )
    
    private val logBuffer = ConcurrentLinkedQueue<ModuleLogEntry>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    /**
     * Log a message from a module
     */
    fun logModule(level: String, tag: String, message: String, moduleName: String = "unknown", throwable: Throwable? = null) {
        try {
            val processName = getCurrentProcessName()
            
            val entry = ModuleLogEntry(
                timestamp = System.currentTimeMillis(),
                level = level,
                tag = tag,
                message = message,
                moduleName = moduleName,
                processName = processName,
                throwable = throwable
            )
            
            // Add to buffer
            logBuffer.offer(entry)
            
            // Maintain buffer size
            while (logBuffer.size > MAX_LOG_ENTRIES) {
                logBuffer.poll()
            }
            
            // Also log to Android Log for immediate visibility
            when (level.uppercase()) {
                "D", "DEBUG" -> Log.d("$TAG-$moduleName", message, throwable)
                "I", "INFO" -> Log.i("$TAG-$moduleName", message, throwable)
                "W", "WARN", "WARNING" -> Log.w("$TAG-$moduleName", message, throwable)
                "E", "ERROR" -> Log.e("$TAG-$moduleName", message, throwable)
                else -> Log.i("$TAG-$moduleName", message, throwable)
            }
            
        } catch (e: Exception) {
            // Fallback logging
            Log.e(TAG, "Error logging module message: ${e.message}")
        }
    }
    
    /**
     * Get all captured module logs
     */
    fun getModuleLogs(): List<ModuleLogEntry> {
        return logBuffer.toList().sortedByDescending { it.timestamp }
    }
    
    /**
     * Get logs for a specific module
     */
    fun getModuleLogs(moduleName: String): List<ModuleLogEntry> {
        return logBuffer.filter { it.moduleName == moduleName }
            .sortedByDescending { it.timestamp }
    }
    
    /**
     * Export logs to formatted string
     */
    fun exportLogsToString(): String {
        return buildString {
            appendLine("=== LSPatch Module Logs Export ===")
            appendLine("Generated: ${dateFormat.format(Date())}")
            appendLine("Total Entries: ${logBuffer.size}")
            appendLine()
            
            val logsByModule = logBuffer.groupBy { it.moduleName }
            
            for ((moduleName, logs) in logsByModule) {
                appendLine("=== MODULE: $moduleName ===")
                
                for (log in logs.sortedByDescending { it.timestamp }) {
                    appendLine("[${dateFormat.format(Date(log.timestamp))}] ${log.level}/${log.tag} (${log.processName}): ${log.message}")
                    
                    log.throwable?.let { throwable ->
                        appendLine("Exception: ${throwable.javaClass.simpleName}: ${throwable.message}")
                        throwable.stackTrace.take(5).forEach { frame ->
                            appendLine("    at $frame")
                        }
                    }
                    appendLine()
                }
                appendLine()
            }
        }
    }
    
    /**
     * Save logs to file
     */
    fun saveLogsToFile(directory: File): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${LOG_FILE_PREFIX}_$timestamp.txt"
            val file = File(directory, fileName)
            
            FileWriter(file).use { writer ->
                writer.write(exportLogsToString())
            }
            
            Log.i(TAG, "Logs saved to: ${file.absolutePath}")
            file
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving logs to file: ${e.message}")
            null
        }
    }
    
    /**
     * Clear all logs
     */
    fun clearLogs() {
        logBuffer.clear()
        Log.i(TAG, "Module logs cleared")
    }
    
    /**
     * Get current process name
     */
    private fun getCurrentProcessName(): String {
        return try {
            val pid = android.os.Process.myPid()
            val manager = android.app.ActivityManager::class.java
            val getRunningAppProcesses = manager.getMethod("getRunningAppProcesses")
            
            // This is a simplified approach - in practice you might want to use different methods
            "process_$pid"
        } catch (e: Exception) {
            "unknown_process"
        }
    }
    
    /**
     * Integration with LSPatch service for log forwarding
     */
    fun attachToLSPatchService(service: ILSPApplicationService?) {
        try {
            if (service != null) {
                Log.i(TAG, "LSPatch log service attached to application service")
                
                // Log service startup
                logModule("INFO", TAG, "Module log service started", "LSPatch-Core")
                logModule("INFO", TAG, "Ready to capture module logs", "LSPatch-Core")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching to LSPatch service: ${e.message}")
        }
    }
    
    /**
     * Get log statistics
     */
    fun getLogStatistics(): Map<String, Any> {
        val logs = logBuffer.toList()
        val moduleStats = logs.groupBy { it.moduleName }.mapValues { it.value.size }
        val levelStats = logs.groupBy { it.level }.mapValues { it.value.size }
        
        return mapOf(
            "totalLogs" to logs.size,
            "moduleBreakdown" to moduleStats,
            "levelBreakdown" to levelStats,
            "oldestLog" to logs.minOfOrNull { it.timestamp },
            "newestLog" to logs.maxOfOrNull { it.timestamp }
        )
    }
}
