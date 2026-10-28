# Instalación en dispositivo Android

## Requisitos previos (en el dispositivo)

1. **Habilitar Opciones de desarrollador**
   - Ajustes → Acerca del dispositivo → toque 7 veces "Número de compilación"
2. **Activar Depuración USB**
   - Ajustes → Sistema → Opciones de desarrollador → Depuración USB: ON
3. **Autorizar la computadora**
   - Al conectar por USB, aceptar el diálogo "Permitir depuración USB"
   - Marcar "Siempre permitir desde esta computadora"

## Opción A — Desde Windows (recomendado, más estable)

Windows tiene acceso directo al hardware USB, por lo que es la forma más confiable.

```powershell
# 1. Compilar el APK
.\gradlew assembleDebug

# 2. Instalar en el dispositivo
.\scripts\install-windows.ps1

# 3. Iniciar la app
.\scripts\install-windows.ps1  # o usar adb directamente
$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe shell am start -n com.pawa.scanner/.MainActivity
```

## Opción B — Desde WSL2

WSL2 no tiene acceso directo al USB por defecto. Este script usa el `adb.exe` de Windows (que sí ve el dispositivo).

```bash
# 1. Compilar el APK
./gradlew assembleDebug

# 2. Instalar en el dispositivo
./scripts/install-wsl.sh

# 3. Iniciar la app
/mnt/c/Users/Jorge Luis/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n com.pawa.scanner/.MainActivity
```

## Opción C — WSL2 con USB nativo (usbipd-win)

Para usar `adb` de Linux directamente sobre el USB sin pasar por Windows:

1. En Windows (PowerShell como administrador):
   ```powershell
   winget install usbipd
   usbipd wsl list
   usbipd wsl attach --busid <BUS_ID_DEL_DISPOSITIVO>
   ```
2. En WSL2:
   ```bash
   lsusb  # debería aparecer el dispositivo Android
   ./scripts/install-wsl.sh  # o usar el adb de Linux directamente
   ```

## Scripts

| Script | Entorno | Uso |
|--------|---------|-----|
| `scripts/install-windows.ps1` | Windows PowerShell | Instala APK y da instrucciones si no hay dispositivo |
| `scripts/install-wsl.sh` | WSL2 Bash | Usa `adb.exe` de Windows para instalar el APK |

## Solución de problemas

- **`adb devices` vacío**: el dispositivo no está en depuración USB o no autorizó la PC. Reconecta el cable y acepta el diálogo en el dispositivo.
- **`INSTALL_FAILED_ALREADY_EXISTS`**: usa la bandera `-r` (reinstalar) — los scripts ya la incluyen.
- **`INSTALL_FAILED_NO_MATCHING_ABIS`**: el APK de debug suele incluir todas las arquitecturas; si falla, verifica el dispositivo.
