package com.pawa.scanner.scan

/**
 * Repositorio de escaneos.
 *
 * Implementacion en memoria que mantiene el historial de escaneos y el
 * ultimo estado (cargando, exito o error). Cuando se defina el contrato
 * de la API, esta clase es el punto natural para agregar la capa de red
 * (Retrofit/OkHttp/HttpURLConnection) sin tocar la UI.
 */
class ScanRepository {

    private val scans = mutableListOf<ScanResult>()

    var lastResult: ScanResult? = null
        private set

    var lastError: Throwable? = null
        private set

    var isLoading: Boolean = false
        private set

    val history: List<ScanResult>
        get() = scans.toList()

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

    fun clearError() {
        lastError = null
    }
}
