package com.pawa.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.pawa.scanner.api.ScanApiException
import com.pawa.scanner.audio.ScanSound
import com.pawa.scanner.audio.ScanSoundPlayer
import com.pawa.scanner.barcode.BarcodeExtractor
import com.pawa.scanner.scan.ScanRepository
import com.pawa.scanner.scan.ScanState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val barcodeExtractor = BarcodeExtractor()
    private val scanRepository = ScanRepository.get()
    private lateinit var soundPlayer: ScanSoundPlayer
    private val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var ivLogo: ImageView
    private lateinit var ivAnimation: ImageView
    private lateinit var pbLoading: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var btnScan: MaterialButton

    private var scanJob: Job? = null
    private var isAudioUnlocked = false

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

        ivLogo = findViewById(R.id.ivLogo)
        ivAnimation = findViewById(R.id.ivAnimation)
        pbLoading = findViewById(R.id.pbLoading)
        statusText = findViewById(R.id.statusText)
        btnScan = findViewById(R.id.btnScan)

        soundPlayer = ScanSoundPlayer.get(this)

        btnScan.setOnClickListener {
            // Desbloqueo de audio en el primer gesto del usuario
            if (!isAudioUnlocked) {
                soundPlayer.unlockAudio()
                isAudioUnlocked = true
            }
            triggerScan()
        }

        setupResultObserver()
    }

    /**
     * Observa los cambios de estado del repositorio y actualiza la UI
     * con animación de check/X, sonido y texto de estado.
     */
    private fun setupResultObserver() {
        scanRepository.onOutcomeChange = { outcome ->
            when (outcome.state) {
                ScanState.IDLE -> resetUI()
                ScanState.LOADING -> showLoading(outcome.message)
                ScanState.SUCCESS_NEW -> showSuccess(outcome.message)
                ScanState.SUCCESS_EXISTING -> showExisting(outcome.message)
                ScanState.ERROR -> showError(outcome.message)
            }
        }
    }

    /**
     * Reinicia la UI a su estado inicial.
     */
    private fun resetUI() {
        ivAnimation.alpha = 0f
        ivAnimation.visibility = View.GONE
        ivAnimation.setImageResource(0)

        pbLoading.alpha = 0f
        pbLoading.visibility = View.GONE

        statusText.alpha = 0f
        statusText.visibility = View.GONE

        btnScaleToNormal()
    }

    /**
     * Muestra el estado de carga: spinner y mensaje.
     */
    private fun showLoading(message: String) {
        ivAnimation.alpha = 0f
        ivAnimation.visibility = View.GONE
        ivAnimation.setImageResource(0)

        pbLoading.visibility = View.VISIBLE
        pbLoading.alpha = 0f
        pbLoading.animate().alpha(1f).setDuration(200).start()

        statusText.visibility = View.VISIBLE
        statusText.alpha = 0f
        statusText.text = message
        statusText.animate().alpha(1f).setDuration(200).start()

        btnScaleToNormal()
    }

    /**
     * Muestra animación de éxito (check verde) y reproduce sonido de éxito.
     */
    private fun showSuccess(message: String) {
        pbLoading.visibility = View.GONE
        pbLoading.alpha = 0f

        ivAnimation.setImageResource(R.drawable.ic_check)
        ivAnimation.visibility = View.VISIBLE
        ivAnimation.alpha = 0f
        ivAnimation.scaleX = 0f
        ivAnimation.scaleY = 0f

        ivAnimation.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(320)
            .start()

        statusText.visibility = View.VISIBLE
        statusText.alpha = 0f
        statusText.text = message
        statusText.animate().alpha(1f).setDuration(320).start()

        soundPlayer.play(ScanSound.SUCCESS)
        btnScaleToNormal()

        // Ocultar animación después de 2 segundos y volver a estado inicial
        scope.launch {
            kotlinx.coroutines.delay(2000)
            resetUI()
        }
    }

    /**
     * Muestra animación de código ya existente (X roja) y sonido de error.
     */
    private fun showExisting(message: String) {
        showResultWithError(message, R.drawable.ic_close, ScanSound.ERROR)
    }

    /**
     * Muestra animación de error (X roja) y sonido de error.
     */
    private fun showError(message: String) {
        showResultWithError(message, R.drawable.ic_close, ScanSound.ERROR)
    }

    private fun showResultWithError(message: String, iconRes: Int, sound: ScanSound) {
        pbLoading.visibility = View.GONE
        pbLoading.alpha = 0f

        ivAnimation.setImageResource(iconRes)
        ivAnimation.visibility = View.VISIBLE
        ivAnimation.alpha = 0f
        ivAnimation.scaleX = 0f
        ivAnimation.scaleY = 0f

        ivAnimation.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(320)
            .start()

        statusText.visibility = View.VISIBLE
        statusText.alpha = 0f
        statusText.text = message
        statusText.animate().alpha(1f).setDuration(320).start()

        soundPlayer.play(sound)
        btnScaleToNormal()

        scope.launch {
            kotlinx.coroutines.delay(2000)
            resetUI()
        }
    }

    /**
     * Devuelve el botón a su escala normal (1.0).
     */
    private fun btnScaleToNormal() {
        btnScan.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction("com.teradevice.scan.RESULT")
            addAction("com.tera.android.scan.result")
            addAction("com.tera.scan.action.BARCODE_RECEIVED")
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

    override fun onDestroy() {
        super.onDestroy()
        scanJob?.cancel()
        soundPlayer.release()
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
     * Maneja un código escaneado:
     * 1. Desbloquea audio si es el primer gesto
     * 2. Envía a ScanRepository.processScan (que llama a la API)
     * 3. La UI se actualiza vía observer (loading → success/existing/error)
     */
    private fun handleScannedCode(code: String) {
        if (!isAudioUnlocked) {
            soundPlayer.unlockAudio()
            isAudioUnlocked = true
        }

        // Cancelar escaneo previo si aún está en curso
        scanJob?.cancel()

        scanRepository.processScan(code, scope)
    }
}
