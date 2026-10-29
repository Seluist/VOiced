package com.example.audio.call

import com.example.audio.engine.VoiceAudioEngine
import com.example.audio.model.CallState
import com.example.audio.model.CallStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Manages WebRTC / Outgoing Call Stream session, active call metrics,
 * and duplex conversation simulation.
 */
class CallManager(
    private val audioEngine: VoiceAudioEngine
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var callTimerJob: Job? = null
    private var outgoingPacketsSent = 0L

    private val _callState = MutableStateFlow(
        CallState(
            status = CallStatus.IDLE,
            partnerName = "Team Squad #voice",
            partnerChannel = "Server: Cyber-Central (Room #4)"
        )
    )
    val callState = _callState.asStateFlow()

    init {
        // Wire up outgoing audio stream callback
        audioEngine.outgoingCallStreamListener = { buffer, count ->
            if (_callState.value.isCallActive && !_callState.value.isOutgoingMuted) {
                outgoingPacketsSent++
            }
        }
    }

    fun startCall(
        partnerName: String = "Discord Squad #voice-general",
        channel: String = "Server: Cyber-Central (Room #4)"
    ) {
        scope.launch {
            _callState.value = _callState.value.copy(
                status = CallStatus.CONNECTING,
                partnerName = partnerName,
                partnerChannel = channel,
                durationSeconds = 0
            )

            // Simulate WebRTC SDP offer/answer handshake & ICE candidate gathering
            delay(900)

            audioEngine.setCallStreamActive(true)

            _callState.value = _callState.value.copy(
                status = CallStatus.CONNECTED,
                rttLatencyMs = 24 + Random.nextInt(8),
                bitrateKbps = 64,
                codec = "OPUS 48kHz (WebRTC-VoIP)",
                packetLossPercent = 0.0f
            )

            startCallTimer()
        }
    }

    fun endCall() {
        callTimerJob?.cancel()
        callTimerJob = null
        audioEngine.setCallStreamActive(false)

        _callState.value = _callState.value.copy(
            status = CallStatus.IDLE,
            durationSeconds = 0
        )
    }

    fun toggleMute() {
        val current = _callState.value
        val newMuted = !current.isOutgoingMuted
        val newStatus = if (newMuted) CallStatus.MUTED else CallStatus.CONNECTED
        _callState.value = current.copy(
            isOutgoingMuted = newMuted,
            status = newStatus
        )
    }

    fun toggleSpeaker() {
        _callState.value = _callState.value.copy(
            isSpeakerOn = !_callState.value.isSpeakerOn
        )
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = scope.launch {
            while (isActive && _callState.value.isCallActive) {
                delay(1000)
                val current = _callState.value
                val jitter = Random.nextInt(1, 4)
                val ping = (22 + Random.nextInt(6)).coerceIn(18, 45)
                _callState.value = current.copy(
                    durationSeconds = current.durationSeconds + 1,
                    rttLatencyMs = ping,
                    jitterMs = jitter
                )
            }
        }
    }
}
