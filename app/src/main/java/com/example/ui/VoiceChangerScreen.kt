package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.CallStatusBar
import com.example.ui.components.FineTuningSliders
import com.example.ui.components.MasterControlsHeader
import com.example.ui.components.PresetGrid
import com.example.ui.components.SaveCustomPresetDialog
import com.example.ui.components.StartCallDialog
import com.example.ui.components.WaveformVisualizerView
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceChangerScreen(
    viewModel: VoiceChangerViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val visualizerData by viewModel.visualizerData.collectAsState()
    val metrics by viewModel.audioMetrics.collectAsState()
    val callState by viewModel.callState.collectAsState()

    // Permission state handling for RECORD_AUDIO
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (!isGranted) {
            // Automatically switch to synthetic speech generator so user still has full audio experience!
            viewModel.toggleSyntheticVoice()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("voice_changer_screen"),
        containerColor = VoidBlack,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(ElectricCyan, NeonViolet))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = VoidBlack,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "VOICEMOD",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.0.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ElectricCyan)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "LIVE",
                                        color = VoidBlack,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Text(
                                text = "Real-Time In-Call Voice DSP Pipeline",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    // Call Shortcut Button
                    IconButton(
                        onClick = { viewModel.setShowCallDialog(true) },
                        modifier = Modifier.testTag("top_btn_call")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Open Call Stream",
                            tint = if (callState.isCallActive) LiveEmerald else TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VoidBlack
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Permission Warning Banner if mic not granted
            if (!hasMicPermission) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = WarningAmber.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Microphone Permission Required",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Using Synthetic Voice Generator. Grant mic access for live speech capture.",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WarningAmber,
                                    contentColor = VoidBlack
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. Active Call Status Bar (Active call duration, peer avatar, latency, outgoing stream)
            item {
                CallStatusBar(
                    callState = callState,
                    voiceChangerActive = uiState.isMasterEnabled,
                    onStartCallClick = { viewModel.setShowCallDialog(true) },
                    onEndCallClick = { viewModel.endCall() },
                    onToggleMute = { viewModel.toggleCallMute() },
                    onToggleSpeaker = { viewModel.toggleCallSpeaker() }
                )
            }

            // 3. Master On/Off Toggle & Test Voice Loopback Header
            item {
                MasterControlsHeader(
                    isMasterEnabled = uiState.isMasterEnabled,
                    onToggleMaster = { viewModel.toggleMasterVoiceChanger(it) },
                    selectedPreset = uiState.selectedPreset,
                    isLoopbackActive = uiState.isLoopbackActive,
                    onToggleLoopback = { viewModel.toggleLoopback() },
                    isSyntheticVoiceActive = uiState.isSyntheticVoiceActive,
                    onToggleSyntheticVoice = { viewModel.toggleSyntheticVoice() },
                    metrics = metrics,
                    onStartTestRecording = { viewModel.startTestRecording() },
                    onStopTestRecording = { viewModel.stopTestRecording() },
                    onToggleTestPlayback = { viewModel.toggleTestPlayback() },
                    hasTestAudio = viewModel.hasTestAudio()
                )
            }

            // 4. Live Audio Waveform & Frequency Spectrum Visualizer
            item {
                WaveformVisualizerView(
                    visualizerData = visualizerData,
                    metrics = metrics,
                    isMasterEnabled = uiState.isMasterEnabled
                )
            }

            // 5. Navigation Tabs (Presets vs Fine-Tune Studio)
            item {
                TabRow(
                    selectedTabIndex = uiState.activeTab,
                    containerColor = DarkNavySurface,
                    contentColor = ElectricCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[uiState.activeTab]),
                            color = ElectricCyan,
                            height = 3.dp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .testTag("section_tabs")
                ) {
                    Tab(
                        selected = uiState.activeTab == 0,
                        onClick = { viewModel.setActiveTab(0) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Presets",
                                    fontWeight = if (uiState.activeTab == 0) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        },
                        selectedContentColor = ElectricCyan,
                        unselectedContentColor = TextSecondary
                    )
                    Tab(
                        selected = uiState.activeTab == 1,
                        onClick = { viewModel.setActiveTab(1) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Fine-Tuning",
                                    fontWeight = if (uiState.activeTab == 1) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        },
                        selectedContentColor = ElectricCyan,
                        unselectedContentColor = TextSecondary
                    )
                }
            }

            // 6. Tab Content: Presets Grid or Fine-Tuning Sliders
            if (uiState.activeTab == 0) {
                item {
                    PresetGrid(
                        selectedPreset = uiState.selectedPreset,
                        onSelectPreset = { viewModel.selectPreset(it) },
                        customPresets = uiState.customPresets,
                        onSelectCustomPreset = { viewModel.selectCustomPreset(it) },
                        onAddCustomPresetClick = { viewModel.setShowCustomPresetDialog(true) }
                    )
                }
            } else {
                item {
                    FineTuningSliders(
                        params = uiState.currentParams,
                        onPitchChange = { viewModel.updatePitchSemitones(it) },
                        onFormantChange = { viewModel.updateFormantShift(it) },
                        onRobotChange = { viewModel.updateRobotModulation(it) },
                        onReverbChange = { viewModel.updateReverbWet(it) },
                        onDistortionChange = { viewModel.updateDistortion(it) },
                        onToggleTelephone = { viewModel.toggleTelephoneFilter(it) },
                        onGainChange = { viewModel.updateOutputGain(it) },
                        onResetDefaults = { viewModel.resetFineTuning() }
                    )
                }
            }
        }
    }

    // Custom Preset Dialog
    if (uiState.showCustomPresetDialog) {
        SaveCustomPresetDialog(
            onDismiss = { viewModel.setShowCustomPresetDialog(false) },
            onSave = { name -> viewModel.saveCustomPreset(name) }
        )
    }

    // Call Picker Dialog
    if (uiState.showCallDialog) {
        StartCallDialog(
            onDismiss = { viewModel.setShowCallDialog(false) },
            onStartCall = { name, channel -> viewModel.startCall(name, channel) }
        )
    }
}
