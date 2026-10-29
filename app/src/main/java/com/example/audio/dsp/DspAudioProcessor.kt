package com.example.audio.dsp

import com.example.audio.model.AudioVisualizerData
import com.example.audio.model.VoiceParameters
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * High-performance real-time Digital Signal Processor for voice transformation.
 * Designed for sub-20ms processing latency and zero garbage collection in audio loop.
 */
class DspAudioProcessor(
    private val sampleRate: Int = 24000
) {
    // Pitch shifter delay circular buffer
    private val pitchBufferSize = 4096
    private val pitchBuffer = FloatArray(pitchBufferSize)
    private var pitchWriteIndex = 0
    private var grainPhaseA = 0.0f
    private var grainPhaseB = 0.5f // 180 degrees offset
    private val grainWindowSize = 512f

    // Robot ring modulator state
    private var robotPhase = 0.0f
    private val robotCombBuffer = FloatArray(256)
    private var robotCombIndex = 0

    // Vibrato LFO state
    private var vibratoPhase = 0.0f
    private val vibratoBuffer = FloatArray(1024)
    private var vibratoWriteIndex = 0

    // Reverb: 4 comb filters + 2 all-pass filters (Schroeder model)
    private val combDelays = intArrayOf(
        (sampleRate * 0.029).toInt().coerceAtLeast(100),
        (sampleRate * 0.037).toInt().coerceAtLeast(100),
        (sampleRate * 0.043).toInt().coerceAtLeast(100),
        (sampleRate * 0.051).toInt().coerceAtLeast(100)
    )
    private val combBuffers = Array(4) { FloatArray(combDelays[it] + 1) }
    private val combIndices = IntArray(4)

    private val allpassDelays = intArrayOf(
        (sampleRate * 0.005).toInt().coerceAtLeast(20),
        (sampleRate * 0.0017).toInt().coerceAtLeast(20)
    )
    private val allpassBuffers = Array(2) { FloatArray(allpassDelays[it] + 1) }
    private val allpassIndices = IntArray(2)

    // Biquad filter states for Formant & Bandpass
    private var biquadX1 = 0f
    private var biquadX2 = 0f
    private var biquadY1 = 0f
    private var biquadY2 = 0f

    // Telephone bandpass filter state
    private var telHpX1 = 0f
    private var telHpY1 = 0f
    private var telLpY1 = 0f

    // Visualizer data accumulation
    private val waveHistory = FloatArray(64)
    private val spectrumHistory = FloatArray(16)
    private var waveDecimateCounter = 0

    // Analysis results
    var lastInputRms: Float = 0f
        private set
    var lastOutputRms: Float = 0f
        private set
    var lastInputPeak: Float = 0f
        private set
    var lastOutputPeak: Float = 0f
        private set
    var isClippingOccurred: Boolean = false
        private set

    /**
     * Process an array of mono PCM floating point samples [-1.0f, 1.0f] in-place.
     * @param inSamples Raw input microphone/synthetic samples
     * @param outSamples Destination array for processed audio
     * @param count Number of samples to process
     * @param params Current voice transformation configuration
     */
    fun process(
        inSamples: FloatArray,
        outSamples: FloatArray,
        count: Int,
        params: VoiceParameters
    ) {
        if (!params.isEnabled) {
            // Bypass mode: direct copy with metric tracking
            var sumInSq = 0f
            var maxPeak = 0f
            for (i in 0 until count) {
                val s = inSamples[i]
                outSamples[i] = s
                sumInSq += s * s
                val absS = kotlin.math.abs(s)
                if (absS > maxPeak) maxPeak = absS
            }
            lastInputRms = sqrt(sumInSq / count.coerceAtLeast(1))
            lastOutputRms = lastInputRms
            lastInputPeak = maxPeak
            lastOutputPeak = maxPeak
            updateVisualizerBuffers(outSamples, count)
            return
        }

        var sumInSq = 0f
        var sumOutSq = 0f
        var peakIn = 0f
        var peakOut = 0f
        var clipping = false

        val pitchFactor = params.pitchFactor
        val robotMod = params.robotModulation.coerceIn(0f, 1f)
        val reverbWet = params.reverbWet.coerceIn(0f, 1f)
        val distortion = params.distortion.coerceIn(0f, 1f)
        val noiseGate = params.noiseGate
        val outputGain = params.outputGain

        // Setup Formant Filter Biquad Coefficients
        // Nominal vocal formant resonance ~1200 Hz shifted by formantShift
        val centerFreq = (1100f * params.formantShift).coerceIn(400f, 4800f)
        val q = 1.8f
        val w0 = 2.0 * PI * centerFreq / sampleRate
        val alpha = sin(w0) / (2.0 * q)
        val boostGain = if (params.formantShift != 1.0f) 2.2f else 1.0f
        // Peaking EQ filter coefficients
        val a0 = (1.0 + alpha / boostGain).toFloat()
        val b0 = ((1.0 + alpha * boostGain) / a0).toFloat()
        val b1 = ((-2.0 * cos(w0)) / a0).toFloat()
        val b2 = ((1.0 - alpha * boostGain) / a0).toFloat()
        val a1 = ((-2.0 * cos(w0)) / a0).toFloat()
        val a2 = ((1.0 - alpha / boostGain) / a0).toFloat()

        // Vibrato configuration
        val vibratoRate = params.vibratoRate
        val vibratoDepth = params.vibratoDepth

        // Precompute robot frequency: 65 Hz metallic drone
        val robotFreqOmega = (2.0 * PI * 65.0 / sampleRate).toFloat()

        for (i in 0 until count) {
            var sample = inSamples[i]

            // 1. Input Metering
            val absIn = kotlin.math.abs(sample)
            if (absIn > peakIn) peakIn = absIn
            sumInSq += sample * sample

            // 2. Noise Gate
            if (absIn < noiseGate) {
                sample = 0f
            }

            // 3. Vibrato / Alien pitch modulation
            if (vibratoDepth > 0.01f && vibratoRate > 0.1f) {
                vibratoBuffer[vibratoWriteIndex] = sample
                val vibMod = (sin(vibratoPhase) + 1.0f) * 0.5f * vibratoDepth * 40f
                var readPos = (vibratoWriteIndex - 10f - vibMod)
                while (readPos < 0) readPos += vibratoBuffer.size
                val idx0 = readPos.toInt() % vibratoBuffer.size
                val idx1 = (idx0 + 1) % vibratoBuffer.size
                val frac = readPos - readPos.toInt()
                sample = vibratoBuffer[idx0] * (1f - frac) + vibratoBuffer[idx1] * frac

                vibratoWriteIndex = (vibratoWriteIndex + 1) % vibratoBuffer.size
                vibratoPhase += (2.0 * PI * vibratoRate / sampleRate).toFloat()
                if (vibratoPhase > 2 * PI) vibratoPhase -= (2 * PI).toFloat()
            }

            // 4. Time-Domain Pitch Shifting (Granular Overlap-Add)
            if (kotlin.math.abs(pitchFactor - 1.0f) > 0.01f) {
                pitchBuffer[pitchWriteIndex] = sample

                // Grain A
                val offsetA = grainPhaseA * grainWindowSize
                val readPosA = (pitchWriteIndex - offsetA + pitchBufferSize) % pitchBufferSize
                val idxA0 = readPosA.toInt()
                val idxA1 = (idxA0 + 1) % pitchBufferSize
                val fracA = readPosA - idxA0
                val sampleA = pitchBuffer[idxA0] * (1f - fracA) + pitchBuffer[idxA1] * fracA
                // Triangular/Hann window for grain A
                val winA = 0.5f * (1.0f - cos(2.0 * PI * grainPhaseA)).toFloat()

                // Grain B (offset by 0.5)
                val offsetB = grainPhaseB * grainWindowSize
                val readPosB = (pitchWriteIndex - offsetB + pitchBufferSize) % pitchBufferSize
                val idxB0 = readPosB.toInt()
                val idxB1 = (idxB0 + 1) % pitchBufferSize
                val fracB = readPosB - idxB0
                val sampleB = pitchBuffer[idxB0] * (1f - fracB) + pitchBuffer[idxB1] * fracB
                val winB = 0.5f * (1.0f - cos(2.0 * PI * grainPhaseB)).toFloat()

                sample = (sampleA * winA + sampleB * winB)

                // Advance write & grain phases
                pitchWriteIndex = (pitchWriteIndex + 1) % pitchBufferSize
                val phaseStep = (1.0f - pitchFactor) / grainWindowSize
                grainPhaseA = (grainPhaseA + phaseStep)
                if (grainPhaseA >= 1.0f) grainPhaseA -= 1.0f
                if (grainPhaseA < 0.0f) grainPhaseA += 1.0f

                grainPhaseB = (grainPhaseB + phaseStep)
                if (grainPhaseB >= 1.0f) grainPhaseB -= 1.0f
                if (grainPhaseB < 0.0f) grainPhaseB += 1.0f
            }

            // 5. Formant & Body Resonance Filter (Biquad Peaking EQ)
            if (params.formantShift != 1.0f) {
                val y = b0 * sample + b1 * biquadX1 + b2 * biquadX2 - a1 * biquadY1 - a2 * biquadY2
                biquadX2 = biquadX1
                biquadX1 = sample
                biquadY2 = biquadY1
                biquadY1 = y
                sample = y
            }

            // 6. Cyber-Robot Ring Modulation & Metallic Comb Filter
            if (robotMod > 0.01f) {
                val carrier = sin(robotPhase)
                robotPhase += robotFreqOmega
                if (robotPhase > 2 * PI) robotPhase -= (2 * PI).toFloat()

                // Short comb delay for metallic robotic reflection
                val combLen = 78
                val delayedSample = robotCombBuffer[robotCombIndex]
                robotCombBuffer[robotCombIndex] = sample + delayedSample * 0.65f
                robotCombIndex = (robotCombIndex + 1) % combLen

                val roboticSignal = (sample * carrier * 1.3f) + (delayedSample * 0.45f)
                sample = sample * (1f - robotMod) + roboticSignal * robotMod
            }

            // 7. Telephone Bandpass (300Hz HP + 3400Hz LP)
            if (params.telephoneFilter) {
                // High-pass 300Hz
                val hpAlpha = 0.93f
                val hpOut = hpAlpha * (telHpY1 + sample - telHpX1)
                telHpX1 = sample
                telHpY1 = hpOut

                // Low-pass 3400Hz
                val lpAlpha = 0.58f
                telLpY1 = telLpY1 + lpAlpha * (hpOut - telLpY1)
                sample = telLpY1 * 1.4f
            }

            // 8. Saturation / Drive / Distortion
            if (distortion > 0.01f) {
                val drive = 1.0f + distortion * 4.5f
                sample = tanh(sample * drive)
            }

            // 9. Spatial Schroeder Reverb
            if (reverbWet > 0.01f) {
                var reverbAccum = 0f
                for (c in 0 until 4) {
                    val buf = combBuffers[c]
                    val delay = combDelays[c]
                    val idx = combIndices[c]
                    val delayed = buf[idx]
                    buf[idx] = sample + delayed * 0.78f
                    combIndices[c] = (idx + 1) % delay
                    reverbAccum += delayed
                }
                reverbAccum *= 0.25f

                // Series All-Pass Diffusers
                var apSignal = reverbAccum
                for (a in 0 until 2) {
                    val buf = allpassBuffers[a]
                    val delay = allpassDelays[a]
                    val idx = allpassIndices[a]
                    val delayed = buf[idx]
                    val g = 0.5f
                    val newSample = apSignal + delayed * -g
                    buf[idx] = apSignal + delayed * g
                    allpassIndices[a] = (idx + 1) % delay
                    apSignal = newSample
                }

                sample = sample * (1f - reverbWet * 0.6f) + apSignal * reverbWet
            }

            // 10. Output Gain & Soft Limiting
            var outVal = sample * outputGain
            if (outVal > 1.0f) {
                outVal = 1.0f
                clipping = true
            } else if (outVal < -1.0f) {
                outVal = -1.0f
                clipping = true
            }

            outSamples[i] = outVal

            // Output metering
            val absOut = kotlin.math.abs(outVal)
            if (absOut > peakOut) peakOut = absOut
            sumOutSq += outVal * outVal
        }

        lastInputPeak = peakIn
        lastOutputPeak = peakOut
        lastInputRms = sqrt(sumInSq / count.coerceAtLeast(1))
        lastOutputRms = sqrt(sumOutSq / count.coerceAtLeast(1))
        isClippingOccurred = clipping

        updateVisualizerBuffers(outSamples, count)
    }

    private fun updateVisualizerBuffers(samples: FloatArray, count: Int) {
        if (count <= 0) return
        val step = (count / 64).coerceAtLeast(1)
        for (i in 0 until 64) {
            val sIdx = (i * step).coerceAtMost(count - 1)
            waveHistory[i] = samples[sIdx].coerceIn(-1f, 1f)
        }

        // 16 Frequency Spectrum approximation (Energy across octaves/bands)
        val bandSize = (count / 16).coerceAtLeast(1)
        for (b in 0 until 16) {
            var bandSum = 0f
            val start = b * bandSize
            val end = (start + bandSize).coerceAtMost(count)
            for (j in start until end) {
                val v = kotlin.math.abs(samples[j])
                bandSum += v
            }
            val avg = if (end > start) bandSum / (end - start) else 0f
            // Exponential smoothing
            spectrumHistory[b] = spectrumHistory[b] * 0.7f + (avg * 2.5f).coerceIn(0f, 1f) * 0.3f
        }
    }

    fun getVisualizerSnapshot(): AudioVisualizerData {
        return AudioVisualizerData(
            waveform = waveHistory.copyOf(),
            spectrumBands = spectrumHistory.copyOf(),
            isVoiceActive = lastOutputRms > 0.02f
        )
    }

    fun reset() {
        pitchBuffer.fill(0f)
        robotCombBuffer.fill(0f)
        vibratoBuffer.fill(0f)
        for (b in combBuffers) b.fill(0f)
        for (b in allpassBuffers) b.fill(0f)
        biquadX1 = 0f
        biquadX2 = 0f
        biquadY1 = 0f
        biquadY2 = 0f
        telHpX1 = 0f
        telHpY1 = 0f
        telLpY1 = 0f
    }
}
