package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Masks
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.model.VoicePreset
import com.example.ui.CustomPresetItem
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
import com.example.ui.theme.VioletGlow
import com.example.ui.theme.VoidBlack

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PresetGrid(
    selectedPreset: VoicePreset,
    onSelectPreset: (VoicePreset) -> Unit,
    customPresets: List<CustomPresetItem>,
    onSelectCustomPreset: (CustomPresetItem) -> Unit,
    onAddCustomPresetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("preset_grid_section")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "VOICE PRESETS",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Instant low-latency vocal profiles",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            FilledTonalButton(
                onClick = onAddCustomPresetClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = DarkCardSurface,
                    contentColor = ElectricCyan
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_save_custom_preset")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Save Custom",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Custom Presets row if any exist
        if (customPresets.isNotEmpty()) {
            Text(
                text = "YOUR CUSTOM PRESETS",
                color = ElectricCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                customPresets.forEach { custom ->
                    CustomPresetChip(
                        item = custom,
                        onClick = { onSelectCustomPreset(custom) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Standard Presets 2-Column Grid
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val presets = VoicePreset.values()
            val chunked = presets.toList().chunked(2)
            chunked.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { preset ->
                        PresetCard(
                            preset = preset,
                            isSelected = selectedPreset == preset,
                            onClick = { onSelectPreset(preset) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetCard(
    preset: VoicePreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) ElectricCyan else DarkBorder,
        label = "presetBorder"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("preset_card_${preset.id}"),
        color = if (isSelected) DarkCardSurface else DarkNavySurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        tonalElevation = if (isSelected) 6.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Preset Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) ElectricCyan.copy(alpha = 0.2f)
                            else DarkCardSurface
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getPresetIcon(preset),
                        contentDescription = null,
                        tint = if (isSelected) ElectricCyan else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Selected Check or Category Tag
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = VoidBlack,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkBorder.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = preset.category,
                            color = TextTertiary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = preset.displayName,
                color = if (isSelected) CyanGlow else TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = preset.description,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun CustomPresetChip(
    item: CustomPresetItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag("custom_preset_${item.id}"),
        color = DarkCardSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = VioletGlow,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = item.name,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun getPresetIcon(preset: VoicePreset): ImageVector {
    return when (preset) {
        VoicePreset.CLEAN -> Icons.Default.Mic
        VoicePreset.DEEP_BARITONE -> Icons.Default.GraphicEq
        VoicePreset.CYBER_ROBOT -> Icons.Default.SmartToy
        VoicePreset.RADIO_DJ -> Icons.Default.Campaign
        VoicePreset.HELIUM -> Icons.Default.Air
        VoicePreset.ANONYMOUS -> Icons.Default.Masks
        VoicePreset.SPACE_ALIEN -> Icons.Default.RocketLaunch
        VoicePreset.ECHO_CAVE -> Icons.Default.Landscape
        VoicePreset.TELEPHONE_LOFI -> Icons.Default.Phone
        VoicePreset.TITAN_MONSTER -> Icons.Default.Psychology
    }
}
