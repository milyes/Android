package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.audio.BasicAudioRecorder
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudioRecorderTest {

    @Test
    fun `internal storage directory exists and is created`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val recorder = BasicAudioRecorder(context)
        val dir = recorder.getInternalStorageDir()
        assertTrue(dir.exists())
        assertTrue(dir.isDirectory)
        assertEquals("recordings", dir.name)
    }

    @Test
    fun `recorder handles initial state correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val recorder = BasicAudioRecorder(context)
        assertFalse(recorder.isRecording)
        assertFalse(recorder.isPaused)
        assertNull(recorder.currentOutputFile)
        assertEquals(0, recorder.getMaxAmplitude())
    }

    @Test
    fun `cancel deletes file when called`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val recorder = BasicAudioRecorder(context)
        val testFile = File(recorder.getInternalStorageDir(), "test_temp.m4a")
        testFile.writeText("sample audio data")
        assertTrue(testFile.exists())

        // Start sets currentOutputFile
        recorder.start(testFile)
        recorder.cancel()

        assertFalse(testFile.exists())
        assertFalse(recorder.isRecording)
    }
}
