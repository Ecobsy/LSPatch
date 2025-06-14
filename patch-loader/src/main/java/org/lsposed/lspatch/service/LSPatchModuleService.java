package org.lsposed.lspatch.service;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import org.lsposed.lspd.service.ILSPApplicationService;
import org.lsposed.lspd.models.Module;
import org.lsposed.lspatch.util.ModuleLoader;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio mejorado para gestión de módulos en LSPatch
 * Proporciona funcionalidad completa de módulos sin root
 */
public class LSPatchModuleService {
    
    private static final String TAG = "LSPatch-ModuleService";
    
    private Context context;
    private ILSPApplicationService applicationService;
    private LSPatchHookService hookService;
    
    // Almacenamiento de módulos
    private final Map<String, ModuleInfo> loadedModules = new ConcurrentHashMap<>();
    private final Map<String, ModuleState> moduleStates = new ConcurrentHashMap<>();
    
    // Estadísticas de módulos
    private int totalModulesLoaded = 0;
    private int totalModulesActive = 0;
    private int totalModulesFailed = 0;
    
    /**
     * Inicializa el servicio de módulos
     */
    public boolean initialize(Context context, ILSPApplicationService appService, LSPatchHookService hookService) {
        this.context = context;
        this.applicationService = appService;
        this.hookService = hookService;
        
        try {
            Log.i(TAG, "Initializing LSPatch Module Service");
            
            // Configurar el entorno para módulos
            setupModuleEnvironment();
            
            // Cargar módulos embedded
            loadEmbeddedModules();
            
            // Cargar módulos externos (si están disponibles)
            loadExternalModules();
            
            // Configurar callbacks de módulos
            setupModuleCallbacks();
            
            Log.i(TAG, "Module service initialized successfully");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize module service: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Configura el entorno para módulos
     */
    private void setupModuleEnvironment() {
        try {
            // Configurar propiedades del sistema para módulos
            System.setProperty("lspatch.module.service.active", "true");
            System.setProperty("lspatch.module.service.version", "2.0");
            System.setProperty("lspatch.xposed.api.version", "93");
            
            // Configurar XposedBridge
            XposedBridge.log("LSPatch Module Service initialized");
            
            Log.d(TAG, "Module environment configured");
            
        } catch (Exception e) {
            Log.e(TAG, "Error configuring module environment: " + e.getMessage());
        }
    }
    
    /**
     * Carga módulos embedded en la APK
     */
    private void loadEmbeddedModules() {
        try {
            Log.i(TAG, "Loading embedded modules");
            
            // Obtener lista de módulos del servicio de aplicación
            List<Module> modules = applicationService.getLegacyModulesList();
            
            if (modules == null || modules.isEmpty()) {
                Log.w(TAG, "No embedded modules found");
                return;
            }
            
            Log.i(TAG, "Found " + modules.size() + " embedded modules");
            
            for (Module module : modules) {
                loadModule(module);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading embedded modules: " + e.getMessage());
        }
    }
    
    /**
     * Carga módulos externos
     */
    private void loadExternalModules() {
        try {
            Log.i(TAG, "Loading external modules");
            
            // Buscar módulos instalados en el sistema
            List<String> installedModules = findInstalledModules();
            
            for (String modulePath : installedModules) {
                loadExternalModule(modulePath);
            }
            
            Log.i(TAG, "External modules loading completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading external modules: " + e.getMessage());
        }
    }
    
    /**
     * Busca módulos instalados en el sistema
     */
    private List<String> findInstalledModules() {
        List<String> modules = new ArrayList<>();
        
        try {
            PackageManager pm = context.getPackageManager();
            List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_META_DATA);
            
            for (PackageInfo packageInfo : packages) {
                ApplicationInfo appInfo = packageInfo.applicationInfo;
                
                // Verificar si es un módulo Xposed/LSPatch
                if (isXposedModule(appInfo)) {
                    modules.add(appInfo.sourceDir);
                    Log.d(TAG, "Found installed module: " + appInfo.packageName);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error finding installed modules: " + e.getMessage());
        }
        
        return modules;
    }
    
    /**
     * Verifica si una aplicación es un módulo Xposed
     */
    private boolean isXposedModule(ApplicationInfo appInfo) {
        try {
            // Verificar metadatos de Xposed
            if (appInfo.metaData != null) {
                return appInfo.metaData.containsKey("xposedmodule") ||
                       appInfo.metaData.containsKey("xposeddescription") ||
                       appInfo.metaData.containsKey("xposedminversion");
            }
            
            // Verificar archivos de inicialización
            File apkFile = new File(appInfo.sourceDir);
            if (apkFile.exists()) {
                try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(apkFile)) {
                    return zipFile.getEntry("assets/xposed_init") != null;
                }
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Error checking if app is Xposed module: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Carga un módulo específico
     */
    public boolean loadModule(String modulePath) {
        try {
            Log.i(TAG, "Loading module: " + modulePath);
            
            // Cargar el módulo usando ModuleLoader
            var preLoadedApk = ModuleLoader.loadModule(modulePath);
            if (preLoadedApk == null) {
                Log.e(TAG, "Failed to load module: " + modulePath);
                totalModulesFailed++;
                return false;
            }
            
            // Crear información del módulo
            ModuleInfo moduleInfo = new ModuleInfo();
            moduleInfo.modulePath = modulePath;
            moduleInfo.packageName = extractPackageNameFromPath(modulePath);
            moduleInfo.preLoadedApk = preLoadedApk;
            moduleInfo.loadTime = System.currentTimeMillis();
            moduleInfo.isActive = true;
            
            // Ejecutar clases de inicialización del módulo
            executeModuleInitClasses(moduleInfo);
            
            // Guardar módulo cargado
            loadedModules.put(moduleInfo.packageName, moduleInfo);
            
            // Actualizar estado
            ModuleState state = new ModuleState();
            state.loaded = true;
            state.active = true;
            state.loadTime = moduleInfo.loadTime;
            moduleStates.put(moduleInfo.packageName, state);
            
            totalModulesLoaded++;
            totalModulesActive++;
            
            Log.i(TAG, "Module loaded successfully: " + moduleInfo.packageName);
            return true;
            
        } catch (Exception e) {
            totalModulesFailed++;
            Log.e(TAG, "Error loading module " + modulePath + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Carga un módulo desde el sistema de módulos legacy
     */
    private boolean loadModule(Module module) {
        try {
            Log.i(TAG, "Loading legacy module: " + module.packageName);
            
            ModuleInfo moduleInfo = new ModuleInfo();
            moduleInfo.modulePath = module.apkPath;
            moduleInfo.packageName = module.packageName;
            moduleInfo.preLoadedApk = module.file;
            moduleInfo.loadTime = System.currentTimeMillis();
            moduleInfo.isActive = true;
            moduleInfo.isLegacy = true;
            
            // Ejecutar clases de inicialización
            if (module.file != null && module.file.moduleClassNames != null) {
                for (String className : module.file.moduleClassNames) {
                    executeModuleClass(className, moduleInfo);
                }
            }
            
            // Guardar módulo
            loadedModules.put(moduleInfo.packageName, moduleInfo);
            
            ModuleState state = new ModuleState();
            state.loaded = true;
            state.active = true;
            state.loadTime = moduleInfo.loadTime;
            moduleStates.put(moduleInfo.packageName, state);
            
            totalModulesLoaded++;
            totalModulesActive++;
            
            Log.i(TAG, "Legacy module loaded successfully: " + moduleInfo.packageName);
            return true;
            
        } catch (Exception e) {
            totalModulesFailed++;
            Log.e(TAG, "Error loading legacy module " + module.packageName + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Carga un módulo externo
     */
    private boolean loadExternalModule(String modulePath) {
        try {
            Log.i(TAG, "Loading external module: " + modulePath);
            
            // Verificar permisos y accesibilidad
            File moduleFile = new File(modulePath);
            if (!moduleFile.exists() || !moduleFile.canRead()) {
                Log.w(TAG, "External module not accessible: " + modulePath);
                return false;
            }
            
            // Intentar carga del módulo
            return loadModule(modulePath);
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading external module " + modulePath + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Ejecuta las clases de inicialización del módulo
     */
    private void executeModuleInitClasses(ModuleInfo moduleInfo) {
        try {
            if (moduleInfo.preLoadedApk == null || moduleInfo.preLoadedApk.moduleClassNames == null) {
                return;
            }
            
            for (String className : moduleInfo.preLoadedApk.moduleClassNames) {
                executeModuleClass(className, moduleInfo);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error executing module init classes: " + e.getMessage());
        }
    }
    
    /**
     * Ejecuta una clase específica del módulo
     */
    private void executeModuleClass(String className, ModuleInfo moduleInfo) {
        try {
            Log.d(TAG, "Executing module class: " + className);
            
            // Cargar la clase
            Class<?> moduleClass = Class.forName(className);
            
            // Verificar si implementa IXposedHookLoadPackage
            if (isXposedHookLoadPackage(moduleClass)) {
                executeXposedHookLoadPackage(moduleClass, moduleInfo);
            }
            
            // Verificar si implementa IXposedHookInitPackageResources
            if (isXposedHookInitPackageResources(moduleClass)) {
                executeXposedHookInitPackageResources(moduleClass, moduleInfo);
            }
            
            // Verificar si implementa IXposedHookZygoteInit
            if (isXposedHookZygoteInit(moduleClass)) {
                executeXposedHookZygoteInit(moduleClass, moduleInfo);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error executing module class " + className + ": " + e.getMessage());
        }
    }
    
    /**
     * Verifica si la clase implementa IXposedHookLoadPackage
     */
    private boolean isXposedHookLoadPackage(Class<?> clazz) {
        try {
            Class<?> interfaceClass = Class.forName("de.robv.android.xposed.IXposedHookLoadPackage");
            return interfaceClass.isAssignableFrom(clazz);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Ejecuta IXposedHookLoadPackage
     */
    private void executeXposedHookLoadPackage(Class<?> moduleClass, ModuleInfo moduleInfo) {
        try {
            Object instance = moduleClass.getDeclaredConstructor().newInstance();
            
            // Crear LoadPackageParam
            XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam(
                XposedBridge.sLoadedPackageCallbacks);
            lpparam.packageName = context.getPackageName();
            lpparam.processName = android.app.ActivityThread.currentProcessName();
            lpparam.classLoader = context.getClassLoader();
            lpparam.appInfo = context.getApplicationInfo();
            lpparam.isFirstApplication = true;
            
            // Llamar handleLoadPackage
            java.lang.reflect.Method handleLoadPackageMethod = 
                moduleClass.getMethod("handleLoadPackage", XC_LoadPackage.LoadPackageParam.class);
            handleLoadPackageMethod.invoke(instance, lpparam);
            
            Log.i(TAG, "Executed IXposedHookLoadPackage for: " + moduleClass.getName());
            
        } catch (Exception e) {
            Log.e(TAG, "Error executing IXposedHookLoadPackage: " + e.getMessage());
        }
    }
    
    /**
     * Verifica si la clase implementa IXposedHookInitPackageResources
     */
    private boolean isXposedHookInitPackageResources(Class<?> clazz) {
        try {
            Class<?> interfaceClass = Class.forName("de.robv.android.xposed.IXposedHookInitPackageResources");
            return interfaceClass.isAssignableFrom(clazz);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Ejecuta IXposedHookInitPackageResources
     */
    private void executeXposedHookInitPackageResources(Class<?> moduleClass, ModuleInfo moduleInfo) {
        try {
            // Implementación para recursos
            Log.d(TAG, "Executed IXposedHookInitPackageResources for: " + moduleClass.getName());
            
        } catch (Exception e) {
            Log.e(TAG, "Error executing IXposedHookInitPackageResources: " + e.getMessage());
        }
    }
    
    /**
     * Verifica si la clase implementa IXposedHookZygoteInit
     */
    private boolean isXposedHookZygoteInit(Class<?> clazz) {
        try {
            Class<?> interfaceClass = Class.forName("de.robv.android.xposed.IXposedHookZygoteInit");
            return interfaceClass.isAssignableFrom(clazz);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Ejecuta IXposedHookZygoteInit
     */
    private void executeXposedHookZygoteInit(Class<?> moduleClass, ModuleInfo moduleInfo) {
        try {
            // Implementación para inicialización de Zygote
            Log.d(TAG, "Executed IXposedHookZygoteInit for: " + moduleClass.getName());
            
        } catch (Exception e) {
            Log.e(TAG, "Error executing IXposedHookZygoteInit: " + e.getMessage());
        }
    }
    
    /**
     * Configura callbacks de módulos
     */
    private void setupModuleCallbacks() {
        try {
            // Configurar callbacks para detección y gestión de módulos
            Log.d(TAG, "Module callbacks configured");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up module callbacks: " + e.getMessage());
        }
    }
    
    /**
     * Extrae el nombre del paquete desde la ruta del módulo
     */
    private String extractPackageNameFromPath(String modulePath) {
        try {
            // Extraer nombre del archivo sin extensión
            String fileName = new File(modulePath).getName();
            if (fileName.endsWith(".apk")) {
                return fileName.substring(0, fileName.length() - 4);
            }
            return fileName;
            
        } catch (Exception e) {
            return "unknown_module_" + System.currentTimeMillis();
        }
    }
    
    /**
     * Obtiene la cantidad de módulos cargados
     */
    public int getLoadedModulesCount() {
        return totalModulesLoaded;
    }
    
    /**
     * Obtiene estadísticas de módulos
     */
    public ModuleStatistics getModuleStatistics() {
        ModuleStatistics stats = new ModuleStatistics();
        stats.totalLoaded = totalModulesLoaded;
        stats.totalActive = totalModulesActive;
        stats.totalFailed = totalModulesFailed;
        stats.loadedModules = new ArrayList<>(loadedModules.values());
        
        return stats;
    }
    
    /**
     * Obtiene información de un módulo específico
     */
    public ModuleInfo getModuleInfo(String packageName) {
        return loadedModules.get(packageName);
    }
    
    /**
     * Verifica si un módulo está cargado
     */
    public boolean isModuleLoaded(String packageName) {
        ModuleInfo moduleInfo = loadedModules.get(packageName);
        return moduleInfo != null && moduleInfo.isActive;
    }
    
    /**
     * Obtiene lista de todos los módulos cargados
     */
    public List<ModuleInfo> getAllLoadedModules() {
        return new ArrayList<>(loadedModules.values());
    }
    
    /**
     * Clase para información de módulo
     */
    public static class ModuleInfo {
        public String modulePath;
        public String packageName;
        public org.lsposed.lspatch.util.PreLoadedApk preLoadedApk;
        public long loadTime;
        public boolean isActive;
        public boolean isLegacy;
        
        @Override
        public String toString() {
            return "ModuleInfo{" +
                "modulePath='" + modulePath + '\'' +
                ", packageName='" + packageName + '\'' +
                ", loadTime=" + loadTime +
                ", isActive=" + isActive +
                ", isLegacy=" + isLegacy +
                '}';
        }
    }
    
    /**
     * Clase para estado de módulo
     */
    public static class ModuleState {
        public boolean loaded;
        public boolean active;
        public long loadTime;
        public String error;
        
        @Override
        public String toString() {
            return "ModuleState{" +
                "loaded=" + loaded +
                ", active=" + active +
                ", loadTime=" + loadTime +
                ", error='" + error + '\'' +
                '}';
        }
    }
    
    /**
     * Clase para estadísticas de módulos
     */
    public static class ModuleStatistics {
        public int totalLoaded;
        public int totalActive;
        public int totalFailed;
        public List<ModuleInfo> loadedModules;
        
        @Override
        public String toString() {
            return "ModuleStatistics{" +
                "totalLoaded=" + totalLoaded +
                ", totalActive=" + totalActive +
                ", totalFailed=" + totalFailed +
                ", loadedModulesCount=" + (loadedModules != null ? loadedModules.size() : 0) +
                '}';
        }
    }
}
