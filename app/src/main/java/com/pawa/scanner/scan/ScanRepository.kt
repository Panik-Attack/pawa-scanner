package com.pawa.scanner.scan

import com.pawa.scanner.api.MockScanApiClient
import com.pawa.scanner.api.ScanApiClient
import com.pawa.scanner.api.ScanApiException
import com.pawa.scanner.api.ScanApiResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Estado de un escaneo en el repositorio.
 */
enum class ScanState {
    IDLE,           // No hay escaneo en curso
    LOADING,        // Enviando a la API
    SUCCESS_NEW,    // Código registrado como nuevo
    SUCCESS_EXISTING, // Código ya existía
    ERROR           // Error en el envío
}

/**
 * Resultado de un escaneo procesado por el repositorio.
 */
data class ScanOutcome(
    val code: String,
    val state: ScanState,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Repositorio de escaneos.
 *
 * Mantiene el historial de escaneos y el último estado.
 * Cuando se define el contrato de la API, solo hay que inyectar una
 * implementación real de [ScanApiClient] en lugar de [MockScanApiClient].
 */
class ScanRepository private constructor(
    private val apiClient: ScanApiClient
) {

    private val scans = mutableListOf<ScanResult>()

    var lastResult: ScanResult? = null
        private set

    var lastError: Throwable? = null
        private set

    var isLoading: Boolean = false
        private set

    var lastOutcome: ScanOutcome? = null
        private set

    val history: List<ScanResult>
        get() = scans.toList()

    /**
     * Último resultado procesado (para observar cambios desde la UI).
     */
    var onOutcomeChange: ((ScanOutcome) -> Unit)? = null

    companion object {
        @Volatile private var instance: ScanRepository? = null

        fun get(): ScanRepository {
            return instance ?: synchronized(this) {
                instance ?: ScanRepository(MockScanApiClient()).also { instance = it }
            }
        }

        /**
         * Permite inyectar una implementación real de la API cuando se defina el contrato.
         * Debe llamarse antes de obtener la instancia por primera vez.
         */
        fun setApiClient(apiClient: ScanApiClient) {
            synchronized(this) {
                if (instance == null) {
                    instance = ScanRepository(apiClient)
                }
            }
        }
    }

    fun addScan(result: ScanResult) {
        scans.add(result)
        lastResult = result
        lastError = null
    }

    fun setLoading(loading: Boolean) {
        isLoading = loading
    }

    fun setError(throwable: Throwable) {
        lastError = throwable
        lastResult = null
    }

    /**
     * Procesa un código escaneado: lo registra localmente, lo envía a la API
     * y notifica el resultado vía [onOutcomeChange].
     *
     * Este método es seguro para llamar desde cualquier hilo (usa corrutinas
     * con Dispatchers.Default para la red y Dispatchers.Main para callbacks).
     */
    fun processScan(code: String, scope: CoroutineScope) {
        val result = ScanResult(code = code)
        addScan(result)

        val initialOutcome = ScanOutcome(
            code = code,
            state = ScanState.LOADING,
            message = "Enviando al servidor..."
        )
        lastOutcome = initialOutcome
        onOutcomeChange?.invoke(initialOutcome)

        scope.launch {
            try {
                val response: ScanApiResponse = apiClient.sendScan(result)

                val newOutcome = ScanOutcome(
                    code = code,
                    state = if (response.isNew) ScanState.SUCCESS_NEW else ScanState.SUCCESS_EXISTING,
                    message = response.message.ifEmpty {
                        if (response.isNew) "Código registrado como nuevo" else "Código ya existente"
                    }
                )
                lastOutcome = newOutcome
                lastError = null

                // Notificar en hilo principal para actualizar UI
                withContext(Dispatchers.Main) {
                    onOutcomeChange?.invoke(newOutcome)
                }
            } catch (e: ScanApiException) {
                val errorOutcome = ScanOutcome(
                    code = code,
                    state = ScanState.ERROR,
                    message = e.message ?: "Error de servidor"
                )
                lastOutcome = errorOutcome
                lastError = e

                withContext(Dispatchers.Main) {
                    onOutcomeChange?.invoke(errorOutcome)
                }
            } catch (e: Exception) {
                val errorOutcome = ScanOutcome(
                    code = code,
                    state = ScanState.ERROR,
                    message = "Error inesperado: ${e.message}"
                )
                lastOutcome = errorOutcome
                lastError = e

                withContext(Dispatchers.Main) {
                    onOutcomeChange?.invoke(errorOutcome)
                }
            }
        }
    }

    fun clearError() {
        lastError = null
    }
}
