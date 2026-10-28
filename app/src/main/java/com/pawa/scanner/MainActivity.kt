package com.pawa.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

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

        val btnScan = findViewById<MaterialButton>(R.id.btnScan)
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
        val keys = arrayOf(
            "barcode",
            "BARCODE",
            "data",
            "DATA",
            "scan_result",
            "SCAN_RESULT",
            "result",
            "value",
            "code",
            "message",
            "decoded_data",
            "extral_data",
            "SCAN_BARCODE",
            "barcode_string"
        )
        for (key in keys) {
            val value = intent.getStringExtra(key)
            if (!value.isNullOrBlank()) return value
        }
        val byteKeys = arrayOf("barcode_bytes", "data_bytes")
        for (key in byteKeys) {
            val bytes = intent.getByteArrayExtra(key)
            if (bytes != null && bytes.isNotEmpty()) {
                return String(bytes).trim()
            }
        }
        return null
    }

    private fun handleScannedCode(code: String) {
        // TODO: enviar a API cuando me pases endpoint/headers/auth/body/query
        Toast.makeText(this, "Tracking: $code", Toast.LENGTH_LONG).show()
    }
}
