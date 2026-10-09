package com.example.ui.components

import android.Manifest
import android.content.Context
import android.media.MediaPlayer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.audio.AudioRecorder
import com.example.data.audio.BasicAudioRecorder
import com.example.ui.theme.AmberPending
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSynced
import com.example.ui.theme.TextSecondary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * State representing a recently saved recording in internal storage.
 */
data class SavedRecordingInfo(
    val file: File,
    val durationSeconds: Int,
    val fileSizeBytes: Long,
    val timestampFormatted: String
)

/**
 * A comprehensive, Material 3 Compose UI component that provides controls to start,
 * stop, pause, resume, cancel, and save audio recordings directly to the Android app's
 * internal storage using [MediaRecorder].
 *
 * @param modifier Modifier for styling and positioning.
 * @param audioRecorder Optional custom instance of [AudioRecorder]. If null, defaults to [BasicAudioRecorder].
 * @param initialFileName Optional suggested filename for new recordings.
 * @param onRecordingSaved Callback invoked with the saved [File] in internal storage once stopped and saved.
 * @param onRecordingDiscarded Callback invoked if the recording is canceled.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AudioRecorderComponent(
    modifier: Modifier = Modifier,
    audioRecorder: AudioRecorder? = null,
    initialFileName: String = "",
    onRecordingSaved: ((File) -> Unit)? = null,
    onRecordingDiscarded: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Instantiate or remember recorder instance
    val recorder = remember(audioRecorder, context) {
        audioRecorder ?: BasicAudioRecorder(context)
    }

    // Permission state
    val micPermissionState = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO)

    // UI state
    var isRecording by remember { mutableStateOf(recorder.isRecording) }
    var isPaused by remember { mutableStateOf(recorder.isPaused) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var fileNameInput by remember { mutableStateOf(initialFileName) }
    var liveAmplitudes by remember { mutableStateOf(listOf<Int>()) }
    var latestAmplitude by remember { mutableStateOf(0) }
    var currentFile by remember { mutableStateOf<File?>(null) }
    var lastSavedInfo by remember { mutableStateOf<SavedRecordingInfo?>(null) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    // Preview playback for the last saved file
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Clean up media player when component leaves composition
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    // Internal storage directory
    val internalStorageDir = remember(recorder) {
        recorder.getInternalStorageDir()
    }

    // Active recording timer & amplitude polling loop
    LaunchedEffect(isRecording, isPaused) {
        if (isRecording) {
            while (isRecording) {
                delay(200)
                if (!isPaused) {
                    elapsedSeconds = (elapsedSeconds * 200 + 200) / 200 // smooth accumulation
                    val rawAmp = recorder.getMaxAmplitude()
                    val normalized = if (rawAmp > 0) {
                        ((rawAmp / 32767f) * 85 + 15).toInt().coerceIn(15, 100)
                    } else {
                        (15..45).random()
                    }
                    latestAmplitude = normalized
                    liveAmplitudes = (liveAmplitudes.takeLast(24) + normalized)
                }
            }
        } else {
            latestAmplitude = 0
            liveAmplitudes = emptyList()
        }
    }

    // Timer seconds ticker
    LaunchedEffect(isRecording, isPaused) {
        if (isRecording) {
            while (isRecording) {
                delay(1000)
                if (!isPaused) {
                    elapsedSeconds += 1
                }
            }
        }
    }

    fun startRecordingInternal() {
        if (!micPermissionState.status.isGranted) {
            micPermissionState.launchPermissionRequest()
            return
        }

        val targetName = if (fileNameInput.isNotBlank()) {
            fileNameInput
        } else {
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            "audio_record_$ts"
        }

        saveMessage = null
        elapsedSeconds = 0
        liveAmplitudes = emptyList()

        val file = recorder.startInInternalStorage(targetName)
        if (file != null) {
            currentFile = file
            isRecording = true
            isPaused = false
        }
    }

    fun stopAndSaveInternal() {
        if (!isRecording) return

        val duration = maxOf(elapsedSeconds, 1)
        val file = recorder.stop()
        isRecording = false
        isPaused = false

        if (file != null && file.exists()) {
            val bytes = file.length()
            val nowStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val info = SavedRecordingInfo(
                file = file,
                durationSeconds = duration,
                fileSizeBytes = bytes,
                timestampFormatted = nowStr
            )
            lastSavedInfo = info
            saveMessage = "Enregistré avec succès dans le stockage interne !"
            onRecordingSaved?.invoke(file)
        }
    }

    fun pauseInternal() {
        if (isRecording && !isPaused) {
            recorder.pause()
            isPaused = true
        }
    }

    fun resumeInternal() {
        if (isRecording && isPaused) {
            recorder.resume()
            isPaused = false
        }
    }

    fun cancelInternal() {
        if (isRecording) {
            recorder.cancel()
            isRecording = false
            isPaused = false
            elapsedSeconds = 0
            currentFile = null
            saveMessage = "Enregistrement annulé et supprimé."
            onRecordingDiscarded?.invoke()
        }
    }

    fun togglePreviewPlayback(file: File) {
        if (isPreviewPlaying) {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                isPreviewPlaying = false
            } catch (e: Exception) {
                isPreviewPlaying = false
            }
        } else {
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    prepare()
                    setOnCompletionListener {
                        isPreviewPlaying = false
                    }
                    start()
                }
                isPreviewPlaying = true
            } catch (e: Exception) {
                isPreviewPlaying = false
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_recorder_component"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRecording) CrimsonError.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Microphone",
                            tint = if (isRecording) CrimsonError else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AudioRecorder (MediaRecorder)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Format MPEG-4 / AAC • Stockage Interne",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status Chip
                val statusText = when {
                    isRecording && isPaused -> "EN PAUSE"
                    isRecording -> "EN COURS"
                    else -> "PRÊT"
                }
                val statusColor = when {
                    isRecording && isPaused -> AmberPending
                    isRecording -> CrimsonError
                    else -> EmeraldSynced
                }
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Permission Request Banner if not granted
            if (!micPermissionState.status.isGranted) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
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
                                imageVector = Icons.Filled.MicOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Permission microphone requise pour utiliser MediaRecorder.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { micPermissionState.launchPermissionRequest() },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("recorder_component_perm_btn")
                        ) {
                            Text("Activer", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Storage Target Directory Badge
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Stockage interne : ${internalStorageDir.name}/",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timer & Decibel Amplitude Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatTimerSeconds(elapsedSeconds),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isRecording) CrimsonError else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("recorder_component_timer")
                    )
                    if (isRecording && currentFile != null) {
                        Text(
                            text = currentFile!!.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Decibel / Amplitude level indicator
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GraphicEq,
                            contentDescription = null,
                            tint = if (isRecording) CrimsonError else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRecording) "${latestAmplitude} %" else "0 %",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isRecording) CrimsonError else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Microphone Spectrum Visualizer Bars
            LiveMicrophoneSpectrumVisualizer(
                amplitudes = liveAmplitudes,
                isRecording = isRecording,
                isPaused = isPaused,
                barColor = MaterialTheme.colorScheme.primary,
                peakColor = CrimsonError,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // File Name Input (Visible when idle)
            AnimatedVisibility(
                visible = !isRecording,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OutlinedTextField(
                    value = fileNameInput,
                    onValueChange = { fileNameInput = it },
                    label = { Text("Nom de l'enregistrement (ex: note_vocale)") },
                    placeholder = { Text("Généré automatiquement si vide (.m4a)") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Edit, contentDescription = null)
                    },
                    trailingIcon = {
                        if (fileNameInput.isNotBlank()) {
                            IconButton(onClick = { fileNameInput = "" }) {
                                Icon(imageVector = Icons.Filled.Close, contentDescription = "Effacer")
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recorder_component_filename_input")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- Control Action Buttons (Start, Pause/Resume, Stop & Save, Cancel) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isRecording) {
                    // Big Start Recording Button
                    Button(
                        onClick = { startRecordingInternal() },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("recorder_component_start_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Démarrer l'enregistrement",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Démarrer l'enregistrement",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Pause / Resume Secondary Button
                    FilledTonalIconButton(
                        onClick = {
                            if (isPaused) resumeInternal() else pauseInternal()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("recorder_component_pause_btn")
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (isPaused) "Reprendre" else "Pause",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Stop & Save to Internal Storage Primary Button
                    Button(
                        onClick = { stopAndSaveInternal() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSynced
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("recorder_component_stop_save_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = "Arrêter et sauvegarder",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sauvegarder",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Cancel / Discard Recording Button
                    OutlinedIconButton(
                        onClick = { cancelInternal() },
                        shape = RoundedCornerShape(14.dp),
                        colors = IconButtonDefaults.outlinedIconButtonColors(
                            contentColor = CrimsonError
                        ),
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("recorder_component_cancel_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Annuler l'enregistrement",
                            tint = CrimsonError,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Save confirmation banner
            if (saveMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = EmeraldSynced.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSynced,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = saveMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // --- Last Saved Recording Details Card ---
            lastSavedInfo?.let { saved ->
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("recorder_component_saved_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.AudioFile,
                                    contentDescription = null,
                                    tint = EmeraldSynced,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = saved.file.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Quick Play / Stop Preview button
                            FilledTonalIconButton(
                                onClick = { togglePreviewPlayback(saved.file) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("preview_playback_btn")
                            ) {
                                Icon(
                                    imageVector = if (isPreviewPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPreviewPlaying) "Arrêter écoute" else "Écouter aperçu",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Path in internal storage
                        Text(
                            text = "📁 ${saved.file.absolutePath}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Stats: Duration & File size
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏱ Durée: ${formatTimerSeconds(saved.durationSeconds)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val sizeStr = formatFileSize(saved.fileSizeBytes)
                            Text(
                                text = "💾 Taille: $sizeStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "🕒 ${saved.timestampFormatted}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimerSeconds(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

private fun formatFileSize(bytes: Long): String {
    return if (bytes < 1024) {
        "$bytes B"
    } else if (bytes < 1024 * 1024) {
        String.format("%.1f KB", bytes / 1024.0)
    } else {
        String.format("%.2f MB", bytes / (1024.0 * 1024.0))
    }
}
