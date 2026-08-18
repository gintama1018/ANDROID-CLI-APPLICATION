package com.gintama.nlcli.media

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import com.gintama.nlcli.model.ExecutionResult
import com.gintama.nlcli.util.Logger

class MediaController(private val context: Context) {

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    fun playPause(): ExecutionResult {
        return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "Play/Pause toggled")
    }

    fun play(): ExecutionResult {
        return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY, "Playback started")
    }

    fun pause(): ExecutionResult {
        return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE, "Playback paused")
    }

    fun next(): ExecutionResult {
        return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT, "Next track")
    }

    fun previous(): ExecutionResult {
        return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "Previous track")
    }

    private fun sendMediaKeyEvent(keyCode: Int, successMessage: String): ExecutionResult {
        val am = audioManager ?: return ExecutionResult(false, "Audio service unavailable")
        return try {
            val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            am.dispatchMediaKeyEvent(downEvent)
            am.dispatchMediaKeyEvent(upEvent)
            Logger.i("Dispatched media key: $keyCode ($successMessage)")
            ExecutionResult(
                success = true,
                message = "$successMessage 🎵"
            )
        } catch (e: Exception) {
            Logger.e("Failed to dispatch media key event", e)
            ExecutionResult(false, "Failed to control media: ${e.localizedMessage}")
        }
    }
}
