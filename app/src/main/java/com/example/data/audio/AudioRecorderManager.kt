package com.example.data.audio

import android.content.Context
import java.io.File

/**
 * Manager wrapper around [BasicAudioRecorder], implementing the [AudioRecorder] interface.
 */
class AudioRecorderManager(private val context: Context) : AudioRecorder {

    private val delegate = BasicAudioRecorder(context)

    override val isRecording: Boolean
        get() = delegate.isRecording

    override val isPaused: Boolean
        get() = delegate.isPaused

    override val currentOutputFile: File?
        get() = delegate.currentOutputFile

    override fun start(outputFile: File): Boolean {
        return delegate.start(outputFile)
    }

    override fun startInInternalStorage(fileName: String?): File? {
        return delegate.startInInternalStorage(fileName)
    }

    override fun pause() {
        delegate.pause()
    }

    override fun resume() {
        delegate.resume()
    }

    override fun getMaxAmplitude(): Int {
        return delegate.getMaxAmplitude()
    }

    override fun stop(): File? {
        return delegate.stop()
    }

    override fun cancel() {
        delegate.cancel()
    }

    override fun getInternalStorageDir(): File {
        return delegate.getInternalStorageDir()
    }
}
