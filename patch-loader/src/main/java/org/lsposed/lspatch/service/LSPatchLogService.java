package org.lsposed.lspatch.service;

import android.util.Log;
import org.lsposed.lspd.service.ILSPApplicationService;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Enhanced log service for capturing module logs from LSPatch applications
 * This service captures logs from modules running in patched applications
 * and makes them available for export via the LSPatch manager
 */
public class LSPatchLogService {
    
    private static final String TAG = "LSPatch-LogService";
    private static final int MAX_LOG_ENTRIES = 1000;
    private static final String LOG_FILE_PREFIX = "lspatch_module_logs";
    
    private static volatile LSPatchLogService INSTANCE = null;
    
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
     * Log a message from a module
     */
    public void logModule(String level, String tag, String message, String moduleName, Throwable throwable) {
        try {
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
     * Clear all logs
     */
    public void clearLogs() {
        logBuffer.clear();
        Log.i(TAG, "Module logs cleared");
    }
    
    /**
     * Get current process name
     */
    private String getCurrentProcessName() {
        try {
            int pid = android.os.Process.myPid();
            return "process_" + pid;
        } catch (Exception e) {
            return "unknown_process";
        }
    }
    
    /**
     * Integration with LSPatch service for log forwarding
     */
    public void attachToLSPatchService(ILSPApplicationService service) {
        try {
            if (service != null) {
                Log.i(TAG, "LSPatch log service attached to application service");
                
                // Log service startup
                logModule("INFO", TAG, "Module log service started", "LSPatch-Core");
                logModule("INFO", TAG, "Ready to capture module logs", "LSPatch-Core");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error attaching to LSPatch service: " + e.getMessage());
        }
    }
    
    /**
     * Get log statistics
     */
    public Map<String, Object> getLogStatistics() {
        List<ModuleLogEntry> logs = new ArrayList<>(logBuffer);
        
        Map<String, Integer> moduleStats = new HashMap<>();
        Map<String, Integer> levelStats = new HashMap<>();
        long oldestLog = Long.MAX_VALUE;
        long newestLog = Long.MIN_VALUE;
        
        for (ModuleLogEntry log : logs) {
            moduleStats.put(log.moduleName, moduleStats.getOrDefault(log.moduleName, 0) + 1);
            levelStats.put(log.level, levelStats.getOrDefault(log.level, 0) + 1);
            oldestLog = Math.min(oldestLog, log.timestamp);
            newestLog = Math.max(newestLog, log.timestamp);
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLogs", logs.size());
        stats.put("moduleBreakdown", moduleStats);
        stats.put("levelBreakdown", levelStats);
        stats.put("oldestLog", oldestLog == Long.MAX_VALUE ? null : oldestLog);
        stats.put("newestLog", newestLog == Long.MIN_VALUE ? null : newestLog);
        
        return stats;
    }
}
