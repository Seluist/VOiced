package com.example.audio.model

/**
 * Voice parameters for real-time DSP audio processing.
 */
data class VoiceParameters(
    val isEnabled: Boolean = true,
    // Pitch shift in semitones: -12.0f (1 octave down) to +12.0f (1 octave up)
    val pitchSemitones: Float = 0f,
    // Formant shift multiplier: 0.5f (deep throat/chest) to 2.0f (child/bright)
    val formantShift: Float = 1.0f,
    // Cyber-Robot ring modulator / metallic carrier blend (0.0 to 1.0)
    val robotModulation: Float = 0f,
    // Spatial / Reverb wet level (0.0 to 1.0)
    val reverbWet: Float = 0.0f,
    // Saturation / Distortion / Drive (0.0 to 1.0)
    val distortion: Float = 0f,
    // Vintage telephone bandpass filter (cuts <300Hz and >3400Hz)
    val telephoneFilter: Boolean = false,
    // Vibrato / alien pitch oscillation rate (Hz) and depth (0.0 to 1.0)
    val vibratoRate: Float = 0f,
    val vibratoDepth: Float = 0f,
    // Master output gain multiplier (0.5 to 2.0)
    val outputGain: Float = 1.0f,
    // Noise gate threshold to suppress background room noise (0.0 to 0.1)
    val noiseGate: Float = 0.015f
) {
    val pitchFactor: Float
        get() = Math.pow(2.0, pitchSemitones.toDouble() / 12.0).toFloat()
}

/**
 * Predefined voice transformation profiles.
 */
enum class VoicePreset(
    val id: String,
    val displayName: String,
    val description: String,
    val category: String,
    val params: VoiceParameters
) {
    CLEAN(
        id = "clean",
        displayName = "Natural Clean",
        description = "Bypass effects, crisp studio audio",
        category = "Original",
        params = VoiceParameters(
            pitchSemitones = 0f,
            formantShift = 1.0f,
            robotModulation = 0f,
            reverbWet = 0.0f,
            distortion = 0f,
            telephoneFilter = false
        )
    ),
    DEEP_BARITONE(
        id = "deep_baritone",
        displayName = "Deep Baritone",
        description = "Resonant low-pitch broadcaster voice",
        category = "Character",
        params = VoiceParameters(
            pitchSemitones = -5.0f,
            formantShift = 0.72f,
            robotModulation = 0f,
            reverbWet = 0.12f,
            distortion = 0.08f,
            telephoneFilter = false
        )
    ),
    CYBER_ROBOT(
        id = "cyber_robot",
        displayName = "Cyber-Robot",
        description = "Metallic ring modulation & synthesized carrier",
        category = "Sci-Fi",
        params = VoiceParameters(
            pitchSemitones = 0f,
            formantShift = 1.15f,
            robotModulation = 0.85f,
            reverbWet = 0.25f,
            distortion = 0.2f,
            telephoneFilter = false
        )
    ),
    RADIO_DJ(
        id = "radio_dj",
        displayName = "Radio DJ",
        description = "Broadcast warm presence with studio compression",
        category = "Broadcast",
        params = VoiceParameters(
            pitchSemitones = -1.2f,
            formantShift = 0.92f,
            robotModulation = 0f,
            reverbWet = 0.18f,
            distortion = 0.15f,
            telephoneFilter = false
        )
    ),
    HELIUM(
        id = "helium",
        displayName = "Helium Balloon",
        description = "High-pitched squeaky chipmunk vocal shift",
        category = "Fun",
        params = VoiceParameters(
            pitchSemitones = 7.5f,
            formantShift = 1.48f,
            robotModulation = 0f,
            reverbWet = 0.05f,
            distortion = 0f,
            telephoneFilter = false
        )
    ),
    ANONYMOUS(
        id = "anonymous",
        displayName = "Anonymous",
        description = "Dark masked identity with low-pass blur",
        category = "Privacy",
        params = VoiceParameters(
            pitchSemitones = -4.2f,
            formantShift = 0.80f,
            robotModulation = 0.15f,
            reverbWet = 0.08f,
            distortion = 0.22f,
            telephoneFilter = false
        )
    ),
    SPACE_ALIEN(
        id = "space_alien",
        displayName = "Space Alien",
        description = "Cosmic pitch vibrato & spatial modulation",
        category = "Sci-Fi",
        params = VoiceParameters(
            pitchSemitones = 3.2f,
            formantShift = 1.30f,
            robotModulation = 0.35f,
            reverbWet = 0.55f,
            vibratoRate = 6.5f,
            vibratoDepth = 0.45f
        )
    ),
    ECHO_CAVE(
        id = "echo_cave",
        displayName = "Echo Cave",
        description = "Vast subterranean chamber reflections",
        category = "Spatial",
        params = VoiceParameters(
            pitchSemitones = -1.0f,
            formantShift = 1.0f,
            robotModulation = 0f,
            reverbWet = 0.75f,
            distortion = 0f
        )
    ),
    TELEPHONE_LOFI(
        id = "telephone_lofi",
        displayName = "1980s Payphone",
        description = "Band-limited 300Hz-3.4kHz analog telephone",
        category = "Retro",
        params = VoiceParameters(
            pitchSemitones = 0f,
            formantShift = 1.1f,
            robotModulation = 0.05f,
            reverbWet = 0.08f,
            distortion = 0.45f,
            telephoneFilter = true
        )
    ),
    TITAN_MONSTER(
        id = "titan_monster",
        displayName = "Titan Monster",
        description = "Earth-shaking colossal deep bass creature",
        category = "Character",
        params = VoiceParameters(
            pitchSemitones = -8.5f,
            formantShift = 0.58f,
            robotModulation = 0.1f,
            reverbWet = 0.42f,
            distortion = 0.35f,
            telephoneFilter = false
        )
    )
}
