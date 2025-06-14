#!/bin/bash

set -e

echo "📦 Compilando dependencias integradas y mejoradas de LSPatch v2.0..."

# Compilar libxposed (anteriormente submódulos)
echo "🔧 Compilando libxposed API..."
./gradlew :libxposed:api:build

echo "🔍 Compilando libxposed checks..."
./gradlew :libxposed:api:checks:build

echo "⚙️ Compilando libxposed service..."
./gradlew :libxposed:service:build

echo "🔌 Compilando libxposed service interface..."
./gradlew :libxposed:service:interface:build

# Compilar nuevos servicios mejorados
echo "🚀 Compilando servicios LSPatch mejorados..."
./gradlew :patch-loader:build

echo "🛠️ Compilando bridge de compatibilidad..."
./gradlew :core:build

echo "📊 Compilando sistema de logs mejorado..."
./gradlew :manager:build

# Verificar que Shizuku esté disponible como dependencia opcional
echo "🔐 Verificando dependencias de Shizuku..."
if ./gradlew dependencies | grep -q "shizuku"; then
    echo "✅ Shizuku encontrado como dependencia"
else
    echo "⚠️ Shizuku no encontrado - funcionalidad limitada sin root"
fi

# Compilar otras dependencias core
echo "📚 Compilando dependencias Apache..."
./gradlew :apache:build

echo "📦 Compilando apkzlib..."
./gradlew :apkzlib:build

echo "🗂️ Compilando axml..."
./gradlew :axml:build

echo "✅ ¡Todas las dependencias compiladas exitosamente!"