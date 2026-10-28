#!/usr/bin/env bash
# =============================================================================
# install-wsl.sh
# Instala el APK de debug de pawa-scanner en un dispositivo Android conectado
# por USB desde WSL2, usando el adb.exe de Windows (que tiene acceso al USB).
#
# Requisitos previos en el dispositivo Android:
#   1. Opciones de desarrollador habilitadas
#   2. Depuración USB activada
#   3. Autorizar esta computadora cuando aparezca el diálogo en el dispositivo
#
# Uso:
#   ./scripts/install-wsl.sh
# =============================================================================
set -euo pipefail

ADB_EXE="/mnt/c/Users/Jorge Luis/AppData/Local/Android/Sdk/platform-tools/adb.exe"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

echo "=== Pawa Scanner - Instalación en dispositivo (WSL2) ==="

if [[ ! -x "$ADB_EXE" ]]; then
    echo "ERROR: No se encontró adb.exe en $ADB_EXE" >&2
    echo "Instala Android SDK Platform-Tools en Windows o ajusta la ruta ADB_EXE" >&2
    exit 1
fi

if [[ ! -f "$APK_PATH" ]]; then
    echo "ERROR: No se encontró el APK en $APK_PATH" >&2
    echo "Ejecuta primero: ./gradlew assembleDebug" >&2
    exit 1
fi

echo "Verificando dispositivos conectados..."
DEVICES=$("$ADB_EXE" devices | grep -E 'device$' | grep -v 'List of devices' || true)

if [[ -z "$DEVICES" ]]; then
    echo "WARNING: No se detectó ningún dispositivo en modo depuración USB." >&2
    echo "" >&2
    echo "Pasos para solucionarlo:" >&2
    echo "  1. Conecta el dispositivo por USB al puerto de la PC" >&2
    echo "  2. En el dispositivo: Ajustes > Sistema > Opciones de desarrollador" >&2
    echo "  3. Activa: Depuración USB" >&2
    echo "  4. Cuando aparezca el diálogo en el dispositivo, marca" >&2
    echo "     'Siempre permitir desde esta computadora' y acepta" >&2
    echo "  5. Vuelve a ejecutar este script" >&2
    exit 1
fi

echo "Dispositivos detectados:"
echo "$DEVICES" | while read -r line; do
    echo "  $line"
done

echo ""
echo "Instalando APK..."
"$ADB_EXE" install -r "$APK_PATH"

if [[ $? -eq 0 ]]; then
    echo ""
    echo "¡Instalación completada con éxito!"
    echo "Para iniciar la app:"
    echo "  $ADB_EXE shell am start -n com.pawa.scanner/.MainActivity"
else
    echo "ERROR: Falló la instalación del APK." >&2
    exit 1
fi
