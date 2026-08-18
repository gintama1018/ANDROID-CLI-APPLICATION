package com.gintama.nlcli.system

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import com.gintama.nlcli.model.ExecutionResult
import com.gintama.nlcli.util.Logger

class AudioController(private val context: Context) {

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    }

    fun setVolumePercent(percent: Int): ExecutionResult {
        val am = audioManager ?: return ExecutionResult(false, "Audio service unavailable")
        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val clampedPercent = percent.coerceIn(0, 100)
        val targetVol = ((clampedPercent / 100.0) * maxVol).toInt().coerceIn(0, maxVol)

        am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI)
        Logger.i("Volume set to $clampedPercent% (level $targetVol/$maxVol)")
        return ExecutionResult(
            success = true,
            message = "Media volume set to $clampedPercent% ($targetVol/$maxVol)"
        )
    }

    fun adjustVolume(direction: Int): ExecutionResult {
        val am = audioManager ?: return ExecutionResult(false, "Audio service unavailable")
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val percent = (current * 100) / max
        return ExecutionResult(
            success = true,
            message = "Media volume adjusted to $percent% ($current/$max)"
        )
    }

    fun setMaxVolume(): ExecutionResult {
        return setVolumePercent(100)
    }

    fun setMute(): ExecutionResult {
        return setVolumePercent(0)
    }

    fun setRingerMode(mode: Int, modeLabel: String): ExecutionResult {
        val am = audioManager ?: return ExecutionResult(false, "Audio service unavailable")
        val nm = notificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && nm != null) {
            if (!nm.isNotificationPolicyAccessGranted) {
                return ExecutionResult(
                    success = false,
                    message = "Do Not Disturb (Notification Policy) permission required for $modeLabel mode",
                    details = "Grant 'Do Not Disturb access' in Settings to change ringer mode programmatically."
                )
            }
        }

        return try {
            am.ringerMode = mode
            ExecutionResult(
                success = true,
                message = "Sound profile set to $modeLabel"
            )
        } catch (e: Exception) {
            Logger.e("Failed to set ringer mode", e)
            ExecutionResult(
                success = false,
                message = "Failed to set $modeLabel mode: ${e.localizedMessage}"
            )
        }
    }
}
