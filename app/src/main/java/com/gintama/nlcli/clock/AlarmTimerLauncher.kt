package com.gintama.nlcli.clock

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.gintama.nlcli.model.ExecutionResult
import com.gintama.nlcli.util.Logger

class AlarmTimerLauncher(private val context: Context) {

    fun setAlarm(hour: Int, minute: Int, message: String = "Alarm"): ExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Logger.i("Dispatched alarm intent for %02d:%02d".format(hour, minute))
            ExecutionResult(
                success = true,
                message = "Alarm scheduled for %02d:%02d ('%s')".format(hour, minute, message),
                details = "Opened clock intent (EXTRA_SKIP_UI enabled; some OEM clocks may show confirmation UI)"
            )
        } catch (e: Exception) {
            Logger.e("Failed to set alarm", e)
            ExecutionResult(false, "Failed to set alarm: ${e.localizedMessage}")
        }
    }

    fun setTimer(seconds: Int, message: String = "Timer"): ExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            val mins = seconds / 60
            val secs = seconds % 60
            val formatted = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
            ExecutionResult(
                success = true,
                message = "Timer started for $formatted ('$message')",
                details = "Opened clock timer intent"
            )
        } catch (e: Exception) {
            Logger.e("Failed to set timer", e)
            ExecutionResult(false, "Failed to set timer: ${e.localizedMessage}")
        }
    }

    fun showAlarms(): ExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ExecutionResult(
                success = true,
                message = "Opened system Alarms view"
            )
        } catch (e: Exception) {
            ExecutionResult(false, "Could not open alarms: ${e.localizedMessage}")
        }
    }
}
