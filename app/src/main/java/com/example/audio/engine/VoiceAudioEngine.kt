package com.example.audio.engine

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Process
import android.util.Log
import com.example.audio.dsp.DspAudioProcessor
import com.example.audio.dsp.SyntheticVoiceGenerator
import com.example.audio.model.AudioMetrics
import com.example.audio.model.AudioVisualizerData
import com.example.audio.model.VoiceParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.log10
import kotlin.math.max

/**
 * Real-time audio engine that captures mic input, processes through DSP,
 * and routes to either local loopback (Test Voice) or outgoing call audio stream.
 */
class VoiceAudioEngine(
    private val context: Context
) {
    companion object {
        private const val TAG = "VoiceAudioEngine"
        const val SAMPLE_RATE = 24000
        const val BUFFER_SIZE = 512 // 512 samples at 24kHz = 21.3ms processing latency
    }

    private val dsp = DspAudioProcessor(SAMPLE_RATE)
    private val synthGen = SyntheticVoiceGenerator(SAMPLE_RATE)

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val isRunning = AtomicBoolean(false)
    private var audioThread: Thread? = null

    // Engine Controls
    private var voiceParameters = VoiceParameters()
    private val _visualizerFlow = MutableStateFlow(AudioVisualizerData())
    val visualizerFlow = _visualizerFlow.asStateFlow()

    private val _metricsFlow = MutableStateFlow(AudioMetrics())
    val metricsFlow = _metricsFlow.asStateFlow()

    // Loopback / Test Voice
    var isLoopbackEnabled = false
        private set
    var isSyntheticInputActive = false
        private set
    var isCallStreamActive = false
        private set

    // Test Recorder (Record 5s & Playback Preview)
    private val testAudioBuffer = ArrayList<FloatArray>(250) // ~5 seconds of 512 sample blocks
    private val isRecordingTest = AtomicBoolean(false)
    private val isPlayingTest = AtomicBoolean(false)
    private var testPlaybackIndex = 0

    // Outgoing Call Stream listener callback (WebRTC Outgoing Pipeline)
    var outgoingCallStreamListener: ((FloatArray, Int) -> Unit)? = null

    init {
        startAudioProcessingLoop()
    }

    fun updateParameters(params: VoiceParameters) {
        this.voiceParameters = params
    }

    fun setLoopbackEnabled(enabled: Boolean) {
        this.isLoopbackEnabled = enabled
        updateMetrics()
    }

    fun setSyntheticInput(enabled: Boolean) {
        this.isSyntheticInputActive = enabled
    }

    fun setCallStreamActive(active: Boolean) {
        this.isCallStreamActive = active
    }

    fun startTestRecording() {
        testAudioBuffer.clear()
        isPlayingTest.set(false)
        isRecordingTest.set(true)
        updateMetrics()
    }

    fun stopTestRecording() {
        isRecordingTest.set(false)
        updateMetrics()
    }

    fun startTestPlayback() {
        if (testAudioBuffer.isEmpty()) return
        isRecordingTest.set(false)
        testPlaybackIndex = 0
        isPlayingTest.set(true)
        updateMetrics()
    }

    fun stopTestPlayback() {
        isPlayingTest.set(false)
        updateMetrics()
    }

    fun hasTestAudio(): Boolean = testAudioBuffer.isNotEmpty()

    @SuppressLint("MissingPermission")
    private fun startAudioProcessingLoop() {
        if (isRunning.getAndSet(true)) return

        audioThread = Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

            val minRecordBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val minTrackBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            var audioRecord: AudioRecord? = null
            var audioTrack: AudioTrack? = null

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    max(minRecordBuf, BUFFER_SIZE * 4)
                )
                if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.startRecording()
                } else {
                    Log.w(TAG, "AudioRecord initialization failed, fallback to synthetic voice")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cannot start AudioRecord: ${e.message}")
            }

            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()

                audioTrack = AudioTrack(
                    audioAttributes,
                    audioFormat,
                    max(minTrackBuf, BUFFER_SIZE * 4),
                    AudioTrack.MODE_STREAM,
                    android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
                )
                if (audioTrack.state == AudioTrack.STATE_INITIALIZED) {
                    audioTrack.play()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cannot start AudioTrack: ${e.message}")
            }

            val inShorts = ShortArray(BUFFER_SIZE)
            val outShorts = ShortArray(BUFFER_SIZE)
            val inFloats = FloatArray(BUFFER_SIZE)
            val outFloats = FloatArray(BUFFER_SIZE)
            val synthFloats = FloatArray(BUFFER_SIZE)

            var frameCounter = 0
            val startTime = System.currentTimeMillis()

            while (isRunning.get()) {
                val blockStartTime = System.nanoTime()

                // 1. Capture Microphone Audio or Read Recorded Test
                var hasRealMicAudio = false

                if (isPlayingTest.get()) {
                    // Playback recorded test audio
                    if (testPlaybackIndex < testAudioBuffer.size) {
                        val recordedBlock = testAudioBuffer[testPlaybackIndex++]
                        System.arraycopy(recordedBlock, 0, inFloats, 0, BUFFER_SIZE)
                    } else {
                        // Loop test audio or finish
                        testPlaybackIndex = 0
                        isPlayingTest.set(false)
                        inFloats.fill(0f)
                    }
                } else if (!isSyntheticInputActive && audioRecord != null && audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val readCount = audioRecord.read(inShorts, 0, BUFFER_SIZE)
                    if (readCount > 0) {
                        for (i in 0 until readCount) {
                            inFloats[i] = inShorts[i] / 32768.0f
                        }
                        hasRealMicAudio = true
                    }
                }

                // If synthetic mic is forced, or real mic is silent/not available:
                if (isSyntheticInputActive || (!hasRealMicAudio && !isPlayingTest.get())) {
                    synthGen.fillBuffer(synthFloats, BUFFER_SIZE)
                    System.arraycopy(synthFloats, 0, inFloats, 0, BUFFER_SIZE)
                }

                // Record to test buffer if recording is active
                if (isRecordingTest.get()) {
                    if (testAudioBuffer.size < 250) { // cap at ~5.3 seconds
                        testAudioBuffer.add(inFloats.copyOf())
                    } else {
                        isRecordingTest.set(false)
                    }
                }

                // 2. Real-time DSP Voice Processing
                dsp.process(inFloats, outFloats, BUFFER_SIZE, voiceParameters)

                // 3. Routing: Outgoing Call Stream
                if (isCallStreamActive) {
                    outgoingCallStreamListener?.invoke(outFloats, BUFFER_SIZE)
                }

                // 4. Routing: Local Loopback / Test Voice playback to speaker/headphones
                if ((isLoopbackEnabled || isPlayingTest.get()) && audioTrack != null) {
                    for (i in 0 until BUFFER_SIZE) {
                        val sample = (outFloats[i] * 32767.0f).toInt().coerceIn(-32768, 32767)
                        outShorts[i] = sample.toShort()
                    }
                    audioTrack.write(outShorts, 0, BUFFER_SIZE)
                }

                // 5. Measure Latency & Metrics periodically
                val blockDurationMs = ((System.nanoTime() - blockStartTime) / 1_000_000.0).toFloat()
                // Typical buffer acoustic + DSP latency = (512 / 24000) * 1000 = 21.3ms + block processing time
                val estimatedLatencyMs = (21.3f + blockDurationMs).toInt().coerceIn(22, 95)

                frameCounter++
                if (frameCounter % 3 == 0) {
                    val inRms = dsp.lastInputRms
                    val outRms = dsp.lastOutputRms
                    val inDb = if (inRms > 0.00001f) 20f * log10(inRms) else -70f
                    val outDb = if (outRms > 0.00001f) 20f * log10(outRms) else -70f

                    _visualizerFlow.value = dsp.getVisualizerSnapshot()
                    _metricsFlow.value = AudioMetrics(
                        inputRmsDb = inDb.coerceIn(-70f, 0f),
                        outputRmsDb = outDb.coerceIn(-70f, 0f),
                        inputPeak = dsp.lastInputPeak,
                        outputPeak = dsp.lastOutputPeak,
                        latencyMs = estimatedLatencyMs,
                        isClipping = dsp.isClippingOccurred,
                        sampleRate = SAMPLE_RATE,
                        bufferSizeSamples = BUFFER_SIZE,
                        loopbackActive = isLoopbackEnabled,
                        isRecordingTest = isRecordingTest.get(),
                        isPlayingTest = isPlayingTest.get(),
                        testAudioSeconds = (testAudioBuffer.size * BUFFER_SIZE.toFloat() / SAMPLE_RATE)
                    )
                }

                // Throttle pacing when using purely synthetic/playback without hardware audio sync
                if ((!hasRealMicAudio && !isLoopbackEnabled && !isPlayingTest.get())) {
                    val targetMs = (BUFFER_SIZE * 1000L) / SAMPLE_RATE // ~21ms
                    val elapsedMs = (System.nanoTime() - blockStartTime) / 1_000_000L
                    val sleepMs = targetMs - elapsedMs
                    if (sleepMs > 0) {
                        try {
                            Thread.sleep(sleepMs)
                        } catch (ignored: InterruptedException) {
                            break
                        }
                    }
                }
            }

            try {
                audioRecord?.stop()
                audioRecord?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing AudioRecord", e)
            }
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing AudioTrack", e)
            }
        }, "VoiceMod-AudioEngineThread").apply {
            start()
        }
    }

    private fun updateMetrics() {
        val current = _metricsFlow.value
        _metricsFlow.value = current.copy(
            loopbackActive = isLoopbackEnabled,
            isRecordingTest = isRecordingTest.get(),
            isPlayingTest = isPlayingTest.get(),
            testAudioSeconds = (testAudioBuffer.size * BUFFER_SIZE.toFloat() / SAMPLE_RATE)
        )
    }

    fun stop() {
        isRunning.set(false)
        audioThread?.interrupt()
        try {
            audioThread?.join(500)
        } catch (ignored: Exception) {}
    }
}
