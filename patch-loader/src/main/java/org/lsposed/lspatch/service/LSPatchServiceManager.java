package org.lsposed.lspatch.service;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.lsposed.lspd.service.ILSPApplicationService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Servicio principal de LSPatch que gestiona todas las operaciones sin root
 * Utiliza Shizuku y otras alternativas para proporcionar funcionalidad completa
 */
public class LSPatchServiceManager {
    
    private static final String TAG = "LSPatch-ServiceManager";
    private static LSPatchServiceManager instance;
    
    private Context context;
    private ILSPApplicationService applicationService;
    private LSPatchShizukuService shizukuService;
    private LSPatchLogService logService;
    private LSPatchHookService hookService;
    private LSPatchModuleService moduleService;
    private boolean isInitialized = false;
    
    private LSPatchServiceManager() {}
    
    public static synchronized LSPatchServiceManager getInstance() {
        if (instance == null) {
            instance = new LSPatchServiceManager();
        }
        return instance;
    }
    
    /**
     * Inicializa todos los servicios de LSPatch
     */
    public boolean initialize(Context context, ILSPApplicationService appService) {
        if (isInitialized) {
            return true;
        }
        
        this.context = context;
        this.applicationService = appService;
        
        try {
            Log.i(TAG, "Initializing LSPatch Service Manager");
            
            // Inicializar servicio de logs mejorado
            initializeLogService();
            
            // Inicializar servicio Shizuku (si está disponible)
            initializeShizukuService();
            
            // Inicializar servicio de hooks
            initializeHookService();
            
            // Inicializar servicio de módulos
            initializeModuleService();
            
            // Configurar propiedades del sistema para detección
            setupSystemProperties();
            
            isInitialized = true;
            Log.i(TAG, "LSPatch Service Manager initialized successfully");
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize LSPatch Service Manager: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Inicializa el servicio de logs mejorado
     */
    private void initializeLogService() {
        try {
            logService = LSPatchLogService.getInstance();
            logService.initialize(context);
            logService.setLogLevel(LSPatchLogService.LogLevel.DEBUG);
            
            Log.i(TAG, "Log service initialized");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize log service: " + e.getMessage());
        }
    }
    
    /**
     * Inicializa el servicio Shizuku para operaciones que requieren permisos
     */
    private void initializeShizukuService() {
        try {
            shizukuService = new LSPatchShizukuService();
            boolean shizukuAvailable = shizukuService.initialize(context);
            
            if (shizukuAvailable) {
                Log.i(TAG, "Shizuku service initialized successfully");
            } else {
                Log.w(TAG, "Shizuku service not available, using fallback methods");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Shizuku service: " + e.getMessage());
        }
    }
    
    /**
     * Inicializa el servicio de hooks
     */
    private void initializeHookService() {
        try {
            hookService = new LSPatchHookService();
            hookService.initialize(context, applicationService, shizukuService);
            
            Log.i(TAG, "Hook service initialized");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize hook service: " + e.getMessage());
        }
    }
    
    /**
     * Inicializa el servicio de módulos
     */
    private void initializeModuleService() {
        try {
            moduleService = new LSPatchModuleService();
            moduleService.initialize(context, applicationService, hookService);
            
            Log.i(TAG, "Module service initialized");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize module service: " + e.getMessage());
        }
    }
    
    /**
     * Configura propiedades del sistema para detección de LSPatch
     */
    private void setupSystemProperties() {
        try {
            // Propiedades básicas de LSPatch
            System.setProperty("lspatch.version", "2.0");
            System.setProperty("lspatch.initialized", "true");
            System.setProperty("lspatch.noroot", "true");
            System.setProperty("lspatch.services.active", "true");
            
            // Propiedades de servicios disponibles
            System.setProperty("lspatch.log.service", "true");
            System.setProperty("lspatch.hook.service", "true");
            System.setProperty("lspatch.module.service", "true");
            
            if (shizukuService != null && shizukuService.isAvailable()) {
                System.setProperty("lspatch.shizuku.available", "true");
            }
            
            // Propiedades de compatibilidad
            System.setProperty("lspatch.xposed.compatible", "true");
            System.setProperty("lspatch.lsposed.alternative", "true");
            
            Log.d(TAG, "System properties configured");
            
        } catch (Exception e) {
            Log.w(TAG, "Could not set all system properties: " + e.getMessage());
        }
    }
    
    // Getters para los servicios
    
    public ILSPApplicationService getApplicationService() {
        return applicationService;
    }
    
    public LSPatchShizukuService getShizukuService() {
        return shizukuService;
    }
    
    public LSPatchLogService getLogService() {
        return logService;
    }
    
    public LSPatchHookService getHookService() {
        return hookService;
    }
    
    public LSPatchModuleService getModuleService() {
        return moduleService;
    }
    
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Verifica si Shizuku está disponible
     */
    public boolean isShizukuAvailable() {
        return shizukuService != null && shizukuService.isAvailable();
    }
    
    /**
     * Ejecuta una operación que requiere permisos elevados
     */
    public boolean executePrivilegedOperation(String operation, Object... params) {
        if (shizukuService != null && shizukuService.isAvailable()) {
            return shizukuService.executeOperation(operation, params);
        }
        
        // Fallback para operaciones sin Shizuku
        return executeNonPrivilegedOperation(operation, params);
    }
    
    /**
     * Ejecuta operaciones que no requieren permisos especiales
     */
    private boolean executeNonPrivilegedOperation(String operation, Object... params) {
        try {
            switch (operation) {
                case "log":
                    if (params.length >= 2) {
                        logService.log((String) params[0], (String) params[1]);
                    }
                    return true;
                    
                case "hook":
                    if (hookService != null) {
                        return hookService.installHook((String) params[0], params);
                    }
                    return false;
                    
                case "module":
                    if (moduleService != null) {
                        return moduleService.loadModule((String) params[0]);
                    }
                    return false;
                    
                default:
                    Log.w(TAG, "Unknown non-privileged operation: " + operation);
                    return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error executing non-privileged operation: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtiene información de estado del sistema
     */
    public SystemStatus getSystemStatus() {
        SystemStatus status = new SystemStatus();
        status.lspatchInitialized = isInitialized;
        status.shizukuAvailable = isShizukuAvailable();
        status.logServiceActive = logService != null;
        status.hookServiceActive = hookService != null;
        status.moduleServiceActive = moduleService != null;
        
        if (moduleService != null) {
            status.loadedModules = moduleService.getLoadedModulesCount();
        }
        
        return status;
    }
    
    /**
     * Clase para información de estado del sistema
     */
    public static class SystemStatus {
        public boolean lspatchInitialized;
        public boolean shizukuAvailable;
        public boolean logServiceActive;
        public boolean hookServiceActive;
        public boolean moduleServiceActive;
        public int loadedModules;
        
        @Override
        public String toString() {
            return "SystemStatus{" +
                "lspatchInitialized=" + lspatchInitialized +
                ", shizukuAvailable=" + shizukuAvailable +
                ", logServiceActive=" + logServiceActive +
                ", hookServiceActive=" + hookServiceActive +
                ", moduleServiceActive=" + moduleServiceActive +
                ", loadedModules=" + loadedModules +
                '}';
        }
    }
}
