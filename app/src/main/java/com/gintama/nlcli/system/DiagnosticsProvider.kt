package com.gintama.nlcli.system

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.gintama.nlcli.model.ExecutionResult
import java.io.File
import java.util.Locale

class DiagnosticsProvider(private val context: Context) {

    fun getBatteryDiagnostics(): ExecutionResult {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (batteryIntent == null) {
            return ExecutionResult(false, "Could not retrieve battery information")
        }

        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else -1

        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val chargePlug = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val plugType = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Dock"
            else -> if (isCharging) "Connected" else "Discharging"
        }

        val tempTenths = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val tempCelsius = if (tempTenths > 0) tempTenths / 10.0 else 0.0

        val health = when (batteryIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            else -> "Normal"
        }

        val details = buildString {
            appendLine("════════════ BATTERY STATUS ════════════")
            appendLine(" Charge Level : $batteryPct%")
            appendLine(" Power State  : ${if (isCharging) "Charging ($plugType)" else "On Battery"}")
            appendLine(" Temperature  : ${String.format(Locale.US, "%.1f", tempCelsius)}°C")
            appendLine(" Health       : $health")
            append("════════════════════════════════════════")
        }

        return ExecutionResult(
            success = true,
            message = "Battery: $batteryPct% (${if (isCharging) "Charging" else "Discharging"}, ${String.format(Locale.US, "%.1f", tempCelsius)}°C)",
            details = details
        )
    }

    fun getStorageDiagnostics(): ExecutionResult {
        return try {
            val path: File = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = totalBytes - freeBytes

            val totalGB = totalBytes / (1024.0 * 1024.0 * 1024.0)
            val freeGB = freeBytes / (1024.0 * 1024.0 * 1024.0)
            val usedGB = usedBytes / (1024.0 * 1024.0 * 1024.0)
            val usedPercent = if (totalBytes > 0) ((usedBytes * 100) / totalBytes).toInt() else 0

            val details = buildString {
                appendLine("════════════ STORAGE STATS ═════════════")
                appendLine(String.format(Locale.US, " Total Space : %.2f GB", totalGB))
                appendLine(String.format(Locale.US, " Used Space  : %.2f GB (%d%%)", usedGB, usedPercent))
                appendLine(String.format(Locale.US, " Free Space  : %.2f GB", freeGB))
                append("════════════════════════════════════════")
            }

            ExecutionResult(
                success = true,
                message = String.format(Locale.US, "Storage: %.2f GB free of %.2f GB (%d%% used)", freeGB, totalGB, usedPercent),
                details = details
            )
        } catch (e: Exception) {
            ExecutionResult(false, "Failed to read storage stats: ${e.localizedMessage}")
        }
    }

    fun getDeviceInfo(): ExecutionResult {
        val details = buildString {
            appendLine("════════════ DEVICE INFO ═══════════════")
            appendLine(" Manufacturer : ${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }}")
            appendLine(" Model        : ${Build.MODEL}")
            appendLine(" Device Brand : ${Build.BRAND}")
            appendLine(" Android Ver  : ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine(" Build ID     : ${Build.ID}")
            append("════════════════════════════════════════")
        }

        return ExecutionResult(
            success = true,
            message = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})",
            details = details
        )
    }
}
