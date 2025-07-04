package org.lsposed.lspatch.service;

import android.content.Context;
import android.os.Environment;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import org.lsposed.lspatch.loader.util.FileUtils;
import org.lsposed.lspatch.share.Constants;
import org.lsposed.lspatch.util.ModuleLoader;
import org.lsposed.lspd.models.Module;
import org.lsposed.lspd.service.ILSPApplicationService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

public class LocalApplicationService extends ILSPApplicationService.Stub {

    private static final String TAG = "LSPatch";

    private final List<Module> modules = new ArrayList<>();
    private final Context context;
    private LSPatchLogService logService;

    public LocalApplicationService(Context context) {
        this.context = context;
        
        // Initialize log service for capturing module logs
        this.logService = LSPatchLogService.getInstance();
        this.logService.attachToLSPatchService(this);
        
        // Set system properties to indicate LSPatch environment for module detection
        setLSPatchSystemProperties();
        
        try {
            for (var name : context.getAssets().list("lspatch/modules")) {
                String packageName = name.substring(0, name.length() - 4);
                String modulePath = context.getCacheDir() + "/lspatch/" + packageName + "/";
                String cacheApkPath;
                try (ZipFile sourceFile = new ZipFile(context.getPackageResourcePath())) {
                    cacheApkPath = modulePath + sourceFile.getEntry(Constants.EMBEDDED_MODULES_ASSET_PATH + name).getCrc() + ".apk";
                }

                if (!Files.exists(Paths.get(cacheApkPath))) {
                    Log.i(TAG, "Extract module apk: " + packageName);
                    FileUtils.deleteFolderIfExists(Paths.get(modulePath));
                    Files.createDirectories(Paths.get(modulePath));
                    try (var is = context.getAssets().open("lspatch/modules/" + name)) {
                        Files.copy(is, Paths.get(cacheApkPath));
                    }
                }

                var module = new Module();
                module.apkPath = cacheApkPath;
                module.packageName = packageName;
                module.file = ModuleLoader.loadModule(cacheApkPath);
                modules.add(module);
                
                Log.i(TAG, "Loaded module: " + packageName + " from " + cacheApkPath);
            }
            
            Log.i(TAG, "LocalApplicationService initialized with " + modules.size() + " modules");
            
        } catch (IOException e) {
            Log.e(TAG, "Error when initializing LocalApplicationServiceClient", e);
        }
    }

    /**
     * Set system properties to help modules detect LSPatch environment
     */
    private void setLSPatchSystemProperties() {
        try {
            System.setProperty("lspatch.mode", "embedded");
            System.setProperty("lspatch.embedded", "true");
            System.setProperty("lspatch.local", "true");
            System.setProperty("lspatch.version", "1.0.0");
            Log.d(TAG, "LSPatch system properties set for module detection");
        } catch (Exception e) {
            Log.w(TAG, "Could not set all LSPatch system properties: " + e.getMessage());
        }
    }

    @Override
    public boolean isLogMuted() throws RemoteException {
        return false;
    }

    @Override
    public List<Module> getLegacyModulesList() {
        Log.d(TAG, "getLegacyModulesList called, returning " + modules.size() + " modules");
        
        // Enhanced logging for module detection
        for (Module module : modules) {
            Log.d(TAG, "Module available: packageName=" + module.packageName + 
                     ", apkPath=" + module.apkPath + 
                     ", loaded=" + (module.file != null));
        }
        
        return modules;
    }

    @Override
    public List<Module> getModulesList() {
        return new ArrayList<>();
    }

    @Override
    public String getPrefsPath(String packageName) {
        String prefsPath = new File(Environment.getDataDirectory(), "data/" + packageName + "/shared_prefs/").getAbsolutePath();
        Log.d(TAG, "getPrefsPath called for " + packageName + ", returning: " + prefsPath);
        return prefsPath;
    }

    @Override
    public ParcelFileDescriptor requestInjectedManagerBinder(List<IBinder> binder) {
        return null;
    }

    @Override
    public IBinder asBinder() {
        return this;
    }

    /**
     * Enhanced method for module diagnostics
     * This method provides detailed information about the current LSPatch state
     * for debugging and compatibility verification
     */
    public String getDetailedStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== LSPatch LocalApplicationService Status ===\n");
        status.append("Service Type: LocalApplicationService (Embedded Mode)\n");
        status.append("Context Package: ").append(context.getPackageName()).append("\n");
        status.append("Modules Loaded: ").append(modules.size()).append("\n");
        
        status.append("\nModule Details:\n");
        for (Module module : modules) {
            status.append("- Package: ").append(module.packageName).append("\n");
            status.append("  APK Path: ").append(module.apkPath).append("\n");
            status.append("  File Loaded: ").append(module.file != null).append("\n");
            if (module.file != null) {
                status.append("  Legacy: ").append(module.file.legacy).append("\n");
                status.append("  Dex Count: ").append(module.file.preLoadedDexes.size()).append("\n");
                status.append("  Classes: ").append(module.file.moduleClassNames.size()).append("\n");
            }
            status.append("\n");
        }
        
        status.append("System Properties:\n");
        String[] lspatchProps = {
            "lspatch.mode", "lspatch.embedded", "lspatch.local", 
            "lspatch.version", "java.class.path"
        };
        
        for (String prop : lspatchProps) {
            String value = System.getProperty(prop);
            if (value != null) {
                status.append("- ").append(prop).append("=").append(value).append("\n");
            }
        }
        
        return status.toString();
    }

    /**
     * Check if a specific module package is loaded
     */
    public boolean isModuleLoaded(String packageName) {
        for (Module module : modules) {
            if (packageName.equals(module.packageName)) {
                Log.d(TAG, "Module " + packageName + " is loaded");
                return true;
            }
        }
        Log.d(TAG, "Module " + packageName + " is NOT loaded");
        return false;
    }

    /**
     * Get module information for a specific package
     */
    public Module getModuleInfo(String packageName) {
        for (Module module : modules) {
            if (packageName.equals(module.packageName)) {
                return module;
            }
        }
        return null;
    }

    /**
     * Enhanced health check method
     */
    public boolean performHealthCheck() {
        try {
            // Verify context is available
            if (context == null) {
                Log.w(TAG, "Health check failed: context is null");
                return false;
            }
            
            // Verify we can access package info
            String packageName = context.getPackageName();
            if (packageName == null || packageName.isEmpty()) {
                Log.w(TAG, "Health check failed: package name is null/empty");
                return false;
            }
            
            // Verify modules list is accessible
            List<Module> modulesList = getLegacyModulesList();
            if (modulesList == null) {
                Log.w(TAG, "Health check failed: modules list is null");
                return false;
            }
            
            // Log health check via log service
            if (logService != null) {
                logService.logModule("INFO", TAG, 
                    "Health check passed - Service functional with " + modulesList.size() + " modules",
                    "LSPatch-Health");
            }
            
            Log.i(TAG, "Health check passed: Service is functional");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Health check failed with exception: " + e.getMessage());
            if (logService != null) {
                logService.logModule("ERROR", TAG, 
                    "Health check failed: " + e.getMessage(),
                    "LSPatch-Health", e);
            }
            return false;
        }
    }
    
    /**
     * Get captured module logs
     */
    public String getModuleLogs() {
        if (logService != null) {
            return logService.exportLogsToString();
        }
        return "Log service not available";
    }
    
    /**
     * Get log statistics
     */
    public String getLogStatistics() {
        if (logService != null) {
            LSPatchLogService.LogStatistics stats = logService.getLogStatistics();
            StringBuilder sb = new StringBuilder();
            sb.append("=== Log Statistics ===\n");
            sb.append("totalEntries: ").append(stats.totalEntries).append("\n");
            sb.append("maxEntries: ").append(stats.maxEntries).append("\n");
            sb.append("logLevel: ").append(stats.logLevel).append("\n");
            sb.append("persistToDisk: ").append(stats.persistToDisk).append("\n");
            sb.append("initialized: ").append(stats.initialized).append("\n");
            sb.append("debugCount: ").append(stats.debugCount).append("\n");
            sb.append("infoCount: ").append(stats.infoCount).append("\n");
            sb.append("warnCount: ").append(stats.warnCount).append("\n");
            sb.append("errorCount: ").append(stats.errorCount).append("\n");
            return sb.toString();
        }
        return "Log service not available";
    }
    
    /**
     * Clear module logs
     */
    public void clearModuleLogs() {
        if (logService != null) {
            logService.clearLogs();
            Log.i(TAG, "Module logs cleared via LocalApplicationService");
        }
    }
}
