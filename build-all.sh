#!/bin/bash

set -e

echo "🚀 Iniciando compilación completa de LSPatch..."

# Limpiar builds anteriores
echo "🧹 Limpiando builds anteriores..."
./gradlew clean

# Compilar todas las dependencias
echo "📦 Compilando dependencias libxposed..."
./gradlew :libxposed:api:build
./gradlew :libxposed:api:checks:build
./gradlew :libxposed:service:build
./gradlew :libxposed:service:interface:build

# Compilar módulos core
echo "🔧 Compilando módulos core..."
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
