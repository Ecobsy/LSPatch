#!/bin/bash

set -e

echo "🚀 Iniciando compilación completa de LSPatch v2.0 Enhanced..."

# Mostrar información del sistema
echo "📋 Información del sistema:"
echo "Java Version: $(java -version 2>&1 | head -n 1)"
echo "Gradle Version: $(./gradlew --version | grep Gradle)"
echo "Android SDK: $ANDROID_HOME"
echo ""

# Limpiar builds anteriores
echo "🧹 Limpiando builds anteriores..."
./gradlew clean

# Compilar dependencias mejoradas usando script específico
echo "📦 Compilando dependencias integradas y mejoradas..."
chmod +x build-dependencies.sh
./build-dependencies.sh

# Compilar módulos core con orden específico
echo "🔧 Compilando módulos core en orden de dependencias..."
./gradlew :share:build
./gradlew :apache:build
./gradlew :apkzlib:build
./gradlew :axml:build
./gradlew :core:build
./gradlew :hiddenapi:bridge:build
./gradlew :hiddenapi:stubs:build

# Compilar servicios
echo "⚙️ Compilando servicios..."
./gradlew :services:daemon-service:build
./gradlew :services:manager-service:build

# Compilar módulos principales
echo "🏗️ Compilando módulos principales..."
./gradlew :jar:build
./gradlew :manager:build
./gradlew :meta-loader:build
./gradlew :patch:build
./gradlew :patch-loader:build

# Compilar módulos compartidos
echo "📚 Compilando módulos compartidos..."
./gradlew :share:android:build
./gradlew :share:java:build

# Compilación final
echo "🎯 Compilación final..."
./gradlew buildAll

echo "✅ ¡Compilación completa exitosa!"
echo "📁 Los archivos compilados están en:"
echo "   - JAR: jar/build/libs/"
echo "   - Manager APK: manager/build/outputs/apk/"
