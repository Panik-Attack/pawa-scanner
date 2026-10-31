package com.pawa.scanner

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.pawa.scanner.api.ScanApiException
import com.pawa.scanner.audio.ScanSound
import com.pawa.scanner.audio.ScanSoundPlayer
import com.pawa.scanner.scan.ScanRepository
import com.pawa.scanner.scan.ScanState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val scanRepository = ScanRepository.get()
    private lateinit var soundPlayer: ScanSoundPlayer
    private val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var ivLogo: ImageView
    private lateinit var ivAnimation: ImageView
    private lateinit var pbLoading: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var etTracking: EditText
    private lateinit var btnScan: MaterialButton
    private lateinit var btnSendTracking: MaterialButton

    private var scanJob: Job? = null
    private var isAudioUnlocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ivLogo = findViewById(R.id.ivLogo)
        ivAnimation = findViewById(R.id.ivAnimation)
        pbLoading = findViewById(R.id.pbLoading)
        statusText = findViewById(R.id.statusText)
        etTracking = findViewById(R.id.etTracking)
        btnScan = findViewById(R.id.btnScan)
        btnSendTracking = findViewById(R.id.btnSendTracking)

        soundPlayer = ScanSoundPlayer.get(this)

        // Modo keyboard/teclado: el escáner escribe el código en el input con foco.
        // El teclado del sistema no aparece gracias al inputType adecuado y al
        // windowSoftInputMode=stateAlwaysHidden del manifiesto.
        etTracking.requestFocus()
        etTracking.setShowSoftInputOnFocus(false)

        // Detectar "Enter" del escáner (tecla de intro al terminar de escribir el código)
        etTracking.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val code = etTracking.text.toString().trim()
                if (code.isNotEmpty()) {
                    sendTracking(code)
                }
                true
            } else {
                false
            }
        }

        btnScan.setOnClickListener {
            // Desbloqueo de audio en el primer gesto del usuario
            if (!isAudioUnlocked) {
                soundPlayer.unlockAudio()
                isAudioUnlocked = true
            }
            val code = etTracking.text.toString().trim()
            if (code.isNotEmpty()) {
                // Si hay tracking escrito/escaneado, registrarlo directamente
                sendTracking(code)
            } else {
                // En modo keyboard el escáner escribe directamente en el input con foco;
                // no hay que iniciar un escaneo por broadcast/intent.
                Toast.makeText(this, R.string.msg_ready_to_scan, Toast.LENGTH_SHORT).show()
            }
        }

        btnSendTracking.setOnClickListener {
            if (!isAudioUnlocked) {
                soundPlayer.unlockAudio()
                isAudioUnlocked = true
            }
            sendTracking(etTracking.text.toString().trim())
        }

        setupResultObserver()
    }

    /**
     * Envía el código de tracking a la API. Se usa tanto para entrada manual
     * (botón Enviar tracking) como cuando el escáner escribe el código y se
     * detecta "Enter", o cuando el usuario pulsa Escanear con el input relleno.
     */
    private fun sendTracking(code: String) {
        if (code.isEmpty()) {
            Toast.makeText(this, R.string.msg_enter_tracking, Toast.LENGTH_SHORT).show()
            etTracking.requestFocus()
            return
        }

        hideKeyboard()
        etTracking.clearFocus()

        // Cancelar escaneo previo si aún está en curso y enviar a la API
        scanJob?.cancel()
        scanRepository.processScan(code, scope)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        val view = currentFocus
        if (view != null) {
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
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
     * Reinicia la UI a su estado inicial y deja el input listo para el siguiente tracking.
     */
    private fun resetUI() {
        ivAnimation.alpha = 0f
        ivAnimation.visibility = View.GONE
        ivAnimation.setImageResource(0)

        pbLoading.alpha = 0f
        pbLoading.visibility = View.GONE

        statusText.alpha = 0f
        statusText.visibility = View.GONE

        // El input queda con foco para seguir escaneando (modo teclado)
        etTracking.requestFocus()

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
        // Borrar el input tras un éxito para quedar listo con el siguiente tracking
        etTracking.text.clear()
        etTracking.requestFocus()

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
        // Borrar el input tras un éxito (código ya existente) para quedar listo con el siguiente
        etTracking.text.clear()
        etTracking.requestFocus()

        showResultWithError(message, R.drawable.ic_close, ScanSound.ERROR)
    }

    /**
     * Muestra animación de error (X roja) y sonido de error.
     */
    private fun showError(message: String) {
        showResultWithError(message, R.drawable.ic_close, ScanSound.ERROR)
    }

    private fun showResultWithError(message: String, iconRes: Int, sound: ScanSound) {
        // Borrar el input también en error para quedar listo con el siguiente tracking
        etTracking.text.clear()
        etTracking.requestFocus()

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

    override fun onDestroy() {
        super.onDestroy()
        scanJob?.cancel()
        soundPlayer.release()
    }
}
