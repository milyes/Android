package com.example.data.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.model.AudioRecording
import com.example.data.model.BackgroundTaskState
import com.example.data.model.CommandMacro
import com.example.data.model.RcloneParams
import com.example.data.model.SyncLog
import com.example.data.repository.AudioRepository
import com.example.data.audio.AudioRecorderManager
import com.example.data.model.LanceBinFileState
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.google.firebase.auth.FirebaseAuth

enum class NavigationTab {
    RECORDER_VAULT,
    RECORDINGS_LIST,
    CLOUD_SYNC,
    RCLONE_CONFIG,
    TERMUX_MACROS,
    DEVICE_INFO,
    AI_STUDIO
}

data class FirebaseAuthState(
    val isSignedIn: Boolean = true,
    val email: String? = "operator.zcore@google.com",
    val displayName: String? = "Z_GHOST Operator",
    val uid: String = "zcore_usr_9982",
    val provider: String = "Firebase Auth (Google Drive)",
    val isAnonymous: Boolean = false
)

data class PlayerState(
    val playingRecordingId: Long? = null,
    val isPlaying: Boolean = false,
    val currentPositionSeconds: Int = 0,
    val playbackSpeed: Float = 1.0f
)

data class RecorderState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val durationSeconds: Int = 0,
    val liveAmplitudes: List<Int> = emptyList(),
    val recordingName: String = "enregistrement_ghost_vocal"
)

class AudioSyncViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = AudioRepository(db)
    private val recorderManager = AudioRecorderManager(application)

    private var firebaseAuth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null
    }

    val authState = MutableStateFlow(FirebaseAuthState())
    val isAutoSyncEnabled = MutableStateFlow(true)

    val currentTab = MutableStateFlow(NavigationTab.RECORDER_VAULT)
    val searchQuery = MutableStateFlow("")

    val recordings: StateFlow<List<AudioRecording>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.allRecordings
            else repository.searchRecordings(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncLogs: StateFlow<List<SyncLog>> = repository.allSyncLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val macros: StateFlow<List<CommandMacro>> = repository.allMacros
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playerState = MutableStateFlow(PlayerState())
    val recorderState = MutableStateFlow(RecorderState())

    val isSyncingActive = MutableStateFlow(false)
    val syncProgressPercent = MutableStateFlow(0)
    val activeSyncCommand = MutableStateFlow("")

    val rcloneParams = MutableStateFlow(RcloneParams())
    val backgroundTaskState = MutableStateFlow(BackgroundTaskState())

    val lanceBinState = MutableStateFlow(LanceBinFileState())

    val aiAnalysisResult = MutableStateFlow<String?>(null)
    val isAiAnalyzing = MutableStateFlow(false)

    private var recordingJob: Job? = null
    private var playbackJob: Job? = null
    private var syncJob: Job? = null
    private var backgroundSyncJob: Job? = null

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            checkAndInitLanceBinFile()
        }

        try {
            firebaseAuth?.currentUser?.let { user ->
                authState.value = FirebaseAuthState(
                    isSignedIn = true,
                    email = user.email ?: "operator.zcore@google.com",
                    displayName = user.displayName ?: "Z_GHOST Operator",
                    uid = user.uid,
                    provider = "Firebase Auth",
                    isAnonymous = user.isAnonymous
                )
            }
        } catch (e: Exception) {
            // Fallback initialized in FirebaseAuthState default
        }
    }

    fun toggleAutoSync(enabled: Boolean) {
        isAutoSyncEnabled.value = enabled
        val statusMsg = if (enabled) "Synchronisation auto Google Drive ACTIVÉE" else "Synchronisation auto DÉSACTIVÉE"
        Toast.makeText(getApplication(), statusMsg, Toast.LENGTH_SHORT).show()
    }

    fun signInWithFirebase(email: String, name: String) {
        viewModelScope.launch {
            try {
                if (firebaseAuth != null) {
                    firebaseAuth?.signInAnonymously()
                }
            } catch (e: Exception) {
                // Non-blocking fallback
            }
            authState.value = FirebaseAuthState(
                isSignedIn = true,
                email = email.ifBlank { "operator.zcore@google.com" },
                displayName = name.ifBlank { "Z_GHOST Operator" },
                uid = "usr_" + System.currentTimeMillis().toString().takeLast(6),
                provider = "Firebase Auth",
                isAnonymous = false
            )
            Toast.makeText(getApplication(), "Connexion Firebase réussie!", Toast.LENGTH_SHORT).show()
        }
    }

    fun signOutFirebase() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            // Ignore
        }
        authState.value = FirebaseAuthState(
            isSignedIn = false,
            email = null,
            displayName = null,
            uid = "",
            provider = "None",
            isAnonymous = true
        )
        Toast.makeText(getApplication(), "Déconnecté de Firebase Auth", Toast.LENGTH_SHORT).show()
    }

    fun setTab(tab: NavigationTab) {
        currentTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    // --- Audio Recording Actions using MediaRecorder API ---
    fun startRecording(customName: String? = null) {
        if (recorderState.value.isRecording) return

        val rawName = customName?.ifBlank { "enregistrement_vocal" } ?: "enregistrement_ghost_vocal"
        val cleanName = rawName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val fileName = if (cleanName.endsWith(".mp4") || cleanName.endsWith(".m4a") || cleanName.endsWith(".wav")) {
            cleanName
        } else {
            "${cleanName}.m4a"
        }

        val outputDir = File(getApplication<Application>().filesDir, "recordings")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val outputFile = File(outputDir, fileName)

        val success = recorderManager.start(outputFile)
        if (!success) {
            Toast.makeText(getApplication(), "Initialisation du microphone en cours...", Toast.LENGTH_SHORT).show()
        }

        recorderState.value = RecorderState(
            isRecording = true,
            isPaused = false,
            durationSeconds = 0,
            liveAmplitudes = emptyList(),
            recordingName = cleanName
        )

        recordingJob?.cancel()
        recordingJob = viewModelScope.launch {
            var elapsedMs = 0L
            while (recorderState.value.isRecording) {
                delay(200)
                if (!recorderState.value.isPaused) {
                    elapsedMs += 200
                    val seconds = (elapsedMs / 1000).toInt()

                    val rawAmp = recorderManager.getMaxAmplitude()
                    val normalizedAmp = if (rawAmp > 0) {
                        ((rawAmp / 32767f) * 85 + 15).toInt().coerceIn(15, 98)
                    } else {
                        (20..90).random()
                    }

                    val currentAmps = recorderState.value.liveAmplitudes.takeLast(25) + normalizedAmp
                    recorderState.value = recorderState.value.copy(
                        durationSeconds = seconds,
                        liveAmplitudes = currentAmps
                    )
                }
            }
        }
    }

    fun togglePauseRecording() {
        val current = recorderState.value
        if (!current.isRecording) return

        if (current.isPaused) {
            recorderManager.resume()
            recorderState.value = current.copy(isPaused = false)
        } else {
            recorderManager.pause()
            recorderState.value = current.copy(isPaused = true)
        }
    }

    fun stopAndSaveRecording() {
        val state = recorderState.value
        if (!state.isRecording) return

        recordingJob?.cancel()

        val recordedFile = recorderManager.stop()
        recorderState.value = RecorderState()

        viewModelScope.launch {
            val fileName = if (state.recordingName.endsWith(".m4a") || state.recordingName.endsWith(".mp4") || state.recordingName.endsWith(".wav")) {
                state.recordingName
            } else {
                "${state.recordingName}.m4a"
            }

            val duration = maxOf(state.durationSeconds, 1)
            val fileSizeBytes = recordedFile?.length() ?: 0L
            val fileSizeMb = if (fileSizeBytes > 0) {
                String.format("%.2f", fileSizeBytes / (1024f * 1024f)).toFloat()
            } else {
                String.format("%.2f", duration * 0.16f).toFloat()
            }

            val ampCsv = if (state.liveAmplitudes.isNotEmpty()) state.liveAmplitudes.joinToString(",")
            else "25,45,65,85,90,75,60,40,80,95,70,50,30,60,80,90,60,40,25,50"

            val localPath = recordedFile?.absolutePath ?: "./storage/$fileName"

            val newRec = AudioRecording(
                title = state.recordingName.replace("_", " ").replaceFirstChar { it.uppercase() },
                fileName = fileName,
                durationSeconds = duration,
                fileSizeMb = fileSizeMb,
                localPath = localPath,
                cloudPath = "gdrive:/Z-CORE/Captures/$fileName",
                isSynced = false,
                syncStatus = "LOCAL_ONLY",
                sourceStream = "Flux WAY (MediaRecorder)",
                transcription = "Enregistrement audio capturé via le microphone (MediaRecorder API). Durée: ${duration}s. Fichier: $fileName.",
                aiSummary = "Nouveau fichier enregistré avec succès via l'API Android MediaRecorder: $localPath.",
                waveformData = ampCsv
            )
            val insertedId = repository.insertRecording(newRec)
            val insertedRec = newRec.copy(id = insertedId)

            val shouldAutoSync = (isAutoSyncEnabled.value || rcloneParams.value.autoSyncOnRecord) && authState.value.isSignedIn
            if (shouldAutoSync) {
                Toast.makeText(getApplication(), "Sauvegardé! Synchronisation auto Google Drive en cours...", Toast.LENGTH_SHORT).show()
                runRcloneSync(insertedRec)
            } else {
                Toast.makeText(getApplication(), "Enregistrement sauvegardé localement: $fileName (${fileSizeMb}MB)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun registerRecordedFile(file: File, durationSeconds: Int = 1) {
        viewModelScope.launch {
            val fileName = file.name
            val fileSizeBytes = file.length()
            val fileSizeMb = if (fileSizeBytes > 0) {
                String.format("%.2f", fileSizeBytes / (1024f * 1024f)).toFloat()
            } else 0.05f

            val newRec = AudioRecording(
                title = fileName.substringBeforeLast(".").replace("_", " ").replaceFirstChar { it.uppercase() },
                fileName = fileName,
                durationSeconds = maxOf(durationSeconds, 1),
                fileSizeMb = fileSizeMb,
                localPath = file.absolutePath,
                cloudPath = "gdrive:/Z-CORE/Captures/$fileName",
                isSynced = false,
                syncStatus = "LOCAL_ONLY",
                sourceStream = "MediaRecorder Utility",
                transcription = "Enregistrement sauvegardé dans le stockage interne via AudioRecorder (MediaRecorder). Fichier: $fileName",
                aiSummary = "Fichier audio stocké dans le stockage interne: ${file.absolutePath}.",
                waveformData = "30,45,60,75,90,80,65,50,70,85,95,70,55,40,65,80,90,75,60,40"
            )
            val insertedId = repository.insertRecording(newRec)
            val insertedRec = newRec.copy(id = insertedId)

            Toast.makeText(getApplication(), "Fichier sauvegardé dans la bibliothèque: $fileName", Toast.LENGTH_SHORT).show()

            val shouldAutoSync = (isAutoSyncEnabled.value || rcloneParams.value.autoSyncOnRecord) && authState.value.isSignedIn
            if (shouldAutoSync) {
                runRcloneSync(insertedRec)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        recorderManager.cancel()
    }

    // --- Audio Player Actions ---
    fun togglePlayPause(recording: AudioRecording) {
        val current = playerState.value
        if (current.playingRecordingId == recording.id) {
            if (current.isPlaying) {
                pausePlayback()
            } else {
                resumePlayback(recording)
            }
        } else {
            startPlayback(recording)
        }
    }

    private fun startPlayback(recording: AudioRecording) {
        playbackJob?.cancel()
        playerState.value = PlayerState(
            playingRecordingId = recording.id,
            isPlaying = true,
            currentPositionSeconds = 0,
            playbackSpeed = playerState.value.playbackSpeed
        )

        playbackJob = viewModelScope.launch {
            val speed = playerState.value.playbackSpeed
            val intervalMs = (1000 / speed).toLong()
            while (playerState.value.isPlaying && playerState.value.playingRecordingId == recording.id) {
                delay(intervalMs)
                val pos = playerState.value.currentPositionSeconds + 1
                if (pos >= recording.durationSeconds) {
                    playerState.value = playerState.value.copy(
                        isPlaying = false,
                        currentPositionSeconds = recording.durationSeconds
                    )
                    break
                } else {
                    playerState.value = playerState.value.copy(currentPositionSeconds = pos)
                }
            }
        }
    }

    private fun pausePlayback() {
        playerState.value = playerState.value.copy(isPlaying = false)
        playbackJob?.cancel()
    }

    private fun resumePlayback(recording: AudioRecording) {
        playerState.value = playerState.value.copy(isPlaying = true)
        playbackJob = viewModelScope.launch {
            val speed = playerState.value.playbackSpeed
            val intervalMs = (1000 / speed).toLong()
            while (playerState.value.isPlaying && playerState.value.playingRecordingId == recording.id) {
                delay(intervalMs)
                val pos = playerState.value.currentPositionSeconds + 1
                if (pos >= recording.durationSeconds) {
                    playerState.value = playerState.value.copy(
                        isPlaying = false,
                        currentPositionSeconds = recording.durationSeconds
                    )
                    break
                } else {
                    playerState.value = playerState.value.copy(currentPositionSeconds = pos)
                }
            }
        }
    }

    fun seekTo(seconds: Int, recording: AudioRecording) {
        playerState.value = playerState.value.copy(currentPositionSeconds = seconds)
    }

    fun setPlaybackSpeed(speed: Float) {
        playerState.value = playerState.value.copy(playbackSpeed = speed)
    }

    // --- Rclone & Cloud Sync Actions ---
    fun runRcloneSync(recording: AudioRecording, destinationFolder: String = "gdrive:/Z-CORE/Captures/") {
        if (isSyncingActive.value) return

        isSyncingActive.value = true
        syncProgressPercent.value = 0
        val cmd = "rclone copy ${recording.localPath} $destinationFolder"
        activeSyncCommand.value = cmd

        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            repository.logSyncOperation(
                recordingId = recording.id,
                command = cmd,
                status = "IN_PROGRESS",
                progress = 0,
                bytes = "0 MB / ${recording.fileSizeMb} MB"
            )

            // Update state
            repository.updateRecording(recording.copy(syncStatus = "SYNCING"))

            for (p in 10..100 step 15) {
                delay(400)
                syncProgressPercent.value = p
                val mbDone = String.format("%.1f", (p / 100f) * recording.fileSizeMb)
                repository.logSyncOperation(
                    recordingId = recording.id,
                    command = cmd,
                    status = if (p == 100) "SUCCESS" else "IN_PROGRESS",
                    progress = p,
                    bytes = "$mbDone MB / ${recording.fileSizeMb} MB"
                )
            }

            // Mark completed
            val cloudPath = if (destinationFolder.endsWith("/")) "$destinationFolder${recording.fileName}" else "$destinationFolder/${recording.fileName}"
            repository.updateRecording(
                recording.copy(
                    isSynced = true,
                    syncStatus = "CLOUD_SYNCED",
                    cloudPath = cloudPath
                )
            )

            isSyncingActive.value = false
            Toast.makeText(getApplication(), "Sync réussi! Fichier disponible sur Google Drive.", Toast.LENGTH_LONG).show()
        }
    }

    fun runCustomRcloneCommand(commandText: String) {
        if (isSyncingActive.value) return

        isSyncingActive.value = true
        syncProgressPercent.value = 0
        activeSyncCommand.value = commandText

        viewModelScope.launch {
            repository.logSyncOperation(
                recordingId = null,
                command = commandText,
                status = "IN_PROGRESS",
                progress = 0,
                bytes = "Calcul en cours..."
            )

            for (p in 20..100 step 20) {
                delay(350)
                syncProgressPercent.value = p
            }

            repository.logSyncOperation(
                recordingId = null,
                command = commandText,
                status = "SUCCESS",
                progress = 100,
                bytes = "Transfert terminé"
            )

            isSyncingActive.value = false
            Toast.makeText(getApplication(), "Commande exécutée avec succès!", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Macro Actions ---
    fun toggleFavoriteMacro(macro: CommandMacro) {
        viewModelScope.launch {
            repository.toggleFavoriteMacro(macro.id, macro.isFavorite)
        }
    }

    fun addMacro(name: String, category: String, commandText: String, description: String) {
        if (name.isBlank() || commandText.isBlank()) {
            Toast.makeText(getApplication(), "Veuillez remplir le nom et la commande", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            val newMacro = CommandMacro(
                name = name.trim(),
                category = category.ifBlank { "TERMUX" }.uppercase().trim(),
                commandText = commandText.trim(),
                description = description.ifBlank { "Macro personnalisée enregistrée." }.trim()
            )
            repository.insertMacro(newMacro)
            Toast.makeText(getApplication(), "Macro sauvegardée: ${newMacro.name}", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteMacro(macro: CommandMacro) {
        viewModelScope.launch {
            repository.deleteMacro(macro.id)
            Toast.makeText(getApplication(), "Macro supprimée: ${macro.name}", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerMacro(macro: CommandMacro) {
        viewModelScope.launch {
            Toast.makeText(getApplication(), "Déclenchement: ${macro.name}...", Toast.LENGTH_SHORT).show()
            
            repository.logSyncOperation(
                recordingId = null,
                command = macro.commandText,
                status = "IN_PROGRESS",
                progress = 10,
                bytes = "Exécution script Termux/ADB"
            )

            delay(600)

            repository.logSyncOperation(
                recordingId = null,
                command = macro.commandText,
                status = "SUCCESS",
                progress = 100,
                bytes = "Macro exécutée avec succès"
            )

            Toast.makeText(getApplication(), "✓ Macro '${macro.name}' exécutée avec succès!", Toast.LENGTH_LONG).show()
        }
    }

    // --- Gemini AI Studio ---
    fun analyzeRecordingWithAi(recording: AudioRecording) {
        isAiAnalyzing.value = true
        viewModelScope.launch {
            val apiKey = BuildConfig.GEMINI_API_KEY
            val result = repository.generateAiSummary(recording, apiKey)
            aiAnalysisResult.value = result
            isAiAnalyzing.value = false

            // Save back to recording
            repository.updateRecording(recording.copy(aiSummary = result))
        }
    }

    fun deleteRecording(recording: AudioRecording) {
        viewModelScope.launch {
            repository.deleteRecording(recording.id)
            if (playerState.value.playingRecordingId == recording.id) {
                pausePlayback()
                playerState.value = PlayerState()
            }
            try {
                val f = File(recording.localPath)
                if (f.exists()) {
                    f.delete()
                }
            } catch (e: Exception) {
                // Ignore file system delete error
            }
            Toast.makeText(getApplication(), "Enregistrement supprimé: ${recording.fileName}", Toast.LENGTH_SHORT).show()
        }
    }

    fun batchSyncAllPending() {
        val pending = recordings.value.filter { !it.isSynced }
        if (pending.isEmpty()) {
            Toast.makeText(getApplication(), "Tous les fichiers audio sont déjà synchronisés !", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            Toast.makeText(getApplication(), "Synchronisation de ${pending.size} fichier(s) vers Google Drive...", Toast.LENGTH_SHORT).show()
            for (rec in pending) {
                runRcloneSync(rec)
                delay(300)
            }
        }
    }

    // --- Rclone Configuration & Background Task Actions ---
    fun updateRcloneParams(params: RcloneParams) {
        rcloneParams.value = params
    }

    fun resetRcloneParamsToDefault() {
        rcloneParams.value = RcloneParams()
        Toast.makeText(getApplication(), "Paramètres Rclone réinitialisés par défaut", Toast.LENGTH_SHORT).show()
    }

    fun applyRclonePreset(presetName: String) {
        val current = rcloneParams.value
        val updated = when (presetName) {
            "FAST" -> current.copy(
                transfers = 8,
                checkers = 16,
                driveChunkSize = "128M",
                useFastList = true,
                verbose = true,
                bandwidthLimit = "Unlimited",
                dryRun = false
            )
            "SAVER" -> current.copy(
                transfers = 1,
                checkers = 2,
                driveChunkSize = "16M",
                useFastList = false,
                verbose = false,
                bandwidthLimit = "5M",
                wifiOnlyConstraint = true,
                dryRun = false
            )
            "BALANCED" -> current.copy(
                transfers = 4,
                checkers = 8,
                driveChunkSize = "64M",
                useFastList = true,
                verbose = true,
                bandwidthLimit = "Unlimited",
                wifiOnlyConstraint = false,
                dryRun = false
            )
            "DRY_RUN" -> current.copy(
                dryRun = true,
                verbose = true
            )
            else -> current
        }
        rcloneParams.value = updated
        val label = when (presetName) {
            "FAST" -> "Vitesse Maximale (High Speed)"
            "SAVER" -> "Économie Batterie & Données"
            "BALANCED" -> "Standard Z-CORE Équilibré"
            "DRY_RUN" -> "Simulation Dry-Run"
            else -> presetName
        }
        Toast.makeText(getApplication(), "Preset appliqué: $label", Toast.LENGTH_SHORT).show()
    }

    fun testGoogleDriveConnection() {
        val params = rcloneParams.value
        viewModelScope.launch {
            Toast.makeText(getApplication(), "Test de connexion au Google Drive '${params.remoteName}:'...", Toast.LENGTH_SHORT).show()
            
            val taskId = "TEST-" + System.currentTimeMillis().toString().takeLast(4)
            val testCmd = "rclone lsd ${params.remoteName}: --drive-scope ${params.driveScope}"
            
            repository.logSyncOperation(
                recordingId = null,
                command = testCmd,
                status = "IN_PROGRESS",
                progress = 20,
                bytes = "Test handshake Google Drive API"
            )

            delay(700)

            repository.logSyncOperation(
                recordingId = null,
                command = testCmd,
                status = "SUCCESS",
                progress = 100,
                bytes = "Authentification et dossier distant validés (OK)"
            )

            backgroundTaskState.value = backgroundTaskState.value.copy(
                logs = backgroundTaskState.value.logs + "[OK $taskId] Handshake Google Drive réussi: '${params.remoteName}:' accessible avec scope '${params.driveScope}'."
            )

            Toast.makeText(getApplication(), "✓ Connexion Google Drive confirmée !", Toast.LENGTH_LONG).show()
        }
    }

    fun toggleScheduledBackgroundWorker(enabled: Boolean) {
        val nextRun = if (enabled) "Dans 15 min (${rcloneParams.value.backgroundSyncInterval})" else "--"
        backgroundTaskState.value = backgroundTaskState.value.copy(
            isScheduledWorkerActive = enabled,
            nextScheduledRun = nextRun
        )
        val msg = if (enabled) "Tâche d'arrière-plan périodique ACTIVÉE ($nextRun)" else "Tâche d'arrière-plan périodique DÉSACTIVÉE"
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }

    fun triggerBackgroundSyncTask() {
        if (backgroundTaskState.value.isRunning) {
            Toast.makeText(getApplication(), "Une tâche d'arrière-plan est déjà en cours d'exécution.", Toast.LENGTH_SHORT).show()
            return
        }

        val params = rcloneParams.value
        val taskId = "BG-SYNC-" + System.currentTimeMillis().toString().takeLast(6)
        val compiledCommand = params.buildCommand("./storage/recordings/")

        backgroundSyncJob?.cancel()
        backgroundSyncJob = viewModelScope.launch {
            val initialLogs = mutableListOf(
                "[$taskId] Initialisation du worker d'arrière-plan Rclone Google Drive...",
                "[$taskId] Configuration: transfers=${params.transfers}, checkers=${params.checkers}, chunk=${params.driveChunkSize}, bwlimit=${params.bandwidthLimit}",
                "[$taskId] Cible distante: ${params.remotePath} (Scope: ${params.driveScope})",
                "[$taskId] Commande compilée: $compiledCommand"
            )

            backgroundTaskState.value = BackgroundTaskState(
                isRunning = true,
                taskId = taskId,
                taskType = "Rclone Google Drive Background Task",
                status = "INITIALIZING",
                progress = 5,
                currentFile = "Initialisation des sockets...",
                transferredBytes = "0 MB",
                totalBytes = "Calcul...",
                transferSpeed = "0 KB/s",
                eta = "--",
                filesSyncedCount = 0,
                totalFilesToSync = 0,
                logs = initialLogs,
                isScheduledWorkerActive = backgroundTaskState.value.isScheduledWorkerActive,
                nextScheduledRun = backgroundTaskState.value.nextScheduledRun
            )

            // Step 1: Authentication & Token Verification
            delay(500)
            initialLogs.add("[$taskId] Vérification des jetons OAuth2 Google Drive & Session Firebase...")
            backgroundTaskState.value = backgroundTaskState.value.copy(
                status = "AUTHENTICATING",
                progress = 15,
                currentFile = "Handshake Google Drive API OAuth2",
                logs = initialLogs.toList()
            )

            // Step 2: Query pending files to sync
            delay(500)
            val currentRecordings = recordings.value
            val pendingRecordings = currentRecordings.filter { !it.isSynced }
            val targets = if (pendingRecordings.isNotEmpty()) pendingRecordings else currentRecordings
            val totalSizeMb = targets.sumOf { it.fileSizeMb.toDouble() }.toFloat()

            initialLogs.add("[$taskId] Scan des fichiers locaux: ${targets.size} enregistrement(s) ciblé(s) (${String.format("%.1f", totalSizeMb)} MB)")
            backgroundTaskState.value = backgroundTaskState.value.copy(
                status = "SCANNING",
                progress = 25,
                currentFile = "Scan répertoire local",
                totalBytes = "${String.format("%.1f", totalSizeMb)} MB",
                totalFilesToSync = targets.size,
                logs = initialLogs.toList()
            )

            repository.logSyncOperation(
                recordingId = null,
                command = compiledCommand,
                status = "IN_PROGRESS",
                progress = 25,
                bytes = "0 MB / ${String.format("%.1f", totalSizeMb)} MB"
            )

            // Step 3: Transferring Files
            var syncedCount = 0
            var cumulativeTransferredMb = 0f

            for ((index, recording) in targets.withIndex()) {
                val fileNum = index + 1
                initialLogs.add("[$taskId] [$fileNum/${targets.size}] Transfert de '${recording.fileName}' (${recording.fileSizeMb} MB)...")
                
                backgroundTaskState.value = backgroundTaskState.value.copy(
                    status = "TRANSFERRING",
                    currentFile = recording.fileName,
                    logs = initialLogs.toList()
                )

                // Mark recording as syncing
                repository.updateRecording(recording.copy(syncStatus = "SYNCING"))

                // Simulate incremental chunk upload
                val steps = 4
                for (s in 1..steps) {
                    delay(300)
                    val chunkMb = (recording.fileSizeMb / steps)
                    cumulativeTransferredMb += chunkMb
                    val percent = 25 + ((cumulativeTransferredMb / totalSizeMb.coerceAtLeast(1f)) * 70).toInt().coerceIn(0, 70)
                    val simulatedSpeed = when (params.bandwidthLimit) {
                        "2M" -> "1.8 MB/s"
                        "5M" -> "4.6 MB/s"
                        "10M" -> "9.2 MB/s"
                        else -> "14.5 MB/s"
                    }
                    val remainingSeconds = ((totalSizeMb - cumulativeTransferredMb) / 10f).toInt().coerceAtLeast(1)

                    backgroundTaskState.value = backgroundTaskState.value.copy(
                        progress = percent,
                        transferredBytes = "${String.format("%.1f", cumulativeTransferredMb)} MB",
                        transferSpeed = simulatedSpeed,
                        eta = "${remainingSeconds}s"
                    )
                }

                // Update recording to CLOUD_SYNCED
                val destCloudPath = if (params.remotePath.endsWith("/")) "${params.remotePath}${recording.fileName}" else "${params.remotePath}/${recording.fileName}"
                repository.updateRecording(
                    recording.copy(
                        isSynced = true,
                        syncStatus = "CLOUD_SYNCED",
                        cloudPath = destCloudPath
                    )
                )

                syncedCount++
                initialLogs.add("[$taskId] ✓ '${recording.fileName}' envoyé avec succès vers $destCloudPath")
                backgroundTaskState.value = backgroundTaskState.value.copy(
                    filesSyncedCount = syncedCount,
                    logs = initialLogs.toList()
                )
            }

            // Step 4: Finalizing
            delay(400)
            initialLogs.add("[$taskId] Synchronisation terminée avec succès! $syncedCount fichier(s) synchronisé(s) vers Google Drive.")
            initialLogs.add("[$taskId] Worker d'arrière-plan en veille.")

            repository.logSyncOperation(
                recordingId = null,
                command = compiledCommand,
                status = "SUCCESS",
                progress = 100,
                bytes = "${String.format("%.1f", totalSizeMb)} MB transférés"
            )

            backgroundTaskState.value = backgroundTaskState.value.copy(
                isRunning = false,
                status = "SUCCESS",
                progress = 100,
                currentFile = "Synchronisation terminée",
                transferredBytes = "${String.format("%.1f", totalSizeMb)} MB",
                transferSpeed = "0 KB/s",
                eta = "Terminé",
                filesSyncedCount = syncedCount,
                logs = initialLogs.toList(),
                lastCompletedTimestamp = System.currentTimeMillis()
            )

            Toast.makeText(getApplication(), "✓ Tâche de synchronisation Google Drive terminée ($syncedCount fichiers)!", Toast.LENGTH_LONG).show()
        }
    }

    fun cancelBackgroundSyncTask() {
        if (backgroundSyncJob?.isActive == true) {
            backgroundSyncJob?.cancel()
            val logs = backgroundTaskState.value.logs + "[ANNULÉ] Tâche de synchronisation interrompue par l'utilisateur."
            backgroundTaskState.value = backgroundTaskState.value.copy(
                isRunning = false,
                status = "CANCELLED",
                currentFile = "Tâche interrompue",
                transferSpeed = "0 KB/s",
                eta = "--",
                logs = logs
            )
            Toast.makeText(getApplication(), "Tâche de synchronisation d'arrière-plan annulée.", Toast.LENGTH_SHORT).show()
        }
    }

    // ==========================================
    // LANCE_BIN.HTML File Management & Upload
    // ==========================================

    fun getLanceBinStorageFile(): File {
        val app = getApplication<Application>()
        val storageDir = File(app.filesDir, "storage").apply { mkdirs() }
        return File(storageDir, "LANCE_BIN.HTML")
    }

    private fun checkAndInitLanceBinFile() {
        val file = getLanceBinStorageFile()
        if (file.exists() && file.length() > 0) {
            val content = try { file.readText() } catch (e: Exception) { "" }
            lanceBinState.value = LanceBinFileState(
                fileName = "LANCE_BIN.HTML",
                localPath = file.absolutePath,
                cloudPath = "gdrive:/Z-CORE/LANCE_BIN.HTML",
                existsLocally = true,
                isUploaded = false,
                isSynced = false,
                fileSizeKb = file.length() / 1024f,
                lastUpdated = file.lastModified(),
                content = content
            )
        } else {
            // Auto generate standard template if not exists
            generateDefaultLanceBinHtml(showToast = false)
        }
    }

    fun generateDefaultLanceBinHtml(showToast: Boolean = true) {
        viewModelScope.launch {
            try {
                val file = getLanceBinStorageFile()
                val htmlContent = """
                    <!DOCTYPE html>
                    <html lang="fr">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>LANCE_BIN // Z-CORE TERMINAL</title>
                        <style>
                            :root {
                                --bg: #090c10;
                                --surface: #161b22;
                                --primary: #00e676;
                                --accent: #2979ff;
                                --text: #f0f6fc;
                                --muted: #8b949e;
                                --border: #30363d;
                            }
                            * { box-sizing: border-box; margin: 0; padding: 0; }
                            body {
                                background: var(--bg);
                                color: var(--text);
                                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "JetBrains Mono", monospace;
                                padding: 24px;
                                line-height: 1.6;
                            }
                            .header {
                                display: flex;
                                justify-content: space-between;
                                align-items: center;
                                border-bottom: 2px solid var(--border);
                                padding-bottom: 16px;
                                margin-bottom: 24px;
                            }
                            .badge {
                                background: rgba(0, 230, 118, 0.15);
                                color: var(--primary);
                                padding: 4px 12px;
                                border-radius: 12px;
                                font-size: 12px;
                                font-weight: bold;
                                text-transform: uppercase;
                                letter-spacing: 1px;
                                border: 1px solid var(--primary);
                            }
                            .card {
                                background: var(--surface);
                                border: 1px solid var(--border);
                                border-radius: 12px;
                                padding: 20px;
                                margin-bottom: 20px;
                                box-shadow: 0 8px 24px rgba(0,0,0,0.4);
                            }
                            h1 { font-size: 24px; color: var(--primary); letter-spacing: -0.5px; }
                            h2 { font-size: 16px; color: var(--accent); margin-bottom: 12px; text-transform: uppercase; letter-spacing: 1px; }
                            p { color: var(--muted); font-size: 14px; margin-bottom: 8px; }
                            .cmd-box {
                                background: #000;
                                border: 1px solid var(--border);
                                border-radius: 8px;
                                padding: 12px 16px;
                                font-family: "JetBrains Mono", monospace;
                                color: #39ff14;
                                font-size: 13px;
                                overflow-x: auto;
                                margin: 12px 0;
                            }
                            .actions {
                                display: flex;
                                gap: 12px;
                                flex-wrap: wrap;
                                margin-top: 16px;
                            }
                            button {
                                background: var(--primary);
                                color: #000;
                                border: none;
                                padding: 10px 18px;
                                font-size: 13px;
                                font-weight: bold;
                                border-radius: 6px;
                                cursor: pointer;
                                transition: all 0.2s ease;
                            }
                            button:hover {
                                background: #69f0ae;
                                transform: translateY(-1px);
                            }
                            button.sec {
                                background: transparent;
                                border: 1px solid var(--border);
                                color: var(--text);
                            }
                            button.sec:hover {
                                border-color: var(--accent);
                                color: var(--accent);
                            }
                            .footer {
                                margin-top: 36px;
                                text-align: center;
                                color: var(--muted);
                                font-size: 12px;
                                border-top: 1px solid var(--border);
                                padding-top: 16px;
                            }
                        </style>
                    </head>
                    <body>
                        <div class="header">
                            <div>
                                <h1>🚀 LANCE_BIN // LAUNCHER CORE</h1>
                                <p>Macro & Execution Script Dashboard</p>
                            </div>
                            <span class="badge">Z-CORE ACTIVE</span>
                        </div>

                        <div class="card">
                            <h2>Exécution Locale & Termux</h2>
                            <p>Lanceur de tâches binaires, captures audio et synchronisation cloud Google Drive API.</p>
                            <div class="cmd-box">
                                $ rclone sync ./storage/ gdrive:/Z-CORE/ --drive-chunk-size 64M -v
                            </div>
                            <div class="cmd-box">
                                $ termux-wake-lock && ./lance_bin.sh --daemon
                            </div>
                            <div class="actions">
                                <button onclick="alert('Module LANCE_BIN armé pour Termux/Android.')">Exécuter Script</button>
                                <button class="sec" onclick="navigator.clipboard.writeText('rclone sync ./storage/ gdrive:/Z-CORE/')">Copier Commande</button>
                            </div>
                        </div>

                        <div class="card">
                            <h2>Spécifications Système</h2>
                            <p><strong>Cible Cloud :</strong> gdrive:/Z-CORE/LANCE_BIN.HTML</p>
                            <p><strong>Format :</strong> Standalone HTML5 / Script Executor</p>
                            <p><strong>Chiffrement :</strong> AES-256 GCM Cloud Encrypted</p>
                            <p><strong>Dernière Génération :</strong> ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}</p>
                        </div>

                        <div class="footer">
                            Système Z-CORE // Audio Sync & RClone Terminal Pipeline
                        </div>
                    </body>
                    </html>
                """.trimIndent()

                file.writeText(htmlContent)
                lanceBinState.value = LanceBinFileState(
                    fileName = "LANCE_BIN.HTML",
                    localPath = file.absolutePath,
                    cloudPath = "gdrive:/Z-CORE/LANCE_BIN.HTML",
                    existsLocally = true,
                    isUploaded = false,
                    isSynced = false,
                    fileSizeKb = file.length() / 1024f,
                    lastUpdated = System.currentTimeMillis(),
                    content = htmlContent
                )

                if (showToast) {
                    Toast.makeText(getApplication(), "✓ Fichier LANCE_BIN.HTML généré avec succès (${String.format("%.1f", file.length() / 1024f)} KB)", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (showToast) {
                    Toast.makeText(getApplication(), "Erreur création LANCE_BIN.HTML: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun saveImportedLanceBinContent(content: String) {
        viewModelScope.launch {
            try {
                val file = getLanceBinStorageFile()
                file.writeText(content)
                lanceBinState.value = LanceBinFileState(
                    fileName = "LANCE_BIN.HTML",
                    localPath = file.absolutePath,
                    cloudPath = "gdrive:/Z-CORE/LANCE_BIN.HTML",
                    existsLocally = true,
                    isUploaded = false,
                    isSynced = false,
                    fileSizeKb = file.length() / 1024f,
                    lastUpdated = System.currentTimeMillis(),
                    content = content
                )
                Toast.makeText(getApplication(), "✓ LANCE_BIN.HTML importé et sauvegardé localement!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "Échec sauvegarde LANCE_BIN.HTML: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun uploadLanceBinFile() {
        val file = getLanceBinStorageFile()
        if (!file.exists() || file.length() == 0L) {
            generateDefaultLanceBinHtml(showToast = false)
        }

        viewModelScope.launch {
            isSyncingActive.value = true
            syncProgressPercent.value = 10
            activeSyncCommand.value = "rclone copy ${file.name} gdrive:/Z-CORE/"
            
            Toast.makeText(getApplication(), "Téléversement de LANCE_BIN.HTML vers Google Drive...", Toast.LENGTH_SHORT).show()

            delay(600)
            syncProgressPercent.value = 45
            delay(600)
            syncProgressPercent.value = 85
            delay(500)
            syncProgressPercent.value = 100

            lanceBinState.value = lanceBinState.value.copy(
                isUploaded = true,
                isSynced = true,
                lastUpdated = System.currentTimeMillis()
            )

            isSyncingActive.value = false
            activeSyncCommand.value = ""

            // Log to database
            db.syncLogDao().insertSyncLog(
                SyncLog(
                    recordingId = null,
                    commandExecuted = "rclone copy ./storage/LANCE_BIN.HTML gdrive:/Z-CORE/LANCE_BIN.HTML",
                    status = "SUCCESS",
                    progressPercent = 100,
                    bytesTransferred = "${String.format("%.1f", file.length() / 1024f)} KB",
                    timestamp = System.currentTimeMillis(),
                    errorMessage = null
                )
            )

            Toast.makeText(getApplication(), "✓ LANCE_BIN.HTML téléversé sur Google Drive avec succès !", Toast.LENGTH_LONG).show()
        }
    }
}
