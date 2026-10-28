package com.pawa.scanner.api

import android.util.Log
import com.pawa.scanner.BuildConfig
import com.pawa.scanner.scan.ScanResult
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Implementación real de [ScanApiClient] usando HttpURLConnection nativo de Android.
 *
 * Se comunica con el servidor de trackings Pre-Arrival de Pawa Shop.
 *
 * Endpoint principal:
 *   POST https://pawashop.com/api/integrations/prearrival/tracking
 *
 * Configuración leída de local.properties (BuildConfig):
 * - BuildConfig.PAWA_API_KEY: valor del header x-api-key (PREARRIVAL_PDA_API_KEY del servidor)
 * - BuildConfig.PAWA_DEVICE_ID: identificador opcional del dispositivo (header x-device-id)
 */
class RealScanApiClient private constructor() : ScanApiClient {

    companion object {
        @Volatile private var instance: RealScanApiClient? = null

        private const val TAG = "RealScanApiClient"
        private const val BASE_URL = "https://pawashop.com/api/integrations/prearrival/tracking"
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 15000

        fun get(): RealScanApiClient {
            return instance ?: synchronized(this) {
                instance ?: RealScanApiClient().also { instance = it }
            }
        }
    }

    override suspend fun sendScan(result: ScanResult): ScanApiResponse = suspendCoroutine { continuation ->
        // Ejecutar en hilo de fondo para no bloquear la UI
        java.lang.Thread {
            try {
                val url = URL(BASE_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "POST"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("x-api-key", BuildConfig.PAWA_API_KEY)
                    BuildConfig.PAWA_DEVICE_ID?.let { setRequestProperty("x-device-id", it) }
                }

                val body = buildJsonBody(result.code)
                conn.outputStream.use { os ->
                    os.write(body.toByteArray(Charsets.UTF_8))
                }

                val responseCode = conn.responseCode
                val responseBody = conn.inputStream?.bufferedReader()?.use { it.readText() }
                    ?: conn.errorStream?.bufferedReader()?.use { it.readText() }
                    ?: ""

                when (responseCode) {
                    201 -> {
                        // Código nuevo creado
                        continuation.resume(ScanApiResponse(
                            isNew = true,
                            message = "Código registrado correctamente",
                            serverTimestamp = System.currentTimeMillis()
                        ))
                    }
                    200 -> {
                        // Ya existía (created: false)
                        continuation.resume(ScanApiResponse(
                            isNew = false,
                            message = "El código ya estaba registrado previamente"
                        ))
                    }
                    400 -> {
                        continuation.resume(ScanApiResponse(
                            isNew = false,
                            message = "Solicitud inválida: $responseBody"
                        ))
                    }
                    401 -> {
                        continuation.resume(ScanApiResponse(
                            isNew = false,
                            message = "No autorizado: verifica la API key"
                        ))
                    }
                    403 -> {
                        continuation.resume(ScanApiResponse(
                            isNew = false,
                            message = "IP no permitida: verifica PREARRIVAL_PDA_ALLOWED_IPS"
                        ))
                    }
                    409 -> {
                        continuation.resume(ScanApiResponse(
                            isNew = false,
                            message = "Conflicto: tracking no se puede eliminar o ya existe"
                        ))
                    }
                    500 -> {
                        continuation.resume(ScanApiResponse(
                            isNew = false,
                            message = "Error interno del servidor"
                        ))
                    }
                    else -> {
                        continuation.resume(ScanApiResponse(
                            isNew = false,
                            message = "Error inesperado: $responseCode $responseBody"
                        ))
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error registrando scan", e)
                continuation.resume(ScanApiResponse(
                    isNew = false,
                    message = "Error de red: ${e.message}"
                ))
            }
        }.start()
    }

    private fun buildJsonBody(trackingNumber: String): String {
        // Normalizar: quitar espacios y pasar a mayúsculas (el servidor también lo hace)
        val normalized = trackingNumber.replace(" ", "").uppercase()
        return """{"trackingNumber":"$normalized","carrier":"other","notes":""}"""
    }
}
