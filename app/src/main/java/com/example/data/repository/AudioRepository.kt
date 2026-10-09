package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.AudioRecording
import com.example.data.model.CommandMacro
import com.example.data.model.SyncLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class AudioRepository(private val db: AppDatabase) {

    val allRecordings: Flow<List<AudioRecording>> = db.audioRecordingDao().getAllRecordings()
    val allSyncLogs: Flow<List<SyncLog>> = db.syncLogDao().getAllSyncLogs()
    val allMacros: Flow<List<CommandMacro>> = db.commandMacroDao().getAllMacros()

    fun searchRecordings(query: String): Flow<List<AudioRecording>> {
        return db.audioRecordingDao().searchRecordings(query)
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        if (db.audioRecordingDao().getCount() == 0) {
            val initialRecordings = listOf(
                AudioRecording(
                    title = "Flux WAY Vocal Capture",
                    fileName = "enregistrement_ghost_vocal.wav",
                    durationSeconds = 60,
                    fileSizeMb = 10.4f,
                    localPath = "./storage/enregistrement_ghost_vocal.wav",
                    cloudPath = "gdrive:/Z-CORE/Captures/enregistrement_ghost_vocal.wav",
                    isSynced = false,
                    syncStatus = "LOCAL_ONLY",
                    sourceStream = "Flux WAY (Z-CORE)",
                    timestamp = System.currentTimeMillis() - 3600000,
                    transcription = "Capture vocale d'une minute issue du flux WAY. Écoutable et prêt pour transfert cloud vers gdrive:/Z-CORE/Captures/.",
                    aiSummary = "Enregistrement vocal de 60s capturé en local sur l'instance Z-CORE. Prêt à être transféré via rclone vers Google Drive.",
                    waveformData = "20,45,80,65,30,90,70,85,95,60,40,75,85,90,65,50,80,95,70,40"
                ),
                AudioRecording(
                    title = "Archives Vocales 2023",
                    fileName = "enregistrement2023-02-06 07-11-38.wav",
                    durationSeconds = 185,
                    fileSizeMb = 31.2f,
                    localPath = "./storage/archives/enregistrement2023-02-06.wav",
                    cloudPath = "gdrive:/Archives/2023/enregistrement2023-02-06 07-11-38.wav",
                    isSynced = true,
                    syncStatus = "CLOUD_SYNCED",
                    sourceStream = "Drive Archive",
                    timestamp = System.currentTimeMillis() - 86400000000L,
                    transcription = "Archive audio historique de février 2023 déjà présente sur le Google Drive principal.",
                    aiSummary = "Fichier audio archivé de 3m05s. Sauvegarde confirmée sur le stockage cloud distant.",
                    waveformData = "15,30,40,50,45,60,70,65,80,75,60,50,40,30,25,35,45,55,60,50"
                )
            )
            for (rec in initialRecordings) {
                db.audioRecordingDao().insertRecording(rec)
            }
        }

        if (db.commandMacroDao().getCount() == 0) {
            val initialMacros = listOf(
                CommandMacro(
                    name = "Sync Ghost Vocal to Drive",
                    category = "RCLONE",
                    commandText = "rclone copy ./storage/enregistrement_ghost_vocal.wav gdrive:/Z-CORE/Captures/",
                    description = "Copie chiffrée de l'enregistrement Z-CORE vers Google Drive."
                ),
                CommandMacro(
                    name = "Sync Root Drive Folder",
                    category = "RCLONE",
                    commandText = "rclone copy ./storage/enregistrement_ghost_vocal.wav gdrive:/",
                    description = "Envoie le fichier directement à la racine de Google Drive."
                ),
                CommandMacro(
                    name = "Setup Termux API Tools",
                    category = "TERMUX",
                    commandText = "pkg update && pkg install termux-api",
                    description = "Installe les extensions Termux API pour gérer le matériel et l'affichage."
                ),
                CommandMacro(
                    name = "Acquire Wake Lock",
                    category = "TERMUX",
                    commandText = "termux-wake-lock",
                    description = "Empêche l'écran de se mettre en veille pendant les transferts."
                ),
                CommandMacro(
                    name = "Release Wake Lock",
                    category = "TERMUX",
                    commandText = "termux-wake-unlock",
                    description = "Permet à l'écran de se remettre en veille normalement."
                ),
                CommandMacro(
                    name = "ADB Wake Screen",
                    category = "ADB",
                    commandText = "adb shell input keyevent 26",
                    description = "Simule l'appui sur le bouton d'alimentation pour réveiller le téléphone."
                ),
                CommandMacro(
                    name = "ADB Swipe Unlock",
                    category = "ADB",
                    commandText = "adb shell input swipe 500 1500 500 500 200",
                    description = "Simule un glissement vers le haut pour afficher l'écran de déverrouillage."
                ),
                CommandMacro(
                    name = "Z_GHOST Wakeup Target",
                    category = "Z_GHOST",
                    commandText = "mode Z_GHOST_TLE wakeup --target +14389855041",
                    description = "Commande de réveil à distance vers le terminal cible via le protocole Z_GHOST."
                ),
                CommandMacro(
                    name = "DK Secure Element Status",
                    category = "DK",
                    commandText = "dumpsys org.carconnectivity.android.digitalkey.secureelement",
                    description = "Vérifie l'état de l'élément sécurisé et de la clé numérique Samsung/CarConnectivity (DK)."
                ),
                CommandMacro(
                    name = "DK Ranging Service Test",
                    category = "DK",
                    commandText = "adb shell am start-activity -a org.carconnectivity.android.digitalkey.rangingintent",
                    description = "Teste la télémétrie et le canal de localisation ultra-wideband Digital Key."
                ),
                CommandMacro(
                    name = "DK Native Library Check",
                    category = "DK",
                    commandText = "ls -la /vendor/lib64/*dk.samsung.so",
                    description = "Inspecte la présence des bibliothèques natives lib_vnd_client.dk.samsung.so."
                ),
                CommandMacro(
                    name = "LANCE_BIN.HTML Runner",
                    category = "SCRIPT",
                    commandText = "termux-open ./storage/LANCE_BIN.HTML || am start -a android.intent.action.VIEW -d 'file:///data/user/0/com.aistudio.ghostsync.kmrqzx/files/storage/LANCE_BIN.HTML'",
                    description = "Exécute et ouvre l'interface de lancement LANCE_BIN.HTML pour orchestrer les scripts et captures."
                ),
                CommandMacro(
                    name = "Upload LANCE_BIN to Drive",
                    category = "RCLONE",
                    commandText = "rclone copy ./storage/LANCE_BIN.HTML gdrive:/Z-CORE/ --drive-chunk-size 64M -v",
                    description = "Téléverse le fichier LANCE_BIN.HTML vers le cloud Google Drive Z-CORE."
                )
            )
            db.commandMacroDao().insertAll(initialMacros)
        }
    }

    suspend fun insertRecording(recording: AudioRecording): Long = withContext(Dispatchers.IO) {
        db.audioRecordingDao().insertRecording(recording)
    }

    suspend fun updateRecording(recording: AudioRecording) = withContext(Dispatchers.IO) {
        db.audioRecordingDao().updateRecording(recording)
    }

    suspend fun deleteRecording(id: Long) = withContext(Dispatchers.IO) {
        db.audioRecordingDao().deleteRecording(id)
    }

    suspend fun logSyncOperation(
        recordingId: Long?,
        command: String,
        status: String,
        progress: Int,
        bytes: String,
        errorMsg: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val log = SyncLog(
            recordingId = recordingId,
            commandExecuted = command,
            status = status,
            progressPercent = progress,
            bytesTransferred = bytes,
            errorMessage = errorMsg
        )
        db.syncLogDao().insertSyncLog(log)
    }

    suspend fun insertMacro(macro: CommandMacro): Long = withContext(Dispatchers.IO) {
        db.commandMacroDao().insertMacro(macro)
    }

    suspend fun deleteMacro(id: Long) = withContext(Dispatchers.IO) {
        db.commandMacroDao().deleteMacro(id)
    }

    suspend fun toggleFavoriteMacro(id: Long, currentFav: Boolean) = withContext(Dispatchers.IO) {
        db.commandMacroDao().toggleFavorite(id, !currentFav)
    }

    suspend fun generateAiSummary(recording: AudioRecording, apiKey: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Analyse Locale Z-CORE: '${recording.title}' est une capture audio de ${recording.durationSeconds}s (${recording.fileSizeMb}MB). Format .wav prêt pour diffusion ou transfert cloud RClone."
        }
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val promptText = "Tu es l'assistant IA Z-CORE. Rédige un résumé fluide, professionnel et concis en français pour l'enregistrement vocal nommé '${recording.title}' (${recording.fileName}), durée ${recording.durationSeconds} secondes, issu du flux ${recording.sourceStream}. Donne les points clés et l'état de synchronisation."

            val jsonBody = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                        })
                    })
                })
            }

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(jsonBody.toString())
            writer.flush()
            writer.close()

            if (conn.responseCode == 200) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val responseJson = JSONObject(responseStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text")
                    }
                }
            }
            return@withContext "Synthèse Z-CORE: Capture ${recording.fileName} de ${recording.durationSeconds}s traitée. Prête pour révision."
        } catch (e: Exception) {
            return@withContext "Note d'analyse: Fichier '${recording.fileName}' vérifié. Durée: ${recording.durationSeconds}s. Statut sync: ${recording.syncStatus}."
        }
    }
}
