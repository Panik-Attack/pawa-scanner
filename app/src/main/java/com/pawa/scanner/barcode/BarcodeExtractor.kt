package com.pawa.scanner.barcode

/**
 * Extrae el codigo de barras de los datos devueltos por lectores industriales.
 *
 * Cada fabricante usa claves y acciones distintas en sus Intents de respuesta.
 * Esta clase es pura (sin dependencias de Android) para poder probarla con
 * tests unitarios sin instrumentation.
 */
class BarcodeExtractor {

    val stringKeys = listOf(
        "barcode", "BARCODE", "data", "DATA", "scan_result", "SCAN_RESULT",
        "result", "value", "code", "message", "decoded_data", "extral_data",
        "SCAN_BARCODE", "barcode_string"
    )

    val byteKeys = listOf("barcode_bytes", "data_bytes")

    /**
     * Extrae el codigo de barras a partir de los extras de un Intent.
     *
     * @param stringExtras mapa de claves a valores String (simula intent.getStringExtra)
     * @param byteExtras mapa de claves a arreglos de bytes (simula intent.getByteArrayExtra)
     * @return el codigo detectado, o null si no hay ninguno
     */
    fun extract(
        stringExtras: Map<String, String?>,
        byteExtras: Map<String, ByteArray?>
    ): String? {
        for (key in stringKeys) {
            val value = stringExtras[key]
            if (value.isNullOrBlank()) continue
            return value.trim()
        }

        for (key in byteKeys) {
            val bytes = byteExtras[key]
            if (bytes == null || bytes.isEmpty()) continue
            return String(bytes).trim()
        }

        return null
    }
}
