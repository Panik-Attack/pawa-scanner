package com.pawa.scanner.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log

/**
 * Reproductor de sonidos de escaneo sintetizados.
 *
 * Diseñado para almacenes ruidosos: volumen alto (pico 0.8), frecuencias claras.
 *
 * Sonidos:
 * - [ScanSound.SUCCESS]: onda triangle, 880 Hz → 1318.5 Hz, ~0.38 s (quinta justa ascendente)
 * - [ScanSound.ERROR]: onda square, 233 Hz → 175 Hz, ~0.48 s (pulsos graves descendentes)
 *
 * Características:
 * - Mute persistente vía SharedPreferences.
 * - Desbloqueo de audio en el primer gesto del usuario (política de autoplay).
 * - Un solo sonido a la vez; si suena uno, se detiene el anterior.
 */
class ScanSoundPlayer private constructor(
    private val context: Context,
    private val prefs: SharedPreferences
) {

    companion object {
        private const val TAG = "ScanSoundPlayer"
        private const val PREFS_NAME = "pawa_scanner_audio"
        private const val KEY_MUTE = "mute"
        private const val KEY_AUDIO_UNLOCKED = "audio_unlocked"

        @Volatile private var instance: ScanSoundPlayer? = null

        fun get(context: Context): ScanSoundPlayer {
            return instance ?: synchronized(this) {
                instance ?: ScanSoundPlayer(
                    context.applicationContext,
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
        }
    }

    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    /**
     * Reproduce el sonido asociado a un escaneo.
     * Si está muteado, no hace nada.
     */
    fun play(sound: ScanSound) {
        if (isMuted()) {
            Log.d(TAG, "Sonido muteado: ${sound.name}")
            return
        }

        stop()

        val thread = Thread {
            try {
                generateAndPlay(sound)
            } catch (e: Exception) {
                Log.e(TAG, "Error reproduciendo sonido ${sound.name}", e)
            } finally {
                isPlaying = false
            }
        }
        thread.isDaemon = true
        thread.start()
    }

    /**
     * Reproduce un sonido corto de desbloqueo al primer gesto del usuario.
     * Satisface la política de autoplay de audio.
     */
    fun unlockAudio() {
        if (isAudioUnlocked()) return
        setAudioUnlocked(true)
        // Sonido breve de desbloqueo: beep suave de 440 Hz por 100 ms
        val thread = Thread {
            try {
                playTone(440.0, 100, 0.3f)
            } catch (e: Exception) {
                Log.d(TAG, "Audio unlock tone skipped", e)
            } finally {
                isPlaying = false
            }
        }
        thread.isDaemon = true
        thread.start()
    }

    fun setMute(muted: Boolean) {
        prefs.edit().putBoolean(KEY_MUTE, muted).apply()
    }

    fun isMuted(): Boolean {
        return prefs.getBoolean(KEY_MUTE, false)
    }

    fun isAudioUnlocked(): Boolean {
        return prefs.getBoolean(KEY_AUDIO_UNLOCKED, false)
    }

    private fun setAudioUnlocked(unlocked: Boolean) {
        prefs.edit().putBoolean(KEY_AUDIO_UNLOCKED, unlocked).apply()
    }

    private fun generateAndPlay(sound: ScanSound) {
        val sampleRate = 44100
        val durationMs = sound.durationMs
        val sampleCount = (sampleRate * durationMs / 1000).toInt()

        val buffer = FloatArray(sampleCount)
        val startFreq = sound.startFreq
        val endFreq = sound.endFreq

        for (i in 0 until sampleCount) {
            val t = i.toFloat() / sampleRate
            val progress = t / (durationMs / 1000f)
            val freq = when {
                progress >= 1f -> endFreq
                else -> startFreq + (endFreq - startFreq) * progress
            }

            val sample = when (sound.waveform) {
                Waveform.TRIANGLE -> triangleWave(freq, t)
                Waveform.SQUARE -> squareWave(freq, t)
            }

            // Aplicar envolvente simple (fade in/out de 20 ms para evitar clics)
            val envelope = envelope(i, sampleCount, 20)
            buffer[i] = sample * envelope * 0.8f // volumen pico 0.8
        }

        playBuffer(buffer, sampleRate)
    }

    private fun playBuffer(samples: FloatArray, sampleRate: Int) {
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        ).coerceAtLeast(samples.size * 4)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .build()

        audioTrack?.play()
        audioTrack?.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
        isPlaying = true

        // Esperar a que termine de reproducirse
        val durationMs = (samples.size.toFloat() / sampleRate * 1000).toLong()
        Thread.sleep(durationMs + 50)

        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        isPlaying = false
    }

    private fun playTone(freq: Double, durationMs: Long, volume: Float) {
        val sampleRate = 44100
        val sampleCount = (sampleRate * durationMs / 1000).toInt()
        val buffer = FloatArray(sampleCount)

        for (i in 0 until sampleCount) {
            val t = i.toFloat() / sampleRate
            val sample = Math.sin(2.0 * Math.PI * freq * t).toFloat()
            buffer[i] = sample * volume
        }

        playBuffer(buffer, sampleRate)
    }

    private fun triangleWave(freq: Double, t: Float): Float {
        val period = 1.0 / freq
        val phase = (t % period.toFloat()) / period.toFloat()
        return if (phase < 0.5f) {
            (4.0f * phase - 1.0f).coerceIn(-1f, 1f)
        } else {
            (3.0f - 4.0f * phase).coerceIn(-1f, 1f)
        }
    }

    private fun squareWave(freq: Double, t: Float): Float {
        val period = 1.0 / freq
        val phase = (t % period.toFloat()) / period.toFloat()
        return if (phase < 0.5f) 0.8f else -0.8f
    }

    private fun envelope(index: Int, total: Int, fadeMs: Int): Float {
        val sampleRate = 44100
        val fadeSamples = (sampleRate * fadeMs / 1000).toInt()
        val t = index.toFloat()
        return when {
            index < fadeSamples -> t / fadeSamples
            index > total - fadeSamples -> (total - index).toFloat() / fadeSamples
            else -> 1f
        }
    }

    fun stop() {
        isPlaying = false
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    fun release() {
        stop()
    }
}

/**
 * Tipos de sonido de escaneo disponibles.
 */
enum class ScanSound(val startFreq: Double, val endFreq: Double, val durationMs: Long, val waveform: Waveform) {
    SUCCESS(880.0, 1318.5, 380, Waveform.TRIANGLE),
    ERROR(233.0, 175.0, 480, Waveform.SQUARE)
}

enum class Waveform { TRIANGLE, SQUARE }
