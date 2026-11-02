package com.ginckl.thumbplan.services

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Generates the shop's alarm tones as small looping WAV files (no audio assets to ship).
 * Files are cached in filesDir/tones and rebuilt only when missing.
 */
object ToneSynth {
    private const val RATE = 22_050

    fun fileFor(context: Context, toneId: String): File? {
        if (toneId == "system" || toneId !in TONES) return null
        val dir = File(context.filesDir, "tones").apply { mkdirs() }
        val file = File(dir, "$toneId.wav")
        if (!file.exists() || file.length() < 1000) {
            runCatching { file.writeBytes(wav(render(toneId))) }.onFailure { file.delete(); return null }
        }
        return file
    }

    val TONES = setOf("chime", "digital", "marimba", "sunrise", "zen")

    private class Voice(val start: Double, val freq: Double, val dur: Double, val kind: Kind, val gain: Double = 0.6)
    private enum class Kind { BELL, BEEP, MARIMBA, BOWL }

    private fun render(id: String): ShortArray {
        val voices = ArrayList<Voice>()
        var total = 4.0
        when (id) {
            "chime" -> {
                val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.5)
                notes.forEachIndexed { i, f -> voices += Voice(i * 0.3, f, 1.4, Kind.BELL) }
                total = 3.2
            }
            "digital" -> {
                for (rep in 0 until 3) {
                    val t = rep * 1.2
                    voices += Voice(t, 1000.0, 0.12, Kind.BEEP, 0.45)
                    voices += Voice(t + 0.2, 1000.0, 0.12, Kind.BEEP, 0.45)
                }
                total = 3.6
            }
            "marimba" -> {
                val melody = doubleArrayOf(392.0, 493.88, 587.33, 493.88, 392.0, 587.33, 783.99)
                melody.forEachIndexed { i, f -> voices += Voice(i * 0.22, f, 0.7, Kind.MARIMBA) }
                total = 2.6
            }
            "sunrise" -> {
                val chord = doubleArrayOf(261.63, 329.63, 392.0, 523.25)
                chord.forEachIndexed { i, f -> voices += Voice(i * 0.9, f, 2.2, Kind.BELL, 0.35 + i * 0.07) }
                total = 5.0
            }
            "zen" -> {
                voices += Voice(0.0, 196.0, 3.8, Kind.BOWL, 0.55)
                voices += Voice(2.4, 293.66, 3.6, Kind.BOWL, 0.4)
                total = 6.0
            }
        }
        val out = DoubleArray((total * RATE).toInt())
        for (v in voices) mix(out, v)
        var peak = 0.0
        for (s in out) peak = maxOf(peak, kotlin.math.abs(s))
        val norm = if (peak > 0) 0.85 / peak else 1.0
        return ShortArray(out.size) { (out[it] * norm * Short.MAX_VALUE).toInt().toShort() }
    }

    private fun mix(out: DoubleArray, v: Voice) {
        val from = (v.start * RATE).toInt()
        val n = (v.dur * RATE).toInt()
        for (i in 0 until n) {
            val idx = from + i
            if (idx >= out.size) break
            val t = i.toDouble() / RATE
            val w = 2 * PI * v.freq * t
            val s = when (v.kind) {
                Kind.BELL -> (sin(w) + 0.4 * sin(2 * w) + 0.15 * sin(3.01 * w)) * exp(-3.0 * t)
                Kind.BEEP -> {
                    val env = if (t < 0.01) t / 0.01 else if (t > v.dur - 0.02) ((v.dur - t) / 0.02).coerceAtLeast(0.0) else 1.0
                    (if (sin(w) >= 0) 1.0 else -1.0) * 0.6 * env
                }
                Kind.MARIMBA -> (sin(w) + 0.5 * sin(4 * w)) * exp(-7.0 * t)
                Kind.BOWL -> (sin(w) + 0.5 * sin(2.76 * w) + 0.25 * sin(5.4 * w) + 0.3 * sin(w * 1.006)) * exp(-0.9 * t) * (1 - exp(-12.0 * t))
            }
            out[idx] += s * v.gain
        }
    }

    private fun wav(pcm: ShortArray): ByteArray {
        val dataLen = pcm.size * 2
        val b = ByteArrayOutputStream(44 + dataLen)
        fun int(v: Int) { b.write(v and 255); b.write(v shr 8 and 255); b.write(v shr 16 and 255); b.write(v shr 24 and 255) }
        fun short(v: Int) { b.write(v and 255); b.write(v shr 8 and 255) }
        b.write("RIFF".toByteArray()); int(36 + dataLen)
        b.write("WAVE".toByteArray()); b.write("fmt ".toByteArray())
        int(16); short(1); short(1); int(RATE); int(RATE * 2); short(2); short(16)
        b.write("data".toByteArray()); int(dataLen)
        for (s in pcm) short(s.toInt())
        return b.toByteArray()
    }
}
