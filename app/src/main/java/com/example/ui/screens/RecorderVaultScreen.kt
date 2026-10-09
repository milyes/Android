package com.example.ui.screens

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioRecording
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.data.viewmodel.NavigationTab
import com.example.ui.components.AudioPlayerCard
import com.example.ui.components.AudioRecorderComponent
import com.example.ui.components.DecibelMeterIndicator
import com.example.ui.components.LiveMicrophoneSpectrumVisualizer
import com.example.ui.components.RecordingToggleButton
import com.example.ui.theme.AmberPending
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSynced
import com.example.ui.theme.TextSecondary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RecorderVaultScreen(
    viewModel: AudioSyncViewModel,
    modifier: Modifier = Modifier
) {
    val recordings by viewModel.recordings.collectAsState()
    val recorderState by viewModel.recorderState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val micPermissionState = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO)

    var customNameInput by remember { mutableStateOf("enregistrement_ghost_vocal") }
    var recorderMode by remember { mutableStateOf("COMPONENT") }

    val latestAmplitude = remember(recorderState.liveAmplitudes) {
        recorderState.liveAmplitudes.lastOrNull() ?: 0
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("recorder_vault_screen")
    ) {
        // --- Mode Selector Row ---
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .testTag("recorder_mode_segmented_row")
        ) {
            SegmentedButton(
                selected = recorderMode == "COMPONENT",
                onClick = { recorderMode = "COMPONENT" },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            ) {
                Text("Composant MediaRecorder", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            SegmentedButton(
                selected = recorderMode == "STUDIO",
                onClick = { recorderMode = "STUDIO" },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Radio,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            ) {
                Text("Studio Z-CORE", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (recorderMode == "COMPONENT") {
            AudioRecorderComponent(
                onRecordingSaved = { file ->
                    viewModel.registerRecordedFile(file)
                }
            )
        } else {
        // --- 1. Top Recording Studio Card ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("live_recorder_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Permission Warning Banner if mic permission not granted
                if (!micPermissionState.status.isGranted) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Permission microphone requise pour capturer l'audio.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { micPermissionState.launchPermissionRequest() },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("request_mic_permission_btn")
                            ) {
                                Text("Autoriser", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Studio Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "FLUX WAY - Capture Vocale",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (recorderState.isRecording) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(CrimsonError)
                                )
                            }
                        }
                        Text(
                            text = "Enregistrement local Z-CORE • AAC 44.1 kHz",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Recording Status Badge
                    val (statusText, statusBgColor, statusTextColor) = when {
                        recorderState.isRecording && recorderState.isPaused -> Triple(
                            "EN PAUSE",
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        recorderState.isRecording -> Triple(
                            "RECORDING",
                            MaterialTheme.colorScheme.errorContainer,
                            MaterialTheme.colorScheme.onErrorContainer
                        )
                        else -> Triple(
                            "PRÊT",
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Surface(
                        color = statusBgColor,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("recording_status_indicator")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusTextColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Timer & Decibel Feedback Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimer(recorderState.durationSeconds),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (recorderState.isRecording) CrimsonError else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("recording_timer_display")
                    )

                    DecibelMeterIndicator(
                        amplitude = latestAmplitude,
                        isRecording = recorderState.isRecording
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Visual Sound Spectrum (Microphone Activity Animation)
                LiveMicrophoneSpectrumVisualizer(
                    amplitudes = recorderState.liveAmplitudes,
                    isRecording = recorderState.isRecording,
                    isPaused = recorderState.isPaused,
                    barColor = MaterialTheme.colorScheme.primary,
                    peakColor = CrimsonError,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Recording Name Input Field (when not recording)
                AnimatedVisibility(
                    visible = !recorderState.isRecording,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = customNameInput,
                            onValueChange = { customNameInput = it },
                            label = { Text("Nom du fichier (.m4a / .wav)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recording_name_input"),
                            singleLine = true,
                            leadingIcon = {
                                Icon(imageVector = Icons.Outlined.Edit, contentDescription = null)
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // --- 2. Interactive Start/Stop Toggle Button with Animation ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Left secondary control: Pause/Resume if recording
                        if (recorderState.isRecording) {
                            FilledTonalIconButton(
                                onClick = { viewModel.togglePauseRecording() },
                                modifier = Modifier
                                    .size(52.dp)
                                    .testTag("pause_resume_recording_btn")
                            ) {
                                Icon(
                                    imageVector = if (recorderState.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                    contentDescription = if (recorderState.isPaused) "Reprendre" else "Pause",
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(28.dp))
                        }

                        // Central Prominent Toggle Button
                        RecordingToggleButton(
                            isRecording = recorderState.isRecording,
                            isPaused = recorderState.isPaused,
                            latestAmplitude = latestAmplitude,
                            onToggleRecording = {
                                if (!recorderState.isRecording) {
                                    if (micPermissionState.status.isGranted) {
                                        viewModel.startRecording(customNameInput)
                                    } else {
                                        micPermissionState.launchPermissionRequest()
                                    }
                                } else {
                                    viewModel.stopAndSaveRecording()
                                }
                            }
                        )

                        // Right placeholder / alignment spacer if recording
                        if (recorderState.isRecording) {
                            Spacer(modifier = Modifier.width(28.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = CircleShape,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.GraphicEq,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (recorderState.isRecording) "Appuyez sur Arrêter pour sauvegarder" else "Appuyez pour démarrer l'enregistrement",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- 3. Audio Vault Header & Filter ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bibliothèque Vocale Z-CORE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = { viewModel.setTab(NavigationTab.RECORDINGS_LIST) },
                modifier = Modifier.testTag("view_all_recordings_btn")
            ) {
                Text("Voir la liste (${recordings.size})", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Rechercher (ex: ghost_vocal, 2023, Flux)...") },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Search, contentDescription = "Recherche")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Effacer")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_recordings_input"),
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- 4. Audio Recordings List ---
        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.MicNone,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aucun enregistrement audio",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Appuyez sur le micro ci-dessus pour capturer votre voix.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(recordings, key = { it.id }) { rec ->
                    AudioPlayerCard(
                        recording = rec,
                        playerState = playerState,
                        onPlayPause = { viewModel.togglePlayPause(rec) },
                        onSeek = { seconds -> viewModel.seekTo(seconds, rec) },
                        onSpeedChange = { speed -> viewModel.setPlaybackSpeed(speed) },
                        onSyncClick = { viewModel.runRcloneSync(rec) },
                        onAiAnalyzeClick = {
                            viewModel.analyzeRecordingWithAi(rec)
                            viewModel.setTab(NavigationTab.AI_STUDIO)
                        },
                        onDeleteClick = { viewModel.deleteRecording(rec) }
                    )
                }
            }
        }
    }
}

private fun formatTimer(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
