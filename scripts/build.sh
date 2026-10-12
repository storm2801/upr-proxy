#!/usr/bin/env bash
# build.sh — Compila UPR Proxy desde CachyOS/Arch.
#
# Sin proxy: ejecuta 'bash scripts/build.sh'.
# Con proxy: exporta las variables del shell y ejecuta igual.
# Las credenciales NO van aquí, van en tu ~/.bashrc.

set -e
cd "$(dirname "$0")/.."

VARIANT="${1:-release}"

echo "══════════════════════════════════════════"
echo "  UPR Proxy — compilando: $VARIANT"
echo "══════════════════════════════════════════"
echo ""

pkill -9 -f "GradleDaemon" 2>/dev/null || true
rm -rf ~/.gradle/daemon ~/.gradle/workers 2>/dev/null || true
sleep 1

echo "→ Compilando (1-3 min)…"
echo ""

if [ "$VARIANT" = "debug" ]; then
    ./gradlew assembleDebug --no-daemon --console=plain
    APK="app/build/outputs/apk/debug/app-debug.apk"
else
    ./gradlew assembleRelease --no-daemon --console=plain
    APK="app/build/outputs/apk/release/app-release.apk"
fi

echo ""
if [ -f "$APK" ]; then
    SIZE=$(du -h "$APK" | cut -f1)
    echo "══════════════════════════════════════════"
    echo "  ✅ BUILD SUCCESSFUL"
    echo "  📦 APK: $APK ($SIZE)"
    echo "══════════════════════════════════════════"
else
    echo "❌ BUILD FAILED"
    exit 1
fi
