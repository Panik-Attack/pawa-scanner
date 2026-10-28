# =============================================================================
# install-windows.ps1
# Instala el APK de debug de pawa-scanner en un dispositivo Android conectado
# por USB desde Windows.
#
# Requisitos previos en el dispositivo Android:
#   1. Opciones de desarrollador habilitadas
#   2. Depuración USB activada
#   3. Autorizar esta computadora cuando aparezca el diálogo en el dispositivo
#
# Uso:
#   .\scripts\install-windows.ps1
# =============================================================================

$ErrorActionPreference = "Stop"

$adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"

Write-Host "=== Pawa Scanner - Instalación en dispositivo (Windows) ===" -ForegroundColor Cyan

if (-not (Test-Path $adbPath)) {
    Write-Host "ERROR: No se encontró adb.exe en $adbPath" -ForegroundColor Red
    Write-Host "Instala Android SDK Platform-Tools o ajusta $adbPath" -ForegroundColor Yellow
    exit 1
}

if (-not (Test-Path $apkPath)) {
    Write-Host "ERROR: No se encontró el APK en $apkPath" -ForegroundColor Red
    Write-Host "Ejecuta primero: .\gradlew assembleDebug" -ForegroundColor Yellow
    exit 1
}

Write-Host "Verificando dispositivos conectados..." -ForegroundColor Gray
$devices = & $adbPath devices | Where-Object { $_ -match "device$" -and $_ -notmatch "List of devices" }

if ($devices.Count -eq 0) {
    Write-Host "WARNING: No se detectó ningún dispositivo en modo depuración USB." -ForegroundColor Yellow
    Write-Host "" -ForegroundColor Gray
    Write-Host "Pasos para solucionarlo:" -ForegroundColor Cyan
    Write-Host "  1. Conecta el dispositivo por USB al puerto de la PC" -ForegroundColor Gray
    Write-Host "  2. En el dispositivo: Ajustes > Sistema > Opciones de desarrollador" -ForegroundColor Gray
    Write-Host "  3. Activa: Depuración USB" -ForegroundColor Gray
    Write-Host "  4. Cuando aparezca el diálogo en el dispositivo, marca" -ForegroundColor Gray
    Write-Host "     'Siempre permitir desde esta computadora' y acepta" -ForegroundColor Gray
    Write-Host "  5. Vuelve a ejecutar este script" -ForegroundColor Gray
    exit 1
}

Write-Host "Dispositivos detectados:" -ForegroundColor Green
$devices | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }

Write-Host "" -ForegroundColor Gray
Write-Host "Instalando APK..." -ForegroundColor Cyan
& $adbPath install -r $apkPath

if ($LASTEXITCODE -eq 0) {
    Write-Host "" -ForegroundColor Gray
    Write-Host "¡Instalación completada con éxito!" -ForegroundColor Green
    Write-Host "Para iniciar la app:" -ForegroundColor Cyan
    Write-Host "  $adbPath shell am start -n com.pawa.scanner/.MainActivity" -ForegroundColor Gray
} else {
    Write-Host "ERROR: Falló la instalación del APK." -ForegroundColor Red
    exit $LASTEXITCODE
}
