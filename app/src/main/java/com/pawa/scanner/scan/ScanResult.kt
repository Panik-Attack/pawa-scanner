package com.pawa.scanner.scan

/**
 * Resultado de un escaneo de codigo de barras, listo para enviarse a la API.
 *
 * La API concreta (endpoint, headers, auth, cuerpo) se definira en un
 * paso posterior. Este modelo guarda el codigo, el lector de origen y el
 * timestamp para que la capa de red los use cuando este disponible.
 */
data class ScanResult(
    val code: String,
    val source: String? = null,
    val scannedAtMillis: Long = System.currentTimeMillis()
)
