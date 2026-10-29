package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.dsp.DspAudioProcessor
import com.example.audio.model.VoiceParameters
import com.example.audio.model.VoicePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("VoiceMod Live", appName)
    }

    @Test
    fun `test dsp pitch shifter processing`() {
        val dsp = DspAudioProcessor(sampleRate = 24000)
        val inSamples = FloatArray(512) { i -> kotlin.math.sin(2.0 * Math.PI * 440.0 * i / 24000.0).toFloat() * 0.5f }
        val outSamples = FloatArray(512)

        // Process Deep Baritone
        val params = VoicePreset.DEEP_BARITONE.params
        dsp.process(inSamples, outSamples, 512, params)

        assertTrue("Output peak should be greater than zero", dsp.lastOutputPeak > 0f)
        assertTrue("Output RMS should be greater than zero", dsp.lastOutputRms > 0f)

        val snapshot = dsp.getVisualizerSnapshot()
        assertNotNull(snapshot.waveform)
        assertEquals(64, snapshot.waveform.size)
        assertEquals(16, snapshot.spectrumBands.size)
    }

    @Test
    fun `test voice presets configurations`() {
        val presets = VoicePreset.values()
        assertTrue("Should have multiple voice presets", presets.size >= 5)

        val cyberRobot = VoicePreset.CYBER_ROBOT
        assertTrue("Cyber robot should have robot modulation", cyberRobot.params.robotModulation > 0.5f)

        val helium = VoicePreset.HELIUM
        assertTrue("Helium should have positive pitch shift", helium.params.pitchSemitones > 5f)

        val baritone = VoicePreset.DEEP_BARITONE
        assertTrue("Baritone should have negative pitch shift", baritone.params.pitchSemitones < -2f)
    }
}
