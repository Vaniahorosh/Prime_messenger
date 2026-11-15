package com.messenger.prime

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import kotlin.concurrent.thread
import kotlin.math.sin

object TetoVoiceSynthesizer {

    private var activeMediaPlayer: MediaPlayer? = null

    /**
     * Plays the official Kasane Teto audio sound (R.raw.sound_teto) via MediaPlayer,
     * or falls back to synthetic chime if playback is unavailable.
     */
    @JvmStatic
    @JvmOverloads
    fun playTetoSound(context: Context? = null) {
        if (context != null) {
            try {
                activeMediaPlayer?.stop()
                activeMediaPlayer?.release()
                activeMediaPlayer = MediaPlayer.create(context.applicationContext, R.raw.sound_teto)?.apply {
                    setOnCompletionListener { mp ->
                        mp.release()
                        if (activeMediaPlayer == mp) {
                            activeMediaPlayer = null
                        }
                    }
                    start()
                }
                if (activeMediaPlayer != null) return
            } catch (_: Exception) {
                // Fallback to synth chime
            }
        }
        playTetoChime()
    }

    /**
     * Synthesizes Kasane Teto's signature 3-note chime programmatically.
     */
    @JvmStatic
    fun playTetoChime() {
        thread {
            try {
                val sampleRate = 44100
                val notes = doubleArrayOf(440.0, 523.25, 659.25)
                val durationPerNote = 0.12

                val totalSamples = (notes.size * durationPerNote * sampleRate).toInt()
                val buffer = ShortArray(totalSamples)

                var sampleIndex = 0
                val samplesPerNote = (durationPerNote * sampleRate).toInt()

                for (freq in notes) {
                    for (i in 0 until samplesPerNote) {
                        val t = i.toDouble() / sampleRate
                        val envelope = (1.0 - (i.toDouble() / samplesPerNote))
                        val sine = sin(2.0 * Math.PI * freq * t)
                        val harmonic = 0.3 * sin(4.0 * Math.PI * freq * t)
                        val sampleValue = ((sine + harmonic) * envelope * 0.4 * Short.MAX_VALUE).toInt()

                        if (sampleIndex < buffer.size) {
                            buffer[sampleIndex++] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        }
                    }
                }

                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBufferSize, buffer.size * 2))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()

                Thread.sleep(500)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Synthesizes a soft, subtle 2-note micro chime for low-latency UI feedback (button taps, message send).
     */
    @JvmStatic
    fun playTetoMicroChime() {
        thread {
            try {
                val sampleRate = 44100
                val notes = doubleArrayOf(523.25, 659.25)
                val durationPerNote = 0.07

                val totalSamples = (notes.size * durationPerNote * sampleRate).toInt()
                val buffer = ShortArray(totalSamples)

                var sampleIndex = 0
                val samplesPerNote = (durationPerNote * sampleRate).toInt()

                for (freq in notes) {
                    for (i in 0 until samplesPerNote) {
                        val t = i.toDouble() / sampleRate
                        val envelope = (1.0 - (i.toDouble() / samplesPerNote))
                        val sine = sin(2.0 * Math.PI * freq * t)
                        val sampleValue = (sine * envelope * 0.22 * Short.MAX_VALUE).toInt()

                        if (sampleIndex < buffer.size) {
                            buffer[sampleIndex++] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        }
                    }
                }

                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBufferSize, buffer.size * 2))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()

                Thread.sleep(200)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {
            }
        }
    }
}
