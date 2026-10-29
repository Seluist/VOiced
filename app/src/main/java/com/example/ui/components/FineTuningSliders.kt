package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.model.VoiceParameters
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.VoidBlack
import kotlin.math.roundToInt

@Composable
fun FineTuningSliders(
    params: VoiceParameters,
    onPitchChange: (Float) -> Unit,
    onFormantChange: (Float) -> Unit,
    onRobotChange: (Float) -> Unit,
    onReverbChange: (Float) -> Unit,
    onDistortionChange: (Float) -> Unit,
    onToggleTelephone: (Boolean) -> Unit,
    onGainChange: (Float) -> Unit,
    onResetDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("fine_tuning_section"),
        color = DarkNavySurface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with Reset Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "FINE-TUNING CONTROLS",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Manual DSP pitch, formant & acoustic filters",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onResetDefaults,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = DarkCardSurface,
                        contentColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_reset_tuning")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reset",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Pitch Shift Slider (-12 to +12 semitones)
            val pitchDisplay = when {
                params.pitchSemitones > 0.1f -> "+${String.format("%.1f", params.pitchSemitones)} st"
                params.pitchSemitones < -0.1f -> "${String.format("%.1f", params.pitchSemitones)} st"
                else -> "0.0 st (Natural)"
            }
            TuningSliderRow(
                title = "Pitch Shift",
                subtitle = "Frequency scale (-1 to +1 octave)",
                valueDisplay = pitchDisplay,
                value = params.pitchSemitones,
                valueRange = -12.0f..12.0f,
                onValueChange = onPitchChange,
                testTag = "slider_pitch"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Formant Adjustment Slider (0.5x to 2.0x)
            val formantDescriptor = when {
                params.formantShift < 0.85f -> "Deep Chest / Masculine"
                params.formantShift > 1.25f -> "Bright Throat / Petite"
                else -> "Natural Vocal Tract"
            }
            TuningSliderRow(
                title = "Formant Shift",
                subtitle = formantDescriptor,
                valueDisplay = "${String.format("%.2f", params.formantShift)}x",
                value = params.formantShift,
                valueRange = 0.5f..2.0f,
                onValueChange = onFormantChange,
                testTag = "slider_formant"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Cyber-Robot Ring Modulation Slider (0% to 100%)
            TuningSliderRow(
                title = "Cyber-Robot Modulation",
                subtitle = "Metallic 65Hz ring carrier oscillator",
                valueDisplay = "${(params.robotModulation * 100).roundToInt()}%",
                value = params.robotModulation,
                valueRange = 0f..1f,
                onValueChange = onRobotChange,
                testTag = "slider_robot"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Spatial / Reverb Wet Slider (0% to 100%)
            TuningSliderRow(
                title = "Spatial Reverb & Room",
                subtitle = "Schroeder diffusion & acoustic space",
                valueDisplay = "${(params.reverbWet * 100).roundToInt()}%",
                value = params.reverbWet,
                valueRange = 0f..1f,
                onValueChange = onReverbChange,
                testTag = "slider_reverb"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Saturation / Distortion Slider
            TuningSliderRow(
                title = "Drive & Saturation",
                subtitle = "Harmonic character & soft clipping",
                valueDisplay = "${(params.distortion * 100).roundToInt()}%",
                value = params.distortion,
                valueRange = 0f..1f,
                onValueChange = onDistortionChange,
                testTag = "slider_distortion"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Telephone Bandpass Filter Switch (300Hz-3400Hz)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkCardSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "1980s Payphone Bandpass",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Cuts <300Hz & >3.4kHz voice telephony standard",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = params.telephoneFilter,
                    onCheckedChange = onToggleTelephone,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VoidBlack,
                        checkedTrackColor = ElectricCyan,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = DarkNavySurface
                    ),
                    modifier = Modifier.testTag("switch_telephone_filter")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7. Output Gain Slider
            TuningSliderRow(
                title = "Output Voice Gain",
                subtitle = "Outgoing volume multiplier",
                valueDisplay = "${String.format("%.1f", params.outputGain)}x",
                value = params.outputGain,
                valueRange = 0.5f..2.0f,
                onValueChange = onGainChange,
                testTag = "slider_output_gain"
            )
        }
    }
}

@Composable
private fun TuningSliderRow(
    title: String,
    subtitle: String,
    valueDisplay: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ElectricCyan.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = valueDisplay,
                    color = CyanGlow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = ElectricCyan,
                activeTrackColor = ElectricCyan,
                inactiveTrackColor = DarkCardSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}
