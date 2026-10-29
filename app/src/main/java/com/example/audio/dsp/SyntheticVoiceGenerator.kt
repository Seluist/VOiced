package com.example.audio.dsp

import kotlin.math.PI
import kotlin.math.sin

/**
 * Generates synthetic vocal phonemes with natural speech pitch contours
 * for instant testing and emulator demonstration without requiring a physical mic.
 */
class SyntheticVoiceGenerator(
    private val sampleRate: Int = 24000
) {
    private var phase = 0.0
    private var formantPhase1 = 0.0
    private var formantPhase2 = 0.0
    private var sentenceTimer = 0.0

    // Vowel formant frequencies (Hz) for /a/, /o/, /i/, /e/
    private val vowelFormants = arrayOf(
        Pair(730.0, 1090.0), // "ah"
        Pair(570.0, 840.0),  // "oh"
        Pair(270.0, 2290.0), // "ee"
        Pair(530.0, 1840.0)  // "eh"
    )

    fun fillBuffer(buffer: FloatArray, count: Int) {
        val dt = 1.0 / sampleRate.toDouble()

        for (i in 0 until count) {
            sentenceTimer += dt
            if (sentenceTimer > 4.0) sentenceTimer = 0.0

            // Pitch contour: natural human speech intonation ~130 Hz with natural inflection
            val pitchHz = 135.0 + sin(sentenceTimer * 2.2) * 22.0 + sin(sentenceTimer * 6.5) * 8.0
            val fundamentalOmega = 2.0 * PI * pitchHz * dt

            // Vowel cycling: smoothly transition between vowels every second
            val vowelIndex = ((sentenceTimer * 0.8).toInt() % vowelFormants.size)
            val nextVowelIndex = (vowelIndex + 1) % vowelFormants.size
            val vowelFrac = (sentenceTimer * 0.8 - (sentenceTimer * 0.8).toInt())

            val f1 = vowelFormants[vowelIndex].first * (1.0 - vowelFrac) + vowelFormants[nextVowelIndex].first * vowelFrac
            val f2 = vowelFormants[vowelIndex].second * (1.0 - vowelFrac) + vowelFormants[nextVowelIndex].second * vowelFrac

            // Glottal pulse simulation
            phase += fundamentalOmega
            if (phase > 2.0 * PI) {
                phase -= 2.0 * PI
            }

            // Vocal tract impulse excitation
            val glottal = (sin(phase) + 0.5 * sin(2.0 * phase) + 0.25 * sin(3.0 * phase)).toFloat()

            // Formant resonances
            formantPhase1 += 2.0 * PI * f1 * dt
            if (formantPhase1 > 2.0 * PI) formantPhase1 -= 2.0 * PI
            val form1 = (sin(formantPhase1) * 0.45).toFloat()

            formantPhase2 += 2.0 * PI * f2 * dt
            if (formantPhase2 > 2.0 * PI) formantPhase2 -= 2.0 * PI
            val form2 = (sin(formantPhase2) * 0.25).toFloat()

            // Speech cadence envelope (pauses between words)
            val wordEnvelope = ((sin(sentenceTimer * 5.0) + 1.0) * 0.5).toFloat()
            val speechGate = if (sentenceTimer % 1.5 < 1.1) 1.0f else 0.05f

            val rawVoice = (glottal * 0.35f + form1 + form2) * wordEnvelope * speechGate
            buffer[i] = rawVoice.coerceIn(-0.95f, 0.95f)
        }
    }
}
