package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.model.AudioMetrics
import com.example.audio.model.AudioVisualizerData
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
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.VoidBlack
import com.example.ui.theme.WarningAmber

@Composable
fun WaveformVisualizerView(
    visualizerData: AudioVisualizerData,
    metrics: AudioMetrics,
    isMasterEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("waveform_visualizer_view"),
        color = DarkNavySurface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Visualizer title + Peak DB readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "LIVE AUDIO SPECTRUM & WAVEFORM",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.0.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (visualizerData.isVoiceActive) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(LiveEmerald)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "IN: ${metrics.inputRmsDb.toInt()}dB",
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OUT: ${metrics.outputRmsDb.toInt()}dB",
                        color = if (metrics.isClipping) CallRed else CyanGlow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dual Visualizer Canvas: Oscilloscope Waveform + 16-Band Spectrum Analyzer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(VoidBlack)
                    .border(1.dp, DarkBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val w = size.width
                    val h = size.height

                    // 1. Draw subtle background grid lines
                    val midY = h / 2f
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(0f, midY),
                        end = Offset(w, midY),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color(0xFF151E2E),
                        start = Offset(0f, midY - h * 0.25f),
                        end = Offset(w, midY - h * 0.25f),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color(0xFF151E2E),
                        start = Offset(0f, midY + h * 0.25f),
                        end = Offset(w, midY + h * 0.25f),
                        strokeWidth = 1f
                    )

                    // 2. Draw 16 Spectrum Analyzer Bars at the bottom
                    val spectrum = visualizerData.spectrumBands
                    val numBands = spectrum.size
                    val barSpacing = 4f
                    val totalSpacing = (numBands - 1) * barSpacing
                    val barWidth = (w - totalSpacing - 16f) / numBands
                    val startX = 8f

                    val barBrush = Brush.verticalGradient(
                        colors = listOf(
                            if (isMasterEnabled) NeonViolet else TextSecondary,
                            if (isMasterEnabled) ElectricCyan else TextTertiary
                        ),
                        startY = h * 0.3f,
                        endY = h
                    )

                    for (b in 0 until numBands) {
                        val bandEnergy = spectrum[b].coerceIn(0.04f, 1.0f)
                        val barHeight = bandEnergy * (h * 0.55f)
                        val bx = startX + b * (barWidth + barSpacing)
                        val by = h - barHeight

                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(bx, by),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(2f, 2f),
                            alpha = if (isMasterEnabled) 0.65f else 0.3f
                        )
                    }

                    // 3. Draw Oscilloscope Waveform Path across canvas
                    val wave = visualizerData.waveform
                    if (wave.isNotEmpty()) {
                        val path = Path()
                        val stepX = w / (wave.size - 1).coerceAtLeast(1)

                        val waveBrush = Brush.horizontalGradient(
                            colors = if (isMasterEnabled) {
                                listOf(ElectricCyan, CyanGlow, NeonViolet, ElectricCyan)
                            } else {
                                listOf(TextSecondary, TextPrimary, TextSecondary)
                            }
                        )

                        // Path start
                        val startY = midY - wave[0] * (h * 0.42f)
                        path.moveTo(0f, startY)

                        for (i in 1 until wave.size) {
                            val x = i * stepX
                            val y = midY - wave[i] * (h * 0.42f)
                            path.lineTo(x, y)
                        }

                        // Draw glowing stroke
                        drawPath(
                            path = path,
                            brush = waveBrush,
                            style = Stroke(
                                width = 2.5f,
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Peak Meters (Stereo-style In / Out dB volume bars)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MIC IN",
                    color = TextTertiary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp)
                )
                LevelMeterBar(
                    peak = metrics.inputPeak,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "DSP OUT",
                    color = TextTertiary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(42.dp)
                )
                LevelMeterBar(
                    peak = metrics.outputPeak,
                    isOutput = true,
                    isClipping = metrics.isClipping,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LevelMeterBar(
    peak: Float,
    modifier: Modifier = Modifier,
    isOutput: Boolean = false,
    isClipping: Boolean = false
) {
    val level = peak.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(DarkBorder.copy(alpha = 0.5f))
    ) {
        val meterBrush = Brush.horizontalGradient(
            colors = listOf(
                LiveEmerald,
                WarningAmber,
                if (isClipping) CallRed else ElectricCyan
            )
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(level)
                .clip(RoundedCornerShape(4.dp))
                .background(meterBrush)
        )
    }
}
