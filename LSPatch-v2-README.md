# LSPatch v2.0 - Enhanced LSPosed Alternative (No Root)

## 🚀 Nuevas Funcionalidades

LSPatch v2.0 ha sido completamente refactorizado para ser una alternativa completa a LSPosed sin necesidad de root. Las nuevas funcionalidades incluyen:

### 🎯 Características Principales

- **✅ Funcionalidad completa sin root**: Todas las características de LSPosed disponibles
- **🔗 Integración con Shizuku**: Para operaciones que requieren permisos elevados
- **🛠️ Sistema de servicios mejorado**: Gestión completa de hooks, módulos y logs
- **📊 Sistema de logs avanzado**: Captura completa de logs de módulos con rotación automática
- **🔧 Compatibilidad total con WaEnhancer**: Detección y soporte completo
- **⚡ Optimización para aplicaciones inyectadas**: Rendimiento mejorado en entornos sin root

### 🏗️ Arquitectura Mejorada

#### 📦 LSPatchServiceManager
- **Propósito**: Gestor central de todos los servicios
- **Funcionalidades**:
  - Inicialización de servicios de logs, hooks y módulos
  - Integración con Shizuku cuando está disponible
  - Fallback a métodos sin privilegios cuando Shizuku no está disponible
  - Estado del sistema y diagnósticos

#### 🔗 LSPatchShizukuService  
- **Propósito**: Integración con Shizuku para operaciones privilegiadas
- **Funcionalidades**:
  - Instalación/desinstalación de APKs
  - Ejecución de comandos shell
  - Lectura/escritura de archivos del sistema
  - Otorgamiento de permisos

#### 🎣 LSPatchHookService
- **Propósito**: Sistema de hooks optimizado para entornos sin root
- **Funcionalidades**:
  - Hooks específicos para detección de LSPatch
  - Compatibilidad con diferentes versiones de Android
  - Optimizaciones para aplicaciones específicas (WhatsApp, Instagram, etc.)
  - Gestión avanzada de hooks con estadísticas

#### 📋 LSPatchModuleService
- **Propósito**: Gestión completa de módulos Xposed/LSPosed
- **Funcionalidades**:
  - Carga de módulos embedded y externos
  - Detección automática de módulos instalados
  - Ejecución de interfaces Xposed (IXposedHookLoadPackage, etc.)
  - Estadísticas y diagnósticos de módulos

#### 📝 LSPatchLogService (Mejorado)
- **Propósito**: Sistema de logs empresarial para módulos
- **Funcionalidades**:
  - Captura de logs por nivel (VERBOSE, DEBUG, INFO, WARN, ERROR)
  - Rotación automática de archivos de log
  - Exportación de logs para debugging
  - Estadísticas detalladas de logs
  - Persistencia en disco configurable

#### 🌉 LSPatchBridge (Mejorado)
- **Propósito**: Bridge de compatibilidad completa con LSPosed
- **Funcionalidades**:
  - Emulación completa de APIs de LSPosed
  - Sistema de capacidades para consultas de módulos
  - Gestión de errores de módulos
  - Información de estado del sistema

### 🔧 Compatibilidad Mejorada

#### 📱 WhatsApp/WaEnhancer
- Detección mejorada de LSPatch con 7 métodos diferentes
- Compatibilidad completa con APIs de WaEnhancer
- Shims para todas las interfaces esperadas

#### 🔄 Módulos Xposed/LSPosed
- Soporte completo para módulos existentes
- Carga automática de módulos instalados
- Ejecución de todas las interfaces estándar

#### 📱 Versiones de Android
- Soporte desde Android 8.0+
- Optimizaciones específicas para Android 10, 11, 12+
- Compatibilidad con cambios de API recientes

### 🛠️ Instalación y Uso

#### Requisitos
- Android 8.0+ (API 26+)
- Para funcionalidad completa: Shizuku (opcional pero recomendado)

#### Compilación
```bash
# Compilar dependencias mejoradas
./build-dependencies.sh

# Compilar LSPatch completo
./build-all.sh
```

#### Uso Básico
1. **Sin Shizuku**: Funcionalidad básica de hooks y módulos
2. **Con Shizuku**: Funcionalidad completa incluyendo operaciones del sistema

### 📊 Diagnósticos y Debugging

#### Sistema de Logs
```java
// Los módulos pueden usar logging avanzado
LSPatchServiceManager.getInstance().getLogService()
    .logInfo("MiModulo", "Mensaje informativo");
```

#### Información del Sistema
```java
// Obtener estado completo del sistema
var status = LSPatchServiceManager.getInstance().getSystemStatus();
Log.i("Status", status.toString());
```

#### Bridge de Compatibilidad
```java
// Verificar capacidades disponibles
boolean shizukuAvailable = LSPatchBridge.hasCapability("advanced.shizuku");
boolean waenhancerSupport = LSPatchBridge.hasCapability("compat.waenhancer");
```

### 🚀 Mejoras de Rendimiento

- **Carga optimizada de DEX**: Mejor gestión de memoria y velocidad
- **Hooks específicos por aplicación**: Reducción de overhead
- **Cachés inteligentes**: Minimización de reflexión repetitiva
- **Logging asíncrono**: No bloquea el hilo principal

### 🔐 Seguridad

- **Aislamiento de módulos**: Prevención de interferencias
- **Validación de firmas**: Bypass inteligente cuando es necesario
- **Permisos mínimos**: Solo solicita lo estrictamente necesario
- **Detección de tampering**: Protección contra modificaciones maliciosas

### 📚 API para Desarrolladores

#### Para Desarrolladores de Módulos
```java
// Verificar si LSPatch está activo
boolean lspatchActive = "true".equals(System.getProperty("lspatch.initialized"));

// Usar bridge para operaciones avanzadas
LSPatchBridge.handleServiceOperation("log", "INFO", "Mi mensaje");
```

#### Para Aplicaciones como WaEnhancer
```java
// APIs de compatibilidad completa
boolean bridgeReady = LSPatchBridgeShim.isInitialized();
Context context = LSPatchBridgeShim.getContext();
boolean hasCapability = LSPatchBridgeShim.hasCapability("advanced.shizuku");
```

### 🐛 Debugging y Solución de Problemas

#### Logs Detallados
- Los logs se guardan en `/data/data/[app]/cache/lspatch_logs/`
- Exportación automática para debugging
- Rotación para evitar uso excesivo de espacio

#### Información de Estado
```bash
# Ver estado en logcat
adb logcat | grep "LSPatch"
```

#### Verificación de Funcionalidad
```java
// Obtener información completa del bridge
BridgeInfo info = LSPatchBridge.getBridgeInfo();
Log.i("LSPatch", info.toString());
```

### 🔄 Migración desde LSPosed

LSPatch v2.0 es completamente compatible con módulos existentes de LSPosed:

1. **Módulos existentes**: Funcionan sin modificaciones
2. **APIs**: Emulación completa de LSPosed APIs
3. **Configuración**: Migración automática de configuraciones

### 🎯 Casos de Uso Principales

1. **Dispositivos sin root**: Funcionalidad completa de LSPosed
2. **Aplicaciones empresariales**: Sin necesidad de root del dispositivo
3. **Desarrollo y testing**: Entorno controlado sin modificaciones del sistema
4. **Módulos específicos**: WhatsApp, Instagram, Facebook, etc.

### 📈 Métricas y Estadísticas

El sistema incluye métricas detalladas:
- Módulos cargados y activos
- Hooks instalados y estados
- Logs por nivel y módulo
- Rendimiento del sistema
- Uso de recursos

---

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Por favor:

1. Fork el proyecto
2. Crea una rama para tu feature
3. Commit tus cambios
4. Push a la rama
5. Crea un Pull Request

## 📄 Licencia

Este proyecto mantiene la misma licencia que LSPosed original.

## 🙏 Agradecimientos

- Equipo original de LSPosed
- Comunidad de desarrolladores de Xposed
- Desarrolladores de Shizuku
- Contribuidores de la comunidad

---

**LSPatch v2.0 - La evolución de LSPosed para el mundo sin root** 🚀
