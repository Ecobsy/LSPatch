#!/bin/bash

set -e

echo "📦 Compilando dependencias integradas de LSPatch..."

# Compilar libxposed (anteriormente submódulos)
echo "🔧 Compilando libxposed API..."
./gradlew :libxposed:api:build

echo "🔍 Compilando libxposed checks..."
./gradlew :libxposed:api:checks:build

echo "⚙️ Compilando libxposed service..."
./gradlew :libxposed:service:build

echo "🔌 Compilando libxposed service interface..."
./gradlew :libxposed:service:interface:build

# Compilar otras dependencias core
echo "📚 Compilando dependencias Apache..."
./gradlew :apache:build

echo "📦 Compilando apkzlib..."
./gradlew :apkzlib:build

echo "🗂️ Compilando axml..."
./gradlew :axml:build

echo "✅ ¡Todas las dependencias compiladas exitosamente!"