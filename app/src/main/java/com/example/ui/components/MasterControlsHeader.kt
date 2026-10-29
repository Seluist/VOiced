package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.model.AudioMetrics
import com.example.audio.model.VoicePreset
import com.example.ui.theme.CallRed
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LiveEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletGlow
import com.example.ui.theme.VoidBlack
import com.example.ui.theme.WarningAmber

@Composable
fun MasterControlsHeader(
    isMasterEnabled: Boolean,
    onToggleMaster: (Boolean) -> Unit,
    selectedPreset: VoicePreset,
    isLoopbackActive: Boolean,
    onToggleLoopback: () -> Unit,
    isSyntheticVoiceActive: Boolean,
    onToggleSyntheticVoice: () -> Unit,
    metrics: AudioMetrics,
    onStartTestRecording: () -> Unit,
    onStopTestRecording: () -> Unit,
    onToggleTestPlayback: () -> Unit,
    hasTestAudio: Boolean,
    modifier: Modifier = Modifier
) {
    val switchBorderColor by animateColorAsState(
        targetValue = if (isMasterEnabled) ElectricCyan else DarkBorder,
        animationSpec = tween(300),
        label = "switchBorder"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("master_controls_header"),
        color = DarkNavySurface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, switchBorderColor),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Big Master Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (isMasterEnabled) Brush.linearGradient(listOf(ElectricCyan, NeonViolet))
                                else Brush.linearGradient(listOf(DarkBorder, DarkCardSurface))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Voice Changer Power",
                            tint = if (isMasterEnabled) VoidBlack else TextSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "VOICE CHANGER",
                                color = if (isMasterEnabled) CyanGlow else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Latency Badge (<100ms requirement)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(LiveEmerald.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "⚡ ${metrics.latencyMs}ms Low-Latency",
                                    color = LiveEmerald,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = if (isMasterEnabled) "Active: ${selectedPreset.displayName}" else "Bypass (Clean Microphone)",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Tactile Master Switch
                Switch(
                    checked = isMasterEnabled,
                    onCheckedChange = onToggleMaster,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VoidBlack,
                        checkedTrackColor = ElectricCyan,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = DarkCardSurface
                    ),
                    modifier = Modifier.testTag("master_power_switch")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DarkBorder.copy(alpha = 0.6f))
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Lower Row: Loopback "Test Voice" + Test Recorder + Input Source
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. "Test Voice" Loopback Button
                FilledTonalButton(
                    onClick = onToggleLoopback,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isLoopbackActive) NeonViolet else DarkCardSurface,
                        contentColor = if (isLoopbackActive) Color.White else TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_test_voice_loopback")
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isLoopbackActive) Color.White else CyanGlow
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLoopbackActive) "Loopback: ON" else "Test Voice",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // 2. Quick Test Record & Replay
                FilledTonalButton(
                    onClick = {
                        if (metrics.isRecordingTest) {
                            onStopTestRecording()
                        } else if (hasTestAudio) {
                            onToggleTestPlayback()
                        } else {
                            onStartTestRecording()
                        }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = when {
                            metrics.isRecordingTest -> CallRed
                            metrics.isPlayingTest -> LiveEmerald
                            hasTestAudio -> DarkCardSurface
                            else -> DarkCardSurface
                        },
                        contentColor = when {
                            metrics.isRecordingTest || metrics.isPlayingTest -> Color.White
                            else -> TextPrimary
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("btn_quick_test_recorder")
                ) {
                    Icon(
                        imageVector = when {
                            metrics.isRecordingTest -> Icons.Default.Stop
                            metrics.isPlayingTest -> Icons.Default.Stop
                            hasTestAudio -> Icons.Default.PlayArrow
                            else -> Icons.Default.FiberManualRecord
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = when {
                            metrics.isRecordingTest -> Color.White
                            metrics.isPlayingTest -> Color.White
                            hasTestAudio -> LiveEmerald
                            else -> CallRed
                        }
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = when {
                            metrics.isRecordingTest -> "Recording..."
                            metrics.isPlayingTest -> "Playing Preview"
                            hasTestAudio -> "Play Test Clip"
                            else -> "Record 5s Test"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // 3. Synthetic Mic Input vs Physical Mic Toggle (Great for emulator testing)
                FilledTonalButton(
                    onClick = onToggleSyntheticVoice,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isSyntheticVoiceActive) WarningAmber.copy(alpha = 0.25f) else DarkCardSurface,
                        contentColor = if (isSyntheticVoiceActive) WarningAmber else TextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_toggle_synthetic_voice")
                ) {
                    Icon(
                        imageVector = if (isSyntheticVoiceActive) Icons.Default.RecordVoiceOver else Icons.Default.Mic,
                        contentDescription = "Toggle Speech Synthesizer",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSyntheticVoiceActive) "Synth Mic" else "Live Mic",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Headphone feedback guidance hint when loopback is active
            if (isLoopbackActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonViolet.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = VioletGlow,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Headphones recommended for Test Voice loopback to prevent speaker feedback.",
                        color = VioletGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}
