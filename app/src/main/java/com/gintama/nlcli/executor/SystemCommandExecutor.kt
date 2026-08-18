package com.gintama.nlcli.executor

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import com.gintama.nlcli.accessibility.NLCliAccessibilityService
import com.gintama.nlcli.clock.AlarmTimerLauncher
import com.gintama.nlcli.contacts.ContactResolutionResult
import com.gintama.nlcli.contacts.ContactResolver
import com.gintama.nlcli.data.AppDatabase
import com.gintama.nlcli.data.dao.CommandHistoryDao
import com.gintama.nlcli.data.entity.MacroEntity
import com.gintama.nlcli.data.entity.NoteEntity
import com.gintama.nlcli.data.entity.SnippetEntity
import com.gintama.nlcli.data.entity.TodoEntity
import com.gintama.nlcli.media.MediaController
import com.gintama.nlcli.model.ActionType
import com.gintama.nlcli.model.Command
import com.gintama.nlcli.model.ExecutionResult
import com.gintama.nlcli.system.AudioController
import com.gintama.nlcli.system.DiagnosticsProvider
import com.gintama.nlcli.system.TorchController
import com.gintama.nlcli.utility.DevToolsExecutor
import com.gintama.nlcli.utility.MathEvaluator
import com.gintama.nlcli.utility.UnitConverter
import com.gintama.nlcli.util.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SystemCommandExecutor(
    private val context: Context,
    private val contactResolver: ContactResolver,
    private val historyDao: CommandHistoryDao
) : ICommandExecutor {

    private val db by lazy { AppDatabase.getInstance(context) }
    private val torchController by lazy { TorchController(context) }
    private val audioController by lazy { AudioController(context) }
    private val diagnosticsProvider by lazy { DiagnosticsProvider(context) }
    private val mathEvaluator by lazy { MathEvaluator() }
    private val unitConverter by lazy { UnitConverter() }
    private val devTools by lazy { DevToolsExecutor(context) }
    private val alarmLauncher by lazy { AlarmTimerLauncher(context) }
    private val mediaController by lazy { MediaController(context) }

    override suspend fun execute(command: Command): ExecutionResult = withContext(Dispatchers.IO) {
        when (command.action) {
            ActionType.HELP -> {
                val helpText = buildString {
                    appendLine("═════════════ NLCLI v2.0 COMMANDS (100% OFFLINE) ═════════════")
                    appendLine("• WhatsApp:")
                    appendLine("  send whatsapp to <contact>: <message>")
                    appendLine("  whatsapp <contact>: <message>")
                    appendLine("")
                    appendLine("• Phone & SMS:")
                    appendLine("  call <contact>  |  send sms to <contact>: <message>")
                    appendLine("")
                    appendLine("• System Controls & Hardware:")
                    appendLine("  torch on | torch off | toggle torch")
                    appendLine("  volume <0-100> | volume up | volume down | mute | silent mode")
                    appendLine("  battery | storage | device info")
                    appendLine("")
                    appendLine("• Math & Conversions:")
                    appendLine("  calc <expression> (e.g. calc (450 * 18) / 100 | calc 2^10 + 50)")
                    appendLine("  convert <val> <from> to <to> (e.g. convert 5 miles to km)")
                    appendLine("")
                    appendLine("• Notes & Todos (Room DB):")
                    appendLine("  note <text> | notes | note delete <id> | notes clear")
                    appendLine("  todo <task> | todos | todo done <id> | todos clear")
                    appendLine("")
                    appendLine("• Developer & Clipboard Tools:")
                    appendLine("  uuid | sha256 <text> | base64 <text> | copy <text> | paste | ip")
                    appendLine("")
                    appendLine("• Alarms, Timers & Media:")
                    appendLine("  alarm 7:00 am | timer 10 mins | show alarms")
                    appendLine("  play | pause | next song | prev")
                    appendLine("")
                    appendLine("• Aliases & Snippets:")
                    appendLine("  alias <name> = <cmd1>; <cmd2>")
                    appendLine("  snippet <name> = <value>")
                    appendLine("──────────────────────────────────────────────────────────────")
                }
                ExecutionResult(
                    success = true,
                    message = helpText,
                    details = "Chain multiple commands with ';' (e.g. torch on; volume 50; open spotify)"
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
                val hasAudio = PermissionHelper.hasRecordAudioPermission(context)
                val historyCount = historyDao.getCount()

                val statusText = buildString {
                    appendLine("═════════════ NLCLI DIAGNOSTICS ═════════════")
                    appendLine("• Accessibility Automation : ${if (isAccessibilityOn) "✅ ENABLED" else "❌ DISABLED (Settings > Accessibility)"}")
                    appendLine("• Service Process State    : ${if (isServiceActive) "🟢 ACTIVE" else "⚪ IDLE"}")
                    appendLine("• Contacts Permission      : ${if (hasContacts) "✅ GRANTED" else "❌ MISSING"}")
                    appendLine("• Audio/Mic Permission     : ${if (hasAudio) "✅ GRANTED" else "❌ MISSING"}")
                    appendLine("• SMS Permission           : ${if (hasSms) "✅ GRANTED" else "⚠️ OPTIONAL"}")
                    appendLine("• Phone Permission         : ${if (hasCall) "✅ GRANTED" else "⚠️ OPTIONAL"}")
                    appendLine("• Commands Executed        : $historyCount")
                    appendLine("• Network Status           : 🔒 100% OFFLINE (No INTERNET declared)")
                    append("─────────────────────────────────────────────")
                }

                ExecutionResult(
                    success = true,
                    message = statusText
                )
            }

            // 2.1 Torch
            ActionType.TORCH -> {
                val mode = command.payload?.lowercase()?.trim() ?: "toggle"
                when (mode) {
                    "on", "1", "enable" -> torchController.setTorch(true)
                    "off", "0", "disable" -> torchController.setTorch(false)
                    else -> torchController.toggleTorch()
                }
            }

            // 2.2 Volume
            ActionType.VOLUME -> {
                val target = command.payload?.lowercase()?.trim() ?: "up"
                when {
                    target.all { it.isDigit() } -> audioController.setVolumePercent(target.toInt())
                    target == "up" -> audioController.adjustVolume(AudioManager.ADJUST_RAISE)
                    target == "down" -> audioController.adjustVolume(AudioManager.ADJUST_LOWER)
                    target == "mute" -> audioController.setMute()
                    target == "silent" -> audioController.setRingerMode(AudioManager.RINGER_MODE_SILENT, "Silent")
                    target == "vibrate" -> audioController.setRingerMode(AudioManager.RINGER_MODE_VIBRATE, "Vibrate")
                    else -> audioController.adjustVolume(AudioManager.ADJUST_RAISE)
                }
            }

            // 2.3 Diagnostics
            ActionType.BATTERY -> diagnosticsProvider.getBatteryDiagnostics()
            ActionType.STORAGE -> diagnosticsProvider.getStorageDiagnostics()
            ActionType.DEVICE_INFO -> diagnosticsProvider.getDeviceInfo()

            // 2.4 Math Evaluator
            ActionType.CALC -> {
                val expr = command.payload?.trim() ?: ""
                try {
                    val result = mathEvaluator.evaluate(expr)
                    ExecutionResult(
                        success = true,
                        message = "$expr = $result"
                    )
                } catch (e: Exception) {
                    ExecutionResult(
                        success = false,
                        message = "Math Error: ${e.localizedMessage}"
                    )
                }
            }

            // 2.5 Unit Converter
            ActionType.CONVERT -> {
                try {
                    val parts = command.contact?.split(" ") ?: emptyList()
                    val value = parts.getOrNull(0)?.toDoubleOrNull() ?: 1.0
                    val fromUnit = parts.getOrNull(1) ?: "km"
                    val toUnit = command.payload?.trim() ?: "mi"

                    val converted = unitConverter.convert(value, fromUnit, toUnit)
                    ExecutionResult(
                        success = true,
                        message = converted
                    )
                } catch (e: Exception) {
                    ExecutionResult(
                        success = false,
                        message = "Conversion Error: ${e.localizedMessage}"
                    )
                }
            }

            // 2.6 Notes
            ActionType.NOTE -> {
                val noteDao = db.noteDao()
                val payload = command.payload?.trim() ?: ""

                when {
                    payload.equals("list", ignoreCase = true) || payload.isBlank() -> {
                        val notes = noteDao.getAllNotes()
                        if (notes.isEmpty()) {
                            ExecutionResult(true, "No notes found. Create one: 'note buy milk'")
                        } else {
                            val timeFormatter = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
                            val listText = buildString {
                                appendLine("📝 NOTES (${notes.size}):")
                                notes.forEach { n ->
                                    appendLine("[#${n.id}] ${n.text} (${timeFormatter.format(Date(n.timestampMs))})")
                                }
                            }
                            ExecutionResult(true, listText.trimEnd())
                        }
                    }
                    payload.equals("clear", ignoreCase = true) -> {
                        val count = noteDao.clearAll()
                        ExecutionResult(true, "Cleared $count notes")
                    }
                    payload.startsWith("delete:") -> {
                        val id = payload.removePrefix("delete:").toLongOrNull() ?: 0L
                        val deleted = noteDao.deleteById(id)
                        if (deleted > 0) ExecutionResult(true, "Deleted note #$id")
                        else ExecutionResult(false, "Note #$id not found")
                    }
                    else -> {
                        val id = noteDao.insert(NoteEntity(text = payload))
                        ExecutionResult(true, "Saved note #$id: '$payload' 📝")
                    }
                }
            }

            // 2.6 Todos
            ActionType.TODO -> {
                val todoDao = db.todoDao()
                val payload = command.payload?.trim() ?: ""

                when {
                    payload.equals("list", ignoreCase = true) || payload.isBlank() -> {
                        val todos = todoDao.getAllTodos()
                        if (todos.isEmpty()) {
                            ExecutionResult(true, "No todos found. Add one: 'todo call doctor'")
                        } else {
                            val listText = buildString {
                                appendLine("✅ TODOS (${todos.size}):")
                                todos.forEach { t ->
                                    val status = if (t.isCompleted) "[✔]" else "[ ]"
                                    appendLine("$status #${t.id}: ${t.task}")
                                }
                            }
                            ExecutionResult(true, listText.trimEnd())
                        }
                    }
                    payload.equals("clear", ignoreCase = true) -> {
                        val count = todoDao.clearAll()
                        ExecutionResult(true, "Cleared $count todos")
                    }
                    payload.startsWith("done:") -> {
                        val id = payload.removePrefix("done:").toLongOrNull() ?: 0L
                        val updated = todoDao.markCompleted(id)
                        if (updated > 0) ExecutionResult(true, "Completed task #$id! 🎉")
                        else ExecutionResult(false, "Task #$id not found")
                    }
                    payload.startsWith("delete:") -> {
                        val id = payload.removePrefix("delete:").toLongOrNull() ?: 0L
                        val deleted = todoDao.deleteById(id)
                        if (deleted > 0) ExecutionResult(true, "Deleted task #$id")
                        else ExecutionResult(false, "Task #$id not found")
                    }
                    else -> {
                        val id = todoDao.insert(TodoEntity(task = payload))
                        ExecutionResult(true, "Added task #$id: '$payload' 📋")
                    }
                }
            }

            // 2.7 Dev Tools
            ActionType.DEV_TOOL -> {
                val action = command.contact ?: command.payload ?: ""
                when {
                    command.payload == "uuid" || action == "uuid" -> devTools.generateUuid()
                    action == "sha256" -> devTools.generateSha256(command.payload ?: "")
                    action == "base64_encode" -> devTools.encodeBase64(command.payload ?: "")
                    action == "base64_decode" -> devTools.decodeBase64(command.payload ?: "")
                    action == "copy" -> devTools.copyText(command.payload ?: "")
                    command.payload == "paste" -> devTools.pasteText()
                    command.payload == "ip" -> devTools.getLocalIp()
                    else -> devTools.generateUuid()
                }
            }

            // 3.1 Alarms & Timers
            ActionType.ALARM -> {
                val payload = command.payload ?: ""
                if (payload == "show") {
                    alarmLauncher.showAlarms()
                } else {
                    val rawTime = command.contact ?: "07:00"
                    val label = command.payload ?: "Alarm"
                    val (hour, minute) = parseHourMinute(rawTime)
                    alarmLauncher.setAlarm(hour, minute, label)
                }
            }

            ActionType.TIMER -> {
                val rawDuration = command.contact ?: "60"
                val label = command.payload ?: "Timer"
                val seconds = parseDurationSeconds(rawDuration)
                alarmLauncher.setTimer(seconds, label)
            }

            // 3.3 Media
            ActionType.MEDIA -> {
                val action = command.payload?.lowercase()?.trim() ?: "play"
                when (action) {
                    "pause" -> mediaController.pause()
                    "next" -> mediaController.next()
                    "previous", "prev" -> mediaController.previous()
                    else -> mediaController.playPause()
                }
            }

            // 3.4 Snippets
            ActionType.SNIPPET -> {
                val snippetDao = db.snippetDao()
                val name = command.contact?.trim() ?: ""
                val value = command.payload?.trim() ?: ""

                when {
                    name.isNotBlank() && value.isNotBlank() -> {
                        snippetDao.insert(SnippetEntity(name = name, value = value))
                        ExecutionResult(true, "Saved snippet {$name} = '$value' 📌")
                    }
                    command.payload?.startsWith("delete:") == true -> {
                        val toDelete = command.payload.removePrefix("delete:").trim()
                        val count = snippetDao.deleteByName(toDelete)
                        if (count > 0) ExecutionResult(true, "Deleted snippet {$toDelete}")
                        else ExecutionResult(false, "Snippet {$toDelete} not found")
                    }
                    else -> {
                        val snippets = snippetDao.getAllSnippets()
                        if (snippets.isEmpty()) {
                            ExecutionResult(true, "No snippets found. Define one: 'snippet upi = user@bank'")
                        } else {
                            val listText = buildString {
                                appendLine("📌 SNIPPETS (${snippets.size}):")
                                snippets.forEach { s ->
                                    appendLine("  {${s.name}} = \"${s.value}\"")
                                }
                            }
                            ExecutionResult(true, listText.trimEnd())
                        }
                    }
                }
            }

            // 3.5 Macros / Aliases
            ActionType.MACRO -> {
                val macroDao = db.macroDao()
                val name = command.contact?.trim() ?: ""
                val seq = command.payload?.trim() ?: ""

                when {
                    name.isNotBlank() && seq.isNotBlank() -> {
                        macroDao.insert(MacroEntity(name = name, commandSequence = seq))
                        ExecutionResult(true, "Saved alias '$name' ➔ '$seq' ⚡")
                    }
                    command.payload?.startsWith("delete:") == true -> {
                        val toDelete = command.payload.removePrefix("delete:").trim()
                        val count = macroDao.deleteByName(toDelete)
                        if (count > 0) ExecutionResult(true, "Deleted alias '$toDelete'")
                        else ExecutionResult(false, "Alias '$toDelete} not found")
                    }
                    else -> {
                        val macros = macroDao.getAllMacros()
                        if (macros.isEmpty()) {
                            ExecutionResult(true, "No aliases found. Create one: 'alias gm = torch off; volume 100'")
                        } else {
                            val listText = buildString {
                                appendLine("⚡ ALIASES (${macros.size}):")
                                macros.forEach { m ->
                                    appendLine("  ${m.name} ➔ ${m.commandSequence}")
                                }
                            }
                            ExecutionResult(true, listText.trimEnd())
                        }
                    }
                }
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
                    }
                    if (command.payload != null) {
                        appendLine("  • Payload    : \"${command.payload}\"")
                    }
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

    private fun parseHourMinute(raw: String): Pair<Int, Int> {
        val clean = raw.lowercase().trim()
        val isPm = clean.contains("pm")
        val isAm = clean.contains("am")
        val digitsOnly = clean.replace("am", "").replace("pm", "").trim()

        val parts = digitsOnly.split(":")
        var hour = parts.getOrNull(0)?.toIntOrNull() ?: 7
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        if (isPm && hour < 12) hour += 12
        if (isAm && hour == 12) hour = 0

        return Pair(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    private fun parseDurationSeconds(raw: String): Int {
        val clean = raw.lowercase().trim()
        val num = clean.filter { it.isDigit() }.toIntOrNull() ?: 60
        return when {
            clean.contains("h") -> num * 3600
            clean.contains("m") -> num * 60
            clean.contains("s") -> num
            else -> num * 60 // Default to minutes if just number given
        }
    }
}
