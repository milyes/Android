package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioRecording
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.data.viewmodel.NavigationTab
import com.example.data.viewmodel.PlayerState
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.theme.AmberPending
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSynced
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.*

enum class AudioFilter {
    ALL,
    SYNCED,
    LOCAL_ONLY,
    SYNCING
}

enum class AudioSort {
    NEWEST,
    OLDEST,
    DURATION,
    SIZE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsListScreen(
    viewModel: AudioSyncViewModel,
    modifier: Modifier = Modifier
) {
    // Handle back button: return to recorder tab
    BackHandler {
        viewModel.setTab(NavigationTab.RECORDER_VAULT)
    }

    val rawRecordings by viewModel.recordings.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val isGlobalSyncing by viewModel.isSyncingActive.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(AudioFilter.ALL) }
    var selectedSort by remember { mutableStateOf(AudioSort.NEWEST) }
    var recordingToDelete by remember { mutableStateOf<AudioRecording?>(null) }

    // Filter and sort the recordings list
    val displayedRecordings = remember(rawRecordings, searchQuery, selectedFilter, selectedSort) {
        var list = rawRecordings

        // 1. Search Query Filter
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            list = list.filter {
                it.title.lowercase(Locale.ROOT).contains(q) ||
                it.fileName.lowercase(Locale.ROOT).contains(q) ||
                (it.transcription?.lowercase(Locale.ROOT)?.contains(q) == true)
            }
        }

        // 2. Status Filter
        list = when (selectedFilter) {
            AudioFilter.ALL -> list
            AudioFilter.SYNCED -> list.filter { it.isSynced || it.syncStatus == "CLOUD_SYNCED" }
            AudioFilter.LOCAL_ONLY -> list.filter { !it.isSynced && it.syncStatus != "SYNCING" }
            AudioFilter.SYNCING -> list.filter { it.syncStatus == "SYNCING" }
        }

        // 3. Sorting
        when (selectedSort) {
            AudioSort.NEWEST -> list.sortedByDescending { it.timestamp }
            AudioSort.OLDEST -> list.sortedBy { it.timestamp }
            AudioSort.DURATION -> list.sortedByDescending { it.durationSeconds }
            AudioSort.SIZE -> list.sortedByDescending { it.fileSizeMb }
        }
    }

    // Aggregated stats
    val totalCount = rawRecordings.size
    val totalSizeMb = rawRecordings.sumOf { it.fileSizeMb.toDouble() }
    val syncedCount = rawRecordings.count { it.isSynced || it.syncStatus == "CLOUD_SYNCED" }
    val localCount = totalCount - syncedCount

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.setTab(NavigationTab.RECORDER_VAULT) },
                icon = { Icon(Icons.Filled.Mic, contentDescription = "Enregistrer") },
                text = { Text("Enregistrer") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_new_recording")
            )
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("recordings_list_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // --- 1. Overview Statistics & Quick Sync All Bar ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recordings_stats_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Fichiers Audio Enregistrés",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Stockage local Z-CORE & Synchronisation Google Drive",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        // Action Buttons: LANCE_BIN.HTML & Batch Sync
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.setTab(NavigationTab.CLOUD_SYNC) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("top_lance_bin_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Code,
                                    contentDescription = "LANCE_BIN.HTML",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "LANCE_BIN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (localCount > 0) {
                                FilledTonalButton(
                                    onClick = { viewModel.batchSyncAllPending() },
                                    enabled = !isGlobalSyncing,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("batch_sync_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CloudUpload,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isGlobalSyncing) "Syncing..." else "Sync ($localCount)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBadge(
                            label = "Total",
                            value = "$totalCount (${String.format(Locale.ROOT, "%.1f", totalSizeMb)} MB)",
                            icon = Icons.Outlined.AudioFile,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatBadge(
                            label = "Sur Drive",
                            value = "$syncedCount synchronisé(s)",
                            icon = Icons.Outlined.CloudDone,
                            color = EmeraldSynced,
                            modifier = Modifier.weight(1f)
                        )
                        StatBadge(
                            label = "En Local",
                            value = "$localCount en attente",
                            icon = Icons.Outlined.CloudQueue,
                            color = AmberPending,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- 2. Search & Filter Bar ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher par nom, format, transcription...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = "Recherche")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Effacer recherche")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_recordings_input"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedFilter == AudioFilter.ALL,
                    onClick = { selectedFilter = AudioFilter.ALL },
                    label = { Text("Tous ($totalCount)", fontSize = 11.sp) },
                    modifier = Modifier.testTag("filter_all")
                )
                FilterChip(
                    selected = selectedFilter == AudioFilter.SYNCED,
                    onClick = { selectedFilter = AudioFilter.SYNCED },
                    label = { Text("Drive ($syncedCount)", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.CloudDone,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = EmeraldSynced
                        )
                    },
                    modifier = Modifier.testTag("filter_synced")
                )
                FilterChip(
                    selected = selectedFilter == AudioFilter.LOCAL_ONLY,
                    onClick = { selectedFilter = AudioFilter.LOCAL_ONLY },
                    label = { Text("Local ($localCount)", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = AmberPending
                        )
                    },
                    modifier = Modifier.testTag("filter_local")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sort & Result Count Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${displayedRecordings.size} enregistrement(s) affiché(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Trier:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = {
                            selectedSort = when (selectedSort) {
                                AudioSort.NEWEST -> AudioSort.OLDEST
                                AudioSort.OLDEST -> AudioSort.DURATION
                                AudioSort.DURATION -> AudioSort.SIZE
                                AudioSort.SIZE -> AudioSort.NEWEST
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        val sortLabel = when (selectedSort) {
                            AudioSort.NEWEST -> "Plus récent"
                            AudioSort.OLDEST -> "Plus ancien"
                            AudioSort.DURATION -> "Durée"
                            AudioSort.SIZE -> "Taille"
                        }
                        Text(
                            text = sortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Filled.Sort,
                            contentDescription = "Trier",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- 3. Audio Files List View ---
            if (displayedRecordings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("recordings_empty_state"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.AudioFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (searchQuery.isNotBlank()) "Aucun résultat trouvé" else "Aucun enregistrement audio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (searchQuery.isNotBlank())
                                "Essayez un autre mot-clé ou effacez le filtre."
                            else
                                "Utilisez le studio d'enregistrement pour capturer vos fichiers audio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.setTab(NavigationTab.RECORDER_VAULT) },
                            modifier = Modifier.testTag("empty_state_record_btn")
                        ) {
                            Icon(imageVector = Icons.Filled.Mic, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Démarrer un enregistrement")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("audio_recordings_list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(displayedRecordings, key = { it.id }) { rec ->
                        RecordingFileListItem(
                            recording = rec,
                            playerState = playerState,
                            onPlayPause = { viewModel.togglePlayPause(rec) },
                            onSeek = { seconds -> viewModel.seekTo(seconds, rec) },
                            onSpeedChange = { speed -> viewModel.setPlaybackSpeed(speed) },
                            onManualSync = { viewModel.runRcloneSync(rec) },
                            onDelete = { recordingToDelete = rec },
                            onAiAnalyze = {
                                viewModel.analyzeRecordingWithAi(rec)
                                viewModel.setTab(NavigationTab.AI_STUDIO)
                            }
                        )
                    }
                }
            }
        }
    }

    // --- 4. Delete Confirmation Dialog ---
    recordingToDelete?.let { rec ->
        AlertDialog(
            onDismissRequest = { recordingToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text("Supprimer l'enregistrement ?")
            },
            text = {
                Text(
                    text = "Voulez-vous supprimer définitivement '${rec.fileName}' ? Ce fichier sera supprimé du stockage local et de la base de données.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecording(rec)
                        recordingToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_btn")
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { recordingToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_btn")
                ) {
                    Text("Annuler")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingFileListItem(
    recording: AudioRecording,
    playerState: PlayerState,
    onPlayPause: () -> Unit,
    onSeek: (Int) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onManualSync: () -> Unit,
    onDelete: () -> Unit,
    onAiAnalyze: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCurrent = playerState.playingRecordingId == recording.id
    val isPlaying = isCurrent && playerState.isPlaying
    val currentPos = if (isCurrent) playerState.currentPositionSeconds else 0
    val progressRatio = if (recording.durationSeconds > 0) currentPos.toFloat() / recording.durationSeconds else 0f
    val isSyncing = recording.syncStatus == "SYNCING"

    var showDetails by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recording_item_${recording.id}"),
        shape = RoundedCornerShape(18.dp),
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
            // Header Row: Audio File Meta & Sync Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.GraphicEq else Icons.Filled.Audiotrack,
                                contentDescription = null,
                                tint = if (isPlaying) MaterialTheme.colorScheme.primary else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = recording.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // File format tag
                            val ext = recording.fileName.substringAfterLast('.', "M4A").uppercase(Locale.ROOT)
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = ext,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${recording.fileSizeMb} MB • ${formatDate(recording.timestamp)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Cloud Sync Status Badge
                FileStatusBadge(
                    status = recording.syncStatus,
                    modifier = Modifier.testTag("status_badge_${recording.id}")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Waveform Amplitude Visualizer
            val amps = remember(recording.waveformData) {
                recording.waveformData.split(",").mapNotNull { it.trim().toIntOrNull() }
            }
            AudioWaveformVisualizer(
                amplitudes = amps,
                progressRatio = progressRatio,
                isLive = false,
                barColor = MaterialTheme.colorScheme.primary,
                activeBarColor = MaterialTheme.colorScheme.secondary,
                inactiveBarColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Audio Time & Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDuration(currentPos),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Slider(
                    value = currentPos.toFloat(),
                    onValueChange = { onSeek(it.toInt()) },
                    valueRange = 0f..recording.durationSeconds.toFloat().coerceAtLeast(1f),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .testTag("seek_slider_${recording.id}"),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Text(
                    text = formatDuration(recording.durationSeconds),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Primary Action Buttons Row: PLAY, MANUAL SYNC, DELETE ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. PLAY / PAUSE BUTTON (Direct, accessible)
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
                        contentDescription = if (isPlaying) "Mettre en pause" else "Lire l'audio",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // 2. Playback Speed Selector
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
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.height(32.dp)
                )

                // 3. MANUALLY TRIGGER SYNC BUTTON
                FilledTonalButton(
                    onClick = onManualSync,
                    enabled = !isSyncing,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .testTag("sync_button_${recording.id}")
                        .testTag("sync_file_button_${recording.id}")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Syncing...", fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = if (recording.isSynced) Icons.Filled.CloudDone else Icons.Outlined.CloudUpload,
                            contentDescription = "Synchroniser avec Google Drive",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (recording.isSynced) "Re-Sync" else "Sync Drive",
                            fontSize = 12.sp
                        )
                    }
                }

                // 4. DELETE BUTTON (Direct, accessible)
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .testTag("delete_button_${recording.id}")
                        .testTag("delete_file_button_${recording.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Supprimer l'enregistrement",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                // 5. Expand Details Toggle
                IconButton(
                    onClick = { showDetails = !showDetails }
                ) {
                    Icon(
                        imageVector = if (showDetails) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Afficher les détails"
                    )
                }
            }

            // Expanded Details Section
            AnimatedVisibility(visible = showDetails) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Flux source: ${recording.sourceStream}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Local: ${recording.localPath}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Cloud: ${recording.cloudPath}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    recording.transcription?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Transcription:",
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
                        Spacer(modifier = Modifier.height(8.dp))
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

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = onAiAnalyze,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("ai_analyze_btn_${recording.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Analyser avec Gemini", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBadge(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun FileStatusBadge(status: String, modifier: Modifier = Modifier) {
    val (text, bgColor, textColor, icon) = when (status) {
        "CLOUD_SYNCED" -> Quadruple("DRIVE SYNCED", EmeraldSynced.copy(alpha = 0.2f), EmeraldSynced, Icons.Filled.Check)
        "SYNCING" -> Quadruple("SYNC EN COURS", MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), MaterialTheme.colorScheme.primary, Icons.Filled.Sync)
        "ERROR" -> Quadruple("ERREUR", MaterialTheme.colorScheme.error.copy(alpha = 0.2f), MaterialTheme.colorScheme.error, Icons.Filled.Warning)
        else -> Quadruple("LOCAL", AmberPending.copy(alpha = 0.2f), AmberPending, Icons.Filled.CloudUpload)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 10.sp
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.ROOT, "%02d:%02d", mins, secs)
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
