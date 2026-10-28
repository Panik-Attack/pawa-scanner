package com.pawa.scanner.barcode

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class BarcodeExtractorTest {

    private val extractor = BarcodeExtractor()

    private fun stringExtras(vararg pairs: Pair<String, String?>): Map<String, String?> =
        pairs.toMap()

    private fun byteExtras(vararg pairs: Pair<String, ByteArray?>): Map<String, ByteArray?> =
        pairs.toMap()

    @Test
    fun `devuelve codigo cuando se encuentra en primera clave de string`() {
        val result = extractor.extract(
            stringExtras("barcode" to "123456789"),
            byteExtras()
        )
        assertEquals("123456789", result)
    }

    @Test
    fun `recorre claves de string hasta encontrar la primera con valor`() {
        val result = extractor.extract(
            stringExtras(
                "barcode" to null,
                "data" to "",
                "scan_result" to "ABC-999"
            ),
            byteExtras()
        )
        assertEquals("ABC-999", result)
    }

    @Test
    fun `devuelve valor de claves de bytes cuando no hay strings validos`() {
        val result = extractor.extract(
            stringExtras("barcode" to null),
            byteExtras("barcode_bytes" to "BYTE-123".toByteArray())
        )
        assertEquals("BYTE-123", result)
    }

    @Test
    fun `ignora arreglos de bytes vacios o nulos`() {
        val result = extractor.extract(
            stringExtras("barcode" to null),
            byteExtras(
                "barcode_bytes" to null,
                "data_bytes" to emptyByteArray()
            )
        )
        assertNull(result)
    }

    @Test
    fun `devuelve null cuando no hay ni strings ni bytes`() {
        val result = extractor.extract(
            stringOfEmptyMap(),
            byteOfEmptyMap()
        )
        assertNull(result)
    }

    @Test
    fun `devuelve null cuando todos los valores son blancos o nulos`() {
        val result = extractor.extract(
            stringExtras(
                "barcode" to "   ",
                "data" to null,
                "result" to "\t\n"
            ),
            byteExtras("data_bytes" to "   ".toByteArray())
        )
        assertNull(result)
    }

    @Test
    fun `trimma espacios alrededor del codigo`() {
        val result = extractor.extract(
            stringExtras("barcode" to "  12345  "),
            byteExtras()
        )
        assertEquals("12345", result)
    }

    private fun stringOfEmptyMap(): Map<String, String?> = emptyMap()
    private fun byteOfEmptyMap(): Map<String, ByteArray?> = emptyMap()
}
