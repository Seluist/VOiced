package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.call.CallManager
import com.example.audio.engine.VoiceAudioEngine
import com.example.audio.model.AudioMetrics
import com.example.audio.model.AudioVisualizerData
import com.example.audio.model.CallState
import com.example.audio.model.VoiceParameters
import com.example.audio.model.VoicePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val selectedPreset: VoicePreset = VoicePreset.DEEP_BARITONE,
    val currentParams: VoiceParameters = VoicePreset.DEEP_BARITONE.params,
    val isMasterEnabled: Boolean = true,
    val isLoopbackActive: Boolean = false,
    val isSyntheticVoiceActive: Boolean = false,
    val customPresets: List<CustomPresetItem> = emptyList(),
    val showCustomPresetDialog: Boolean = false,
    val showCallDialog: Boolean = false,
    val activeTab: Int = 0 // 0: Presets, 1: Fine-Tune Studio, 2: Call Comms
)

data class CustomPresetItem(
    val id: String,
    val name: String,
    val params: VoiceParameters
)

class VoiceChangerViewModel(application: Application) : AndroidViewModel(application) {

    val audioEngine = VoiceAudioEngine(application)
    val callManager = CallManager(audioEngine)

    val visualizerData: StateFlow<AudioVisualizerData> = audioEngine.visualizerFlow
    val audioMetrics: StateFlow<AudioMetrics> = audioEngine.metricsFlow
    val callState: StateFlow<CallState> = callManager.callState

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Push initial parameters to DSP engine
        audioEngine.updateParameters(_uiState.value.currentParams.copy(isEnabled = _uiState.value.isMasterEnabled))
    }

    fun toggleMasterVoiceChanger(enabled: Boolean) {
        val updatedParams = _uiState.value.currentParams.copy(isEnabled = enabled)
        _uiState.value = _uiState.value.copy(
            isMasterEnabled = enabled,
            currentParams = updatedParams
        )
        audioEngine.updateParameters(updatedParams)
    }

    fun selectPreset(preset: VoicePreset) {
        val newParams = preset.params.copy(isEnabled = _uiState.value.isMasterEnabled)
        _uiState.value = _uiState.value.copy(
            selectedPreset = preset,
            currentParams = newParams
        )
        audioEngine.updateParameters(newParams)
    }

    fun selectCustomPreset(customPreset: CustomPresetItem) {
        val newParams = customPreset.params.copy(isEnabled = _uiState.value.isMasterEnabled)
        _uiState.value = _uiState.value.copy(
            currentParams = newParams
        )
        audioEngine.updateParameters(newParams)
    }

    fun updatePitchSemitones(semitones: Float) {
        val newParams = _uiState.value.currentParams.copy(pitchSemitones = semitones)
        _uiState.value = _uiState.value.copy(currentParams = newParams)
        audioEngine.updateParameters(newParams)
    }

    fun updateFormantShift(shift: Float) {
        val newParams = _uiState.value.currentParams.copy(formantShift = shift)
        _uiState.value = _uiState.value.copy(currentParams = newParams)
        audioEngine.updateParameters(newParams)
    }

    fun updateRobotModulation(robot: Float) {
        val newParams = _uiState.value.currentParams.copy(robotModulation = robot)
        _uiState.value = _uiState.value.copy(currentParams = newParams)
        audioEngine.updateParameters(newParams)
    }

    fun updateReverbWet(reverb: Float) {
        val newParams = _uiState.value.currentParams.copy(reverbWet = reverb)
        _uiState.value = _uiState.value.copy(currentParams = newParams)
        audioEngine.updateParameters(newParams)
    }

    fun updateDistortion(distortion: Float) {
        val newParams = _uiState.value.currentParams.copy(distortion = distortion)
        _uiState.value = _uiState.value.copy(currentParams = newParams)
        audioEngine.updateParameters(newParams)
    }

    fun toggleTelephoneFilter(enabled: Boolean) {
        val newParams = _uiState.value.currentParams.copy(telephoneFilter = enabled)
        _uiState.value = _uiState.value.copy(currentParams = newParams)
        audioEngine.updateParameters(newParams)
    }

    fun updateOutputGain(gain: Float) {
        val newParams = _uiState.value.currentParams.copy(outputGain = gain)
        _uiState.value = _uiState.value.copy(currentParams = newParams)
        audioEngine.updateParameters(newParams)
    }

    fun toggleLoopback() {
        val nextLoopback = !_uiState.value.isLoopbackActive
        _uiState.value = _uiState.value.copy(isLoopbackActive = nextLoopback)
        audioEngine.setLoopbackEnabled(nextLoopback)
    }

    fun toggleSyntheticVoice() {
        val nextSynth = !_uiState.value.isSyntheticVoiceActive
        _uiState.value = _uiState.value.copy(isSyntheticVoiceActive = nextSynth)
        audioEngine.setSyntheticInput(nextSynth)
    }

    fun startTestRecording() {
        audioEngine.startTestRecording()
    }

    fun stopTestRecording() {
        audioEngine.stopTestRecording()
    }

    fun toggleTestPlayback() {
        if (audioMetrics.value.isPlayingTest) {
            audioEngine.stopTestPlayback()
        } else {
            audioEngine.startTestPlayback()
        }
    }

    fun hasTestAudio(): Boolean = audioEngine.hasTestAudio()

    fun resetFineTuning() {
        selectPreset(_uiState.value.selectedPreset)
    }

    fun saveCustomPreset(name: String) {
        if (name.isBlank()) return
        val currentParams = _uiState.value.currentParams
        val custom = CustomPresetItem(
            id = "custom_${System.currentTimeMillis()}",
            name = name.trim(),
            params = currentParams
        )
        _uiState.value = _uiState.value.copy(
            customPresets = _uiState.value.customPresets + custom,
            showCustomPresetDialog = false
        )
    }

    fun setShowCustomPresetDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showCustomPresetDialog = show)
    }

    fun setShowCallDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showCallDialog = show)
    }

    fun setActiveTab(tab: Int) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
    }

    // Call controls delegation
    fun startCall(name: String, channel: String) {
        callManager.startCall(name, channel)
        _uiState.value = _uiState.value.copy(showCallDialog = false)
    }

    fun endCall() {
        callManager.endCall()
    }

    fun toggleCallMute() {
        callManager.toggleMute()
    }

    fun toggleCallSpeaker() {
        callManager.toggleSpeaker()
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
        callManager.endCall()
    }
}
