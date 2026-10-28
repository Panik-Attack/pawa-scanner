package com.pawa.scanner.api

import com.pawa.scanner.scan.ScanResult
import kotlinx.coroutines.Deferred

/**
 * Contrato de la API para enviar códigos escaneados al servidor.
 *
 * Esta interfaz se implementará definitivamente cuando se defina el
 * contrato de la API (endpoint, headers, autenticación, cuerpo y respuesta).
 *
 * Por ahora existe una implementación mock [MockScanApiClient] que simula
 * respuestas de éxito y error para probar el flujo de la UI.
 */
interface ScanApiClient {

    /**
     * Envía un código escaneado al servidor.
     *
     * @param result resultado del escaneo con el código y metadatos
     * @return [ScanApiResponse] indicando si el código fue creado nuevo
     *         ([isNew = true]) o ya existía ([isNew = false]), o un error.
     * @throws [ScanApiException] si hay un error de red o del servidor.
     */
    suspend fun sendScan(result: ScanResult): ScanApiResponse
}

/**
 * Respuesta de la API tras enviar un escaneo.
 *
 * @param isNew true si el código se registró como nuevo, false si ya existía.
 * @param message mensaje descriptivo del servidor.
 * @param serverTimestamp timestamp del servidor (opcional).
 */
data class ScanApiResponse(
    val isNew: Boolean,
    val message: String = "",
    val serverTimestamp: Long? = null
)

/**
 * Excepción de la API para errores de red o del servidor.
 */
class ScanApiException(message: String, val code: Int? = null) : Exception(message)

/**
 * Implementación mock de [ScanApiClient] para pruebas y desarrollo.
 *
 * Simula:
 * - Delay de red (800 ms).
 * - 70% de probabilidad de éxito (código nuevo).
 * - 20% de probabilidad de código ya existente.
 * - 10% de probabilidad de error de red.
 *
 * Esta clase se reemplazará por la implementación real (Retrofit/OkHttp)
 * cuando se defina el contrato de la API.
 */
class MockScanApiClient : ScanApiClient {

    override suspend fun sendScan(result: ScanResult): ScanApiResponse {
        // Simular delay de red
        Thread.sleep(800)

        val random = java.util.Random(System.currentTimeMillis() + result.code.hashCode())
        val roll = random.nextInt(100)

        return when {
            roll < 70 -> {
                ScanApiResponse(
                    isNew = true,
                    message = "Código registrado correctamente",
                    serverTimestamp = System.currentTimeMillis()
                )
            }
            roll < 90 -> {
                ScanApiResponse(
                    isNew = false,
                    message = "El código ya estaba registrado previamente"
                )
            }
            else -> {
                throw ScanApiException(
                    "Error de red: no se pudo conectar con el servidor",
                    code = 503
                )
            }
        }
    }
}
