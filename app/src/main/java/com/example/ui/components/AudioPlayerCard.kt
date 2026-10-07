package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioRecording
import com.example.data.viewmodel.PlayerState
import com.example.ui.theme.EmeraldSynced
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerCard(
    recording: AudioRecording,
    playerState: PlayerState,
    onPlayPause: () -> Unit,
    onSeek: (Int) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSyncClick: () -> Unit,
    onAiAnalyzeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCurrent = playerState.playingRecordingId == recording.id
    val isPlaying = isCurrent && playerState.isPlaying
    val currentPos = if (isCurrent) playerState.currentPositionSeconds else 0
    val progressRatio = if (recording.durationSeconds > 0) currentPos.toFloat() / recording.durationSeconds else 0f

    var showDetails by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_player_card_${recording.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Header: Title & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${recording.fileName} • ${recording.fileSizeMb} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Cloud Status Badge
                StatusBadge(recording.syncStatus)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Waveform Visualizer
            val amps = remember(recording.waveformData) {
                recording.waveformData.split(",").mapNotNull { it.trim().toIntOrNull() }
            }
            AudioWaveformVisualizer(
                amplitudes = amps,
                progressRatio = progressRatio,
                isLive = false,
                barColor = MaterialTheme.colorScheme.primary,
                activeBarColor = MaterialTheme.colorScheme.secondary,
                inactiveBarColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Time & Seek Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(currentPos),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )

                Slider(
                    value = currentPos.toFloat(),
                    onValueChange = { onSeek(it.toInt()) },
                    valueRange = 0f..recording.durationSeconds.toFloat(),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .testTag("audio_slider_${recording.id}"),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Text(
                    text = formatTime(recording.durationSeconds),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause Button
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("play_button_${recording.id}")
                        .testTag("play_pause_button_${recording.id}")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // Speed Selector Pill
                AssistChip(
                    onClick = {
                        val nextSpeed = when (playerState.playbackSpeed) {
                            1.0f -> 1.25f
                            1.25f -> 1.5f
                            1.5f -> 2.0f
                            else -> 1.0f
                        }
                        onSpeedChange(nextSpeed)
                    },
                    label = {
                        Text(
                            text = "${playerState.playbackSpeed}x",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Speed,
                            contentDescription = "Playback Speed",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                // RClone Cloud Sync Button
                FilledTonalIconButton(
                    onClick = onSyncClick,
                    modifier = Modifier
                        .testTag("sync_button_${recording.id}")
                        .testTag("sync_file_button_${recording.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudUpload,
                        contentDescription = "Synchroniser avec rclone"
                    )
                }

                // Direct Delete Button
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .testTag("delete_button_${recording.id}")
                        .testTag("delete_file_button_${recording.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                // Gemini AI Studio Button
                IconButton(
                    onClick = onAiAnalyzeClick,
                    modifier = Modifier.testTag("ai_button_${recording.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Psychology,
                        contentDescription = "Analyse Gemini AI",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                // Expand Details / Info
                IconButton(
                    onClick = { showDetails = !showDetails }
                ) {
                    Icon(
                        imageVector = if (showDetails) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Détails"
                    )
                }
            }

            // Expanded Metadata & AI Insights
            AnimatedVisibility(visible = showDetails) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = "FLUX: ${recording.sourceStream}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "LOCAL: ${recording.localPath}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                    Text(
                        text = "CLOUD: ${recording.cloudPath}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )

                    recording.transcription?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Transcription / Notes:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    recording.aiSummary?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Synthèse Gemini IA:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDeleteClick,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Supprimer",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Supprimer", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (text, bgColor, textColor) = when (status) {
        "CLOUD_SYNCED" -> Triple("GOOGLE DRIVE SYNCED", EmeraldSynced.copy(alpha = 0.2f), EmeraldSynced)
        "SYNCING" -> Triple("SYNC EN COURS...", MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), MaterialTheme.colorScheme.primary)
        "ERROR" -> Triple("ERREUR JETON", MaterialTheme.colorScheme.error.copy(alpha = 0.2f), MaterialTheme.colorScheme.error)
        else -> Triple("Z-CORE LOCAL", MaterialTheme.colorScheme.surfaceVariant, TextSecondary)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}
