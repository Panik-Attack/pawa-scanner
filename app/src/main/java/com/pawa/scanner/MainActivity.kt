package com.pawa.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.pawa.scanner.barcode.BarcodeExtractor
import com.pawa.scanner.scan.ScanRepository
import com.pawa.scanner.scan.ScanResult

class MainActivity : AppCompatActivity() {

    private val barcodeExtractor = BarcodeExtractor()
    private val scanRepository = ScanRepository()

    private lateinit var btnScan: MaterialButton
    private lateinit var statusText: TextView

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val barcode = extractBarcode(intent)
            if (!barcode.isNullOrBlank()) {
                handleScannedCode(barcode.trim())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnScan = findViewById(R.id.btnScan)
        statusText = findViewById(R.id.statusText)

        btnScan.setOnClickListener {
            triggerScan()
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction("com.teradevice.scan.RESULT")
            addAction("com.tera.android.scan.result")
            addAction("android.intent.ACTION_DECODE_DATA")
            addAction("com.android.server.scannerservice.broadcast")
            addAction("com.barcode.sendBroadcast")
            addAction("com.scanner.broadcast")
            addAction("com.honeywell.decode.intent.action.SCAN_RESULT")
            addAction("com.zebra.scanner.RESULT_ACTION")
            addAction("nlscan.action.SCANNER_RESULT")
            addAction("com.se4500.onDecodeComplete")
            addAction("com.urovo.scan.result")
            addAction("com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(scanReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(scanReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(scanReceiver)
        } catch (_: Exception) {
        }
    }

    private fun triggerScan() {
        val actions = listOf(
            "com.teradevice.scan.START",
            "com.tera.android.scan.start",
            "com.android.scanservice.scan",
            "com.scanner.action.START_SCAN",
            "ACTION_BARCODE_START",
            "com.teradevice.scanner.START"
        )
        for (action in actions) {
            try {
                val intent = Intent(action)
                sendBroadcast(intent)
            } catch (_: Exception) {
            }
        }
        Toast.makeText(this, "Listo para escanear...", Toast.LENGTH_SHORT).show()
    }

    private fun extractBarcode(intent: Intent): String? {
        val stringExtras = barcodeExtractor.stringKeys.associateWith { intent.getStringExtra(it) }
        val byteExtras = barcodeExtractor.byteKeys.associateWith { intent.getByteArrayExtra(it) }
        return barcodeExtractor.extract(stringExtras, byteExtras)
    }

    /**
     * Maneja un codigo escaneado. Registra el escaneo en el repositorio,
     * muestra feedback visual y deja preparada la insercion de la capa de
     * red (API) cuando se defina el contrato (endpoint, headers, auth,
     * cuerpo y query).
     *
     * Por ahora el envio a API es un TODO; el codigo se almacena localmente
     * y se notifica al usuario mediante Toast y texto de estado.
     */
    private fun handleScannedCode(code: String) {
        val result = ScanResult(code = code)
        scanRepository.addScan(result)

        showStatus("Codigo recibido: $code")
        Toast.makeText(this, "Tracking: $code", Toast.LENGTH_LONG).show()

        // TODO: cuando se defina el contrato de la API, enviar result aqui:
        // scanRepository.setLoading(true)
        // apiClient.sendScan(result) { success ->
        //     scanRepository.setLoading(false)
        //     if (success) { showExito() } else { showError(...) }
        // }
    }

    /**
     * Muestra un mensaje de estado temporal en la zona de feedback del panel
     * de vidrio. Reemplaza cualquier mensaje previo.
     */
    private fun showStatus(message: String) {
        statusText.text = message
        statusText.visibility = View.VISIBLE
    }

}
