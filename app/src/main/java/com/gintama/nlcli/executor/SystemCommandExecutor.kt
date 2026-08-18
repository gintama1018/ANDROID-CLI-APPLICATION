package com.gintama.nlcli.executor

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.gintama.nlcli.accessibility.NLCliAccessibilityService
import com.gintama.nlcli.contacts.ContactResolutionResult
import com.gintama.nlcli.contacts.ContactResolver
import com.gintama.nlcli.data.dao.CommandHistoryDao
import com.gintama.nlcli.model.ActionType
import com.gintama.nlcli.model.AppType
import com.gintama.nlcli.model.Command
import com.gintama.nlcli.model.ExecutionResult
import com.gintama.nlcli.util.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SystemCommandExecutor(
    private val context: Context,
    private val contactResolver: ContactResolver,
    private val historyDao: CommandHistoryDao
) : ICommandExecutor {

    override suspend fun execute(command: Command): ExecutionResult = withContext(Dispatchers.IO) {
        when (command.action) {
            ActionType.HELP -> {
                val helpText = buildString {
                    appendLine("NLCLI Commands Reference (100% Offline):")
                    appendLine("───────────────────────────────────────────────")
                    appendLine("• WhatsApp:")
                    appendLine("  send whatsapp to <contact>: <message>")
                    appendLine("  whatsapp <contact>: <message>")
                    appendLine("  wa <contact> <message>")
                    appendLine("")
                    appendLine("• Phone Call:")
                    appendLine("  call <contact>")
                    appendLine("  dial <phone number>")
                    appendLine("")
                    appendLine("• SMS:")
                    appendLine("  send sms to <contact>: <message>")
                    appendLine("  sms <contact>: <message>")
                    appendLine("")
                    appendLine("• App Launcher:")
                    appendLine("  open <app name>   (e.g., open YouTube, open Camera)")
                    appendLine("")
                    appendLine("• Utilities:")
                    appendLine("  dryrun <command>  (test parse without executing)")
                    appendLine("  status            (show permissions & service status)")
                    appendLine("  history           (view execution logs)")
                    appendLine("  clear             (clear terminal screen)")
                    appendLine("  help              (display this guide)")
                }
                ExecutionResult(
                    success = true,
                    message = helpText,
                    details = "Type any command or tap quick actions below."
                )
            }

            ActionType.CLEAR -> {
                ExecutionResult(
                    success = true,
                    message = "Terminal screen cleared.",
                    details = "CLEAR_SCREEN_ACTION"
                )
            }

            ActionType.STATUS -> {
                val isAccessibilityOn = PermissionHelper.isAccessibilityServiceEnabled(context)
                val isServiceActive = NLCliAccessibilityService.isServiceRunning
                val hasContacts = PermissionHelper.hasContactsPermission(context)
                val hasSms = PermissionHelper.hasSmsPermission(context)
                val hasCall = PermissionHelper.hasCallPermission(context)
                val isBattOptIgnored = PermissionHelper.isIgnoringBatteryOptimizations(context)
                val historyCount = historyDao.getCount()

                val statusText = buildString {
                    appendLine("NLCLI System Diagnostics:")
                    appendLine("───────────────────────────────────────────────")
                    appendLine("• Accessibility Service : ${if (isAccessibilityOn) "✅ ENABLED" else "❌ DISABLED (Settings > Accessibility)"}")
                    appendLine("• Service Process State : ${if (isServiceActive) "🟢 ACTIVE" else "⚪ IDLE"}")
                    appendLine("• Contacts Permission   : ${if (hasContacts) "✅ GRANTED" else "❌ MISSING"}")
                    appendLine("• SMS Permission        : ${if (hasSms) "✅ GRANTED" else "⚠️ OPTIONAL (opens SMS app)"}")
                    appendLine("• Phone Call Permission : ${if (hasCall) "✅ GRANTED" else "⚠️ OPTIONAL (opens dialer)"}")
                    appendLine("• Battery Optimization  : ${if (isBattOptIgnored) "✅ UNRESTRICTED" else "⚠️ OPTIMIZED (OEM may kill background)"}")
                    appendLine("• Total Commands Run    : $historyCount")
                }

                ExecutionResult(
                    success = true,
                    message = statusText
                )
            }

            ActionType.SEARCH -> {
                val query = command.payload?.trim() ?: ""
                try {
                    val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                        putExtra(SearchManager.QUERY, query)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ExecutionResult(
                        success = true,
                        message = "Searching web for '$query'..."
                    )
                } catch (_: Exception) {
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(browserIntent)
                    ExecutionResult(
                        success = true,
                        message = "Opened browser for '$query'"
                    )
                }
            }

            ActionType.DRY_RUN -> {
                val preview = buildString {
                    appendLine("[DRY RUN] Command Analyzed Successfully:")
                    appendLine("  • Target App : ${command.app.rawValue.uppercase()}")
                    appendLine("  • Action     : ${command.action.rawValue}")
                    if (command.contact != null) {
                        appendLine("  • Contact    : '${command.contact}'")
                        val resolution = contactResolver.resolveContact(command.contact)
                        when (resolution) {
                            is ContactResolutionResult.Found -> appendLine("  • Resolved To: ${resolution.contact.displayName} (${resolution.contact.normalizedPhoneNumber})")
                            is ContactResolutionResult.Ambiguous -> appendLine("  • Closest Match: ${resolution.bestMatch.displayName} (${resolution.bestMatch.normalizedPhoneNumber})")
                            is ContactResolutionResult.NotFound -> appendLine("  • Contact DB: Not found (would ask or use direct number)")
                            is ContactResolutionResult.PermissionDenied -> appendLine("  • Contact DB: Permission missing")
                        }
                    }
                    if (command.payload != null) {
                        appendLine("  • Payload    : \"${command.payload}\"")
                    }
                    appendLine("  • Parse Conf : ${(command.confidence * 100).toInt()}% via ${command.source}")
                    appendLine("  • Execution  : Simulated (No action taken on device)")
                }
                ExecutionResult(
                    success = true,
                    message = preview
                )
            }

            else -> {
                ExecutionResult(
                    success = false,
                    message = "Unknown system command"
                )
            }
        }
    }
}
