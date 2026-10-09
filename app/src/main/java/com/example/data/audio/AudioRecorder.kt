package com.example.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Common contract for an AudioRecorder utility using Android's MediaRecorder API.
 */
interface AudioRecorder {
    val isRecording: Boolean
    val isPaused: Boolean
    val currentOutputFile: File?

    /**
     * Starts recording audio to the specified [outputFile].
     * @return true if recording successfully started, false otherwise.
     */
    fun start(outputFile: File): Boolean

    /**
     * Starts recording audio and saves the file directly into app's internal storage directory.
     * @param fileName optional custom filename. If null or blank, generates a timestamped name.
     * @return The target [File] if started successfully, null otherwise.
     */
    fun startInInternalStorage(fileName: String? = null): File?

    /**
     * Pauses the current recording (Android 7.0+).
     */
    fun pause()

    /**
     * Resumes the paused recording (Android 7.0+).
     */
    fun resume()

    /**
     * Stops the recording, releases the MediaRecorder, and returns the saved [File] in internal storage.
     */
    fun stop(): File?

    /**
     * Cancels the recording and deletes any partial recorded file.
     */
    fun cancel()

    /**
     * Returns the maximum peak amplitude since the last call (0..32767).
     */
    fun getMaxAmplitude(): Int

    /**
     * Returns the default internal storage directory dedicated for audio recordings.
     */
    fun getInternalStorageDir(): File
}

/**
 * Robust implementation of [AudioRecorder] utilizing Android's [MediaRecorder].
 * Captures microphone audio, encodes with AAC into an MPEG-4 (.m4a) container,
 * and saves to application internal storage.
 */
class BasicAudioRecorder(private val context: Context) : AudioRecorder {

    private val tag = "BasicAudioRecorder"

    private var mediaRecorder: MediaRecorder? = null

    override var isRecording: Boolean = false
        private set

    override var isPaused: Boolean = false
        private set

    override var currentOutputFile: File? = null
        private set

    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }

    override fun getInternalStorageDir(): File {
        val dir = File(context.filesDir, "recordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    override fun startInInternalStorage(fileName: String?): File? {
        val cleanName = if (fileName.isNullOrBlank()) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            "recording_$timestamp.m4a"
        } else {
            val sanitized = fileName.trim().replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            if (sanitized.endsWith(".m4a") || sanitized.endsWith(".mp4") || sanitized.endsWith(".aac")) {
                sanitized
            } else {
                "$sanitized.m4a"
            }
        }

        val targetFile = File(getInternalStorageDir(), cleanName)
        val started = start(targetFile)
        return if (started) targetFile else null
    }

    override fun start(outputFile: File): Boolean {
        return try {
            stopAndRelease()

            currentOutputFile = outputFile
            val parentDir = outputFile.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }

            val recorder = createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            isRecording = true
            isPaused = false
            Log.d(tag, "MediaRecorder started successfully, saving to: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to start MediaRecorder", e)
            stopAndRelease()
            false
        }
    }

    override fun pause() {
        if (isRecording && !isPaused) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    mediaRecorder?.pause()
                    isPaused = true
                    Log.d(tag, "MediaRecorder paused")
                } catch (e: Exception) {
                    Log.e(tag, "Error pausing MediaRecorder", e)
                }
            }
        }
    }

    override fun resume() {
        if (isRecording && isPaused) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    mediaRecorder?.resume()
                    isPaused = false
                    Log.d(tag, "MediaRecorder resumed")
                } catch (e: Exception) {
                    Log.e(tag, "Error resuming MediaRecorder", e)
                }
            }
        }
    }

    override fun getMaxAmplitude(): Int {
        return try {
            if (isRecording && !isPaused) {
                mediaRecorder?.maxAmplitude ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    override fun stop(): File? {
        val file = currentOutputFile
        try {
            if (isRecording) {
                mediaRecorder?.stop()
                Log.d(tag, "MediaRecorder stopped. File saved to internal storage: ${file?.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error stopping MediaRecorder", e)
        } finally {
            stopAndRelease()
        }
        return file
    }

    override fun cancel() {
        try {
            currentOutputFile?.let {
                if (it.exists()) {
                    it.delete()
                    Log.d(tag, "Canceled recording and deleted file: ${it.absolutePath}")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error deleting canceled recording file", e)
        } finally {
            stopAndRelease()
        }
    }

    private fun stopAndRelease() {
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(tag, "Error releasing MediaRecorder", e)
        } finally {
            mediaRecorder = null
            isRecording = false
            isPaused = false
            currentOutputFile = null
        }
    }
}
