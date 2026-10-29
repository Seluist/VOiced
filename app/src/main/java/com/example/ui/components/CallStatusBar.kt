package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.model.CallState
import com.example.audio.model.CallStatus
import com.example.ui.theme.CallRed
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LiveEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack
import com.example.ui.theme.WarningAmber

@Composable
fun CallStatusBar(
    callState: CallState,
    voiceChangerActive: Boolean,
    onStartCallClick: () -> Unit,
    onEndCallClick: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("call_status_bar"),
        color = DarkCardSurface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (callState.isCallActive) LiveEmerald.copy(alpha = 0.5f) else DarkBorder
        ),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Status Indicator & Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Pulsing Status Dot
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .scale(if (callState.isCallActive) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(
                                when (callState.status) {
                                    CallStatus.CONNECTED -> LiveEmerald
                                    CallStatus.CONNECTING -> WarningAmber
                                    CallStatus.MUTED -> WarningAmber
                                    CallStatus.IDLE -> TextSecondary.copy(alpha = 0.4f)
                                }
                            )
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (callState.status) {
                                    CallStatus.CONNECTED -> "ACTIVE CALL"
                                    CallStatus.CONNECTING -> "CONNECTING..."
                                    CallStatus.MUTED -> "MIC MUTED"
                                    CallStatus.IDLE -> "VOICE MOD PREVIEW"
                                },
                                color = when (callState.status) {
                                    CallStatus.CONNECTED -> LiveEmerald
                                    CallStatus.CONNECTING -> WarningAmber
                                    CallStatus.MUTED -> WarningAmber
                                    CallStatus.IDLE -> TextSecondary
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )

                            if (callState.isCallActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                val minutes = callState.durationSeconds / 60
                                val seconds = callState.durationSeconds % 60
                                Text(
                                    text = String.format("%02d:%02d", minutes, seconds),
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Text(
                            text = if (callState.isCallActive) callState.partnerName else "Standby • Ready for In-Call Routing",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right: Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (callState.isCallActive) {
                        // Mute button
                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (callState.isOutgoingMuted) WarningAmber.copy(alpha = 0.2f) else DarkBorder.copy(alpha = 0.4f))
                                .testTag("btn_mute_mic")
                        ) {
                            Icon(
                                imageVector = if (callState.isOutgoingMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = if (callState.isOutgoingMuted) "Unmute Mic" else "Mute Mic",
                                tint = if (callState.isOutgoingMuted) WarningAmber else TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // End Call Button
                        IconButton(
                            onClick = onEndCallClick,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(CallRed)
                                .testTag("btn_end_call")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "End Call",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        // Start / Join Call Button
                        FilledTonalButton(
                            onClick = onStartCallClick,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = ElectricCyan.copy(alpha = 0.2f),
                                contentColor = ElectricCyan
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_start_call")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Start Call",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // In-Call Network & Stream Telemetry Bar
            AnimatedVisibility(
                visible = callState.isCallActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DarkBorder)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "RTT ${callState.rttLatencyMs}ms",
                                color = ElectricCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Codec: ${callState.codec}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Voice Changer Outgoing Status Indicator
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (voiceChangerActive) ElectricCyan.copy(alpha = 0.15f)
                                    else TextSecondary.copy(alpha = 0.12f)
                                )
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (voiceChangerActive) "Modulated Voice ➔ Stream" else "Bypass Clean ➔ Stream",
                                color = if (voiceChangerActive) CyanGlow else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
