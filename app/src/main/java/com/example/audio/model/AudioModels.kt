package com.example.audio.model

/**
 * Real-time audio engine runtime metrics.
 */
data class AudioMetrics(
    val inputRmsDb: Float = -60f,
    val outputRmsDb: Float = -60f,
    val inputPeak: Float = 0f,
    val outputPeak: Float = 0f,
    val latencyMs: Int = 28,
    val isClipping: Boolean = false,
    val sampleRate: Int = 24000,
    val bufferSizeSamples: Int = 512,
    val loopbackActive: Boolean = false,
    val isRecordingTest: Boolean = false,
    val isPlayingTest: Boolean = false,
    val testAudioSeconds: Float = 0f
)

/**
 * Visualizer snapshot holding normalized waveform samples and frequency band bars.
 */
data class AudioVisualizerData(
    // 64 raw waveform points in range [-1.0f, 1.0f]
    val waveform: FloatArray = FloatArray(64) { 0f },
    // 16 frequency bands in range [0.0f, 1.0f]
    val spectrumBands: FloatArray = FloatArray(16) { 0f },
    val isVoiceActive: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioVisualizerData
        return waveform.contentEquals(other.waveform) &&
                spectrumBands.contentEquals(other.spectrumBands) &&
                isVoiceActive == other.isVoiceActive
    }

    override fun hashCode(): Int {
        var result = waveform.contentHashCode()
        result = 31 * result + spectrumBands.contentHashCode()
        result = 31 * result + isVoiceActive.hashCode()
        return result
    }
}

/**
 * WebRTC / In-Call simulation state.
 */
enum class CallStatus {
    IDLE,
    CONNECTING,
    CONNECTED,
    MUTED
}

data class CallState(
    val status: CallStatus = CallStatus.IDLE,
    val partnerName: String = "Discord Squad #general",
    val partnerChannel: String = "Voice Room A-12",
    val durationSeconds: Long = 0,
    val rttLatencyMs: Int = 24,
    val jitterMs: Int = 2,
    val packetLossPercent: Float = 0.0f,
    val bitrateKbps: Int = 64,
    val codec: String = "OPUS 48kHz (Fullband)",
    val isOutgoingMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isIncomingVoiceAudible: Boolean = true
) {
    val isCallActive: Boolean
        get() = status == CallStatus.CONNECTED || status == CallStatus.MUTED
}
