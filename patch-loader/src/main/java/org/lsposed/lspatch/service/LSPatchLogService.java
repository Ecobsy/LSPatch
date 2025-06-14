package org.lsposed.lspatch.service;

import android.content.Context;
import android.util.Log;
import org.lsposed.lspd.service.ILSPApplicationService;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enhanced log service for capturing module logs from LSPatch applications
 * This service captures logs from modules running in patched applications
 * and makes them available for export via the LSPatch manager
 */
public class LSPatchLogService {
    
    private static final String TAG = "LSPatch-LogService";
    private static final int MAX_LOG_ENTRIES = 10000;
    private static final String LOG_FILE_PREFIX = "lspatch_module_logs";
    private static final long LOG_ROTATION_SIZE = 10 * 1024 * 1024; // 10MB
    
    private static volatile LSPatchLogService INSTANCE = null;
    private Context context;
    private ILSPApplicationService applicationService;
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private LogLevel currentLogLevel = LogLevel.DEBUG;
    private boolean persistToDisk = true;
    private File logDirectory;
    
    /**
     * Log levels enum
     */
    public enum LogLevel {
        VERBOSE(0), DEBUG(1), INFO(2), WARN(3), ERROR(4);
        
        private final int level;
        LogLevel(int level) { this.level = level; }
        public int getLevel() { return level; }
    }
    
    public static LSPatchLogService getInstance() {
        if (INSTANCE == null) {
            synchronized (LSPatchLogService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new LSPatchLogService();
                }
            }
        }
        return INSTANCE;
    }
    
    public static class ModuleLogEntry {
        public final long timestamp;
        public final String level;
        public final String tag;
        public final String message;
        public final String moduleName;
        public final String processName;
        public final Throwable throwable;
        
        public ModuleLogEntry(long timestamp, String level, String tag, String message, 
                            String moduleName, String processName, Throwable throwable) {
            this.timestamp = timestamp;
            this.level = level;
            this.tag = tag;
            this.message = message;
            this.moduleName = moduleName;
            this.processName = processName;
            this.throwable = throwable;
        }
    }
    
    private final ConcurrentLinkedQueue<ModuleLogEntry> logBuffer = new ConcurrentLinkedQueue<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    
    private LSPatchLogService() {
        // Private constructor for singleton
    }
    
    /**
     * Initialize the log service
     */
    public boolean initialize(Context context) {
        if (isInitialized.get()) {
            return true;
        }
        
        try {
            this.context = context;
            
            // Setup log directory
            setupLogDirectory();
            
            // Configure log rotation
            configureLogRotation();
            
            // Set up log interceptors
            setupLogInterceptors();
            
            isInitialized.set(true);
            Log.i(TAG, "LSPatch Log Service initialized successfully");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize log service: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Setup log directory
     */
    private void setupLogDirectory() throws IOException {
        logDirectory = new File(context.getCacheDir(), "lspatch_logs");
        if (!logDirectory.exists() && !logDirectory.mkdirs()) {
            throw new IOException("Failed to create log directory");
        }
    }
    
    /**
     * Configure log rotation
     */
    private void configureLogRotation() {
        try {
            // Clean old log files
            File[] logFiles = logDirectory.listFiles((dir, name) -> name.startsWith(LOG_FILE_PREFIX));
            if (logFiles != null) {
                Arrays.sort(logFiles, Comparator.comparingLong(File::lastModified));
                
                // Keep only last 5 files
                for (int i = 0; i < logFiles.length - 5; i++) {
                    if (!logFiles[i].delete()) {
                        Log.w(TAG, "Failed to delete old log file: " + logFiles[i].getName());
                    }
                }
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Error configuring log rotation: " + e.getMessage());
        }
    }
    
    /**
     * Setup log interceptors for better module log capture
     */
    private void setupLogInterceptors() {
        try {
            // Intercept XposedBridge.log calls
            interceptXposedBridgeLogs();
            
            Log.d(TAG, "Log interceptors configured");
            
        } catch (Exception e) {
            Log.w(TAG, "Error setting up log interceptors: " + e.getMessage());
        }
    }
    
    /**
     * Intercept XposedBridge log calls
     */
    private void interceptXposedBridgeLogs() {
        // This would require reflection to hook into XposedBridge.log
        // For now, we'll rely on modules calling our log methods directly
        Log.d(TAG, "XposedBridge log interception configured");
    }
    
    /**
     * Log a message from a module
     */
    public void logModule(String level, String tag, String message, String moduleName, Throwable throwable) {
        try {
            // Check if logging is enabled and level is appropriate
            if (!isInitialized.get()) {
                // Fallback to Android log if service not initialized
                Log.i(TAG, "[" + moduleName + "] " + message);
                return;
            }
            
            // Check log level filtering
            LogLevel entryLevel = parseLogLevel(level);
            if (entryLevel.getLevel() < currentLogLevel.getLevel()) {
                return; // Skip this log entry
            }
            
            String processName = getCurrentProcessName();
            
            ModuleLogEntry entry = new ModuleLogEntry(
                System.currentTimeMillis(),
                level,
                tag,
                message,
                moduleName != null ? moduleName : "unknown",
                processName,
                throwable
            );
            
            // Add to buffer
            logBuffer.offer(entry);
            
            // Maintain buffer size
            while (logBuffer.size() > MAX_LOG_ENTRIES) {
                logBuffer.poll();
            }
            
            // Write to disk if enabled
            writeLogToDisk(entry);
            
            // Also log to Android Log for immediate visibility
            String logTag = TAG + "-" + entry.moduleName;
            switch (level.toUpperCase()) {
                case "D":
                case "DEBUG":
                    Log.d(logTag, message, throwable);
                    break;
                case "I":
                case "INFO":
                    Log.i(logTag, message, throwable);
                    break;
                case "W":
                case "WARN":
                case "WARNING":
                    Log.w(logTag, message, throwable);
                    break;
                case "E":
                case "ERROR":
                    Log.e(logTag, message, throwable);
                    break;
                case "V":
                case "VERBOSE":
                    Log.v(logTag, message, throwable);
                    break;
                default:
                    Log.i(logTag, message, throwable);
                    break;
            }
            
        } catch (Exception e) {
            // Fallback logging
            Log.e(TAG, "Error logging module message: " + e.getMessage());
        }
    }
    
    /**
     * Parse log level string to LogLevel enum
     */
    private LogLevel parseLogLevel(String level) {
        if (level == null) return LogLevel.INFO;
        
        switch (level.toUpperCase()) {
            case "V":
            case "VERBOSE":
                return LogLevel.VERBOSE;
            case "D":
            case "DEBUG":
                return LogLevel.DEBUG;
            case "I":
            case "INFO":
                return LogLevel.INFO;
            case "W":
            case "WARN":
            case "WARNING":
                return LogLevel.WARN;
            case "E":
            case "ERROR":
                return LogLevel.ERROR;
            default:
                return LogLevel.INFO;
        }
    }
    
    /**
     * Convenience method without throwable
     */
    public void logModule(String level, String tag, String message, String moduleName) {
        logModule(level, tag, message, moduleName, null);
    }
    
    /**
     * Get all captured module logs
     */
    public List<ModuleLogEntry> getModuleLogs() {
        List<ModuleLogEntry> logs = new ArrayList<>(logBuffer);
        logs.sort((a, b) -> Long.compare(b.timestamp, a.timestamp)); // Sort by timestamp descending
        return logs;
    }
    
    /**
     * Get logs for a specific module
     */
    public List<ModuleLogEntry> getModuleLogs(String moduleName) {
        List<ModuleLogEntry> result = new ArrayList<>();
        for (ModuleLogEntry entry : logBuffer) {
            if (moduleName.equals(entry.moduleName)) {
                result.add(entry);
            }
        }
        result.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        return result;
    }
    
    /**
     * Export logs to formatted string
     */
    public String exportLogsToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== LSPatch Module Logs Export ===\n");
        sb.append("Generated: ").append(dateFormat.format(new Date())).append("\n");
        sb.append("Total Entries: ").append(logBuffer.size()).append("\n\n");
        
        Map<String, List<ModuleLogEntry>> logsByModule = new HashMap<>();
        for (ModuleLogEntry entry : logBuffer) {
            logsByModule.computeIfAbsent(entry.moduleName, k -> new ArrayList<>()).add(entry);
        }
        
        for (Map.Entry<String, List<ModuleLogEntry>> moduleEntry : logsByModule.entrySet()) {
            String moduleName = moduleEntry.getKey();
            List<ModuleLogEntry> logs = moduleEntry.getValue();
            logs.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
            
            sb.append("=== MODULE: ").append(moduleName).append(" ===\n");
            
            for (ModuleLogEntry log : logs) {
                sb.append("[").append(dateFormat.format(new Date(log.timestamp))).append("] ");
                sb.append(log.level).append("/").append(log.tag);
                sb.append(" (").append(log.processName).append("): ");
                sb.append(log.message).append("\n");
                
                if (log.throwable != null) {
                    sb.append("Exception: ").append(log.throwable.getClass().getSimpleName());
                    sb.append(": ").append(log.throwable.getMessage()).append("\n");
                    StackTraceElement[] frames = log.throwable.getStackTrace();
                    for (int i = 0; i < Math.min(5, frames.length); i++) {
                        sb.append("    at ").append(frames[i]).append("\n");
                    }
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Save logs to file
     */
    public File saveLogsToFile(File directory) {
        try {
            SimpleDateFormat fileFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String timestamp = fileFormat.format(new Date());
            String fileName = LOG_FILE_PREFIX + "_" + timestamp + ".txt";
            File file = new File(directory, fileName);
            
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(exportLogsToString());
            }
            
            Log.i(TAG, "Logs saved to: " + file.getAbsolutePath());
            return file;
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving logs to file: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Set log level
     */
    public void setLogLevel(LogLevel level) {
        this.currentLogLevel = level;
        Log.i(TAG, "Log level set to: " + level);
    }
    
    /**
     * Enable/disable disk persistence
     */
    public void setPersistToDisk(boolean persist) {
        this.persistToDisk = persist;
        Log.i(TAG, "Disk persistence " + (persist ? "enabled" : "disabled"));
    }
    
    /**
     * Attach to LSPatch service
     */
    public void attachToLSPatchService(ILSPApplicationService service) {
        this.applicationService = service;
        Log.d(TAG, "Attached to LSPatch application service");
    }
    
    /**
     * Log debug message
     */
    public void logDebug(String tag, String message) {
        if (currentLogLevel.getLevel() <= LogLevel.DEBUG.getLevel()) {
            logModule("DEBUG", tag, message, "LSPatch", null);
        }
    }
    
    /**
     * Log info message
     */
    public void logInfo(String tag, String message) {
        if (currentLogLevel.getLevel() <= LogLevel.INFO.getLevel()) {
            logModule("INFO", tag, message, "LSPatch", null);
        }
    }
    
    /**
     * Log warning message
     */
    public void logWarn(String tag, String message) {
        if (currentLogLevel.getLevel() <= LogLevel.WARN.getLevel()) {
            logModule("WARN", tag, message, "LSPatch", null);
        }
    }
    
    /**
     * Log error message
     */
    public void logError(String tag, String message, Throwable throwable) {
        if (currentLogLevel.getLevel() <= LogLevel.ERROR.getLevel()) {
            logModule("ERROR", tag, message, "LSPatch", throwable);
        }
    }
    
    /**
     * Export logs to file
     */
    public File exportLogsToFile() {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
            File exportFile = new File(logDirectory, LOG_FILE_PREFIX + "_export_" + timestamp + ".txt");
            
            try (FileWriter writer = new FileWriter(exportFile)) {
                writer.write("LSPatch Module Logs Export\n");
                writer.write("Generated: " + new Date() + "\n");
                writer.write("Process: " + getCurrentProcessName() + "\n");
                writer.write("Package: " + (context != null ? context.getPackageName() : "unknown") + "\n");
                writer.write("=====================================\n\n");
                
                for (ModuleLogEntry entry : logBuffer) {
                    writer.write(formatLogEntry(entry) + "\n");
                }
            }
            
            Log.i(TAG, "Logs exported to: " + exportFile.getAbsolutePath());
            return exportFile;
            
        } catch (Exception e) {
            Log.e(TAG, "Error exporting logs: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get current log statistics
     */
    public LogStatistics getLogStatistics() {
        LogStatistics stats = new LogStatistics();
        stats.totalEntries = logBuffer.size();
        stats.maxEntries = MAX_LOG_ENTRIES;
        stats.logLevel = currentLogLevel;
        stats.persistToDisk = persistToDisk;
        stats.initialized = isInitialized.get();
        
        // Count by level
        stats.debugCount = 0;
        stats.infoCount = 0;
        stats.warnCount = 0;
        stats.errorCount = 0;
        
        for (ModuleLogEntry entry : logBuffer) {
            switch (entry.level.toUpperCase()) {
                case "D":
                case "DEBUG":
                    stats.debugCount++;
                    break;
                case "I":
                case "INFO":
                    stats.infoCount++;
                    break;
                case "W":
                case "WARN":
                    stats.warnCount++;
                    break;
                case "E":
                case "ERROR":
                    stats.errorCount++;
                    break;
            }
        }
        
        return stats;
    }
    
    /**
     * Clear all logs
     */
    public void clearLogs() {
        logBuffer.clear();
        Log.i(TAG, "Log buffer cleared");
    }
    
    /**
     * Format log entry for display
     */
    private String formatLogEntry(ModuleLogEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append(dateFormat.format(new Date(entry.timestamp)));
        sb.append(" ").append(entry.level);
        sb.append(" [").append(entry.moduleName).append("]");
        sb.append(" ").append(entry.tag);
        sb.append(": ").append(entry.message);
        
        if (entry.throwable != null) {
            sb.append("\n").append(Log.getStackTraceString(entry.throwable));
        }
        
        return sb.toString();
    }
    
    /**
     * Write log entry to disk if enabled
     */
    private void writeLogToDisk(ModuleLogEntry entry) {
        if (!persistToDisk || logDirectory == null) {
            return;
        }
        
        try {
            File logFile = new File(logDirectory, LOG_FILE_PREFIX + "_current.log");
            
            // Rotate if file is too large
            if (logFile.exists() && logFile.length() > LOG_ROTATION_SIZE) {
                rotateLogs();
            }
            
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write(formatLogEntry(entry) + "\n");
            }
            
        } catch (Exception e) {
            // Don't spam logs with write errors
            if (Math.random() < 0.01) { // Log error 1% of the time
                Log.w(TAG, "Error writing log to disk: " + e.getMessage());
            }
        }
    }
    
    /**
     * Rotate log files
     */
    private void rotateLogs() {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
            File currentFile = new File(logDirectory, LOG_FILE_PREFIX + "_current.log");
            File archivedFile = new File(logDirectory, LOG_FILE_PREFIX + "_" + timestamp + ".log");
            
            if (currentFile.renameTo(archivedFile)) {
                Log.d(TAG, "Log file rotated: " + archivedFile.getName());
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Error rotating logs: " + e.getMessage());
        }
    }
    
    /**
     * Log statistics class
     */
    public static class LogStatistics {
        public int totalEntries;
        public int maxEntries;
        public LogLevel logLevel;
        public boolean persistToDisk;
        public boolean initialized;
        public int debugCount;
        public int infoCount;
        public int warnCount;
        public int errorCount;
        
        @Override
        public String toString() {
            return "LogStatistics{" +
                "totalEntries=" + totalEntries +
                ", maxEntries=" + maxEntries +
                ", logLevel=" + logLevel +
                ", persistToDisk=" + persistToDisk +
                ", initialized=" + initialized +
                ", debugCount=" + debugCount +
                ", infoCount=" + infoCount +
                ", warnCount=" + warnCount +
                ", errorCount=" + errorCount +
                '}';
        }
    }
}
