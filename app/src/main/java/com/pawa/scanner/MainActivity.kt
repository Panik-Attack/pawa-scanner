package com.pawa.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.RenderEffect
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
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

    private lateinit var glassBar: LinearLayout
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

        glassBar = findViewById(R.id.glassBar)
        btnScan = findViewById(R.id.btnScan)
        statusText = findViewById(R.id.statusText)

        applyGlassBlur(glassBar)

        btnScan.setOnClickListener {
            triggerScan()
        }

        setupFocusIndicator(glassBar)
    }

    /**
     * Agrega un indicador de foco visible (borde verde pawa) al panel de vidrio,
     * para navegacion por teclado o D-pad en terminales PDA.
     */
    private fun setupFocusIndicator(view: View?) {
        view?.let {
            it.isFocusable = true
            it.isFocusableInTouchMode = true
            it.setOnFocusChangeListener { _, hasFocus ->
                it.setOutlineAmbientShadowColor(
                    if (hasFocus) android.graphics.Color.parseColor("#6E9C19")
                    else android.graphics.Color.TRANSPARENT
                )
            }
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

    /**
     * Aplica blur de fondo al panel de vidrio segun el nivel de API.
     *
     * - API 31+: RenderEffect real (blur 24dp) sobre el contenido trasero.
     * - API < 31: sin blur (el drawable ya usa fill translúcido 8 % como fallback).
     */
    private fun applyGlassBlur(view: View?) {
        if (view == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            view.setRenderEffect(
                RenderEffect.createBlurEffect(24f, 24f, android.graphics.Shader.TileMode.CLAMP)
            )
        }
    }
}
