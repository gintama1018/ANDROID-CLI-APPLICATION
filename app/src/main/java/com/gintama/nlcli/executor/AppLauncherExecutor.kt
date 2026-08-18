package com.gintama.nlcli.executor

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.gintama.nlcli.contacts.ContactResolver
import com.gintama.nlcli.model.Command
import com.gintama.nlcli.model.ExecutionResult
import com.gintama.nlcli.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppLauncherExecutor(
    private val context: Context
) : ICommandExecutor {

    override suspend fun execute(command: Command): ExecutionResult = withContext(Dispatchers.IO) {
        val appQuery = command.payload?.trim()

        if (appQuery.isNullOrBlank()) {
            return@withContext ExecutionResult(
                success = false,
                message = "Missing app name to launch",
                details = "Syntax: open <app name>"
            )
        }

        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        // Find match by exact label or package name
        var bestMatchPkg: String? = null
        var bestMatchLabel: String? = null
        var highestScore = 0.0f

        val cleanQuery = appQuery.lowercase()

        // Known aliases mapping
        val commonAliases = mapOf(
            "yt" to "youtube",
            "calc" to "calculator",
            "msg" to "messages",
            "browser" to "chrome",
            "photos" to "gallery",
            "cam" to "camera"
        )
        val expandedQuery = commonAliases[cleanQuery] ?: cleanQuery

        for (app in installedApps) {
            // Filter non-launchable apps
            if (pm.getLaunchIntentForPackage(app.packageName) == null) continue

            val label = pm.getApplicationLabel(app).toString().lowercase()
            val pkg = app.packageName.lowercase()

            if (label == expandedQuery || label == cleanQuery || pkg == expandedQuery) {
                bestMatchPkg = app.packageName
                bestMatchLabel = pm.getApplicationLabel(app).toString()
                highestScore = 1.0f
                break
            }

            if (label.startsWith(expandedQuery) || label.contains(expandedQuery)) {
                val score = 0.9f
                if (score > highestScore) {
                    highestScore = score
                    bestMatchPkg = app.packageName
                    bestMatchLabel = pm.getApplicationLabel(app).toString()
                }
            } else {
                val similarity = ContactResolver.calculateSimilarity(expandedQuery, label)
                if (similarity > 0.65f && similarity > highestScore) {
                    highestScore = similarity
                    bestMatchPkg = app.packageName
                    bestMatchLabel = pm.getApplicationLabel(app).toString()
                }
            }
        }

        if (bestMatchPkg != null) {
            try {
                val launchIntent = pm.getLaunchIntentForPackage(bestMatchPkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return@withContext ExecutionResult(
                        success = true,
                        message = "Launching $bestMatchLabel ($bestMatchPkg)"
                    )
                }
            } catch (e: Exception) {
                Logger.e("Failed to launch application $bestMatchPkg", e)
                return@withContext ExecutionResult(
                    success = false,
                    message = "Failed to launch $bestMatchLabel: ${e.localizedMessage}"
                )
            }
        }

        ExecutionResult(
            success = false,
            message = "Could not find any installed app matching '$appQuery'",
            details = "Ensure the app is installed and launchable."
        )
    }
}
