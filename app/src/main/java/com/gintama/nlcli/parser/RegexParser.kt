package com.gintama.nlcli.parser

import com.gintama.nlcli.model.ActionType
import com.gintama.nlcli.model.AppType
import com.gintama.nlcli.model.Command
import com.gintama.nlcli.model.ParseSource
import com.gintama.nlcli.model.ParserResult
import java.util.regex.Pattern

class RegexParser : CommandParser {

    private data class PatternRule(
        val pattern: Pattern,
        val app: AppType,
        val action: ActionType,
        val contactGroup: String? = null,
        val payloadGroup: String? = null,
        val isDryRun: Boolean = false
    )

    private val rules = listOf(
        // System / Utility commands
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:help|\\?|commands)\\s*$"),
            app = AppType.SYSTEM,
            action = ActionType.HELP
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:clear|cls)\\s*$"),
            app = AppType.SYSTEM,
            action = ActionType.CLEAR
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:history|logs)\\s*$"),
            app = AppType.SYSTEM,
            action = ActionType.HISTORY
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:status|info|ping)\\s*$"),
            app = AppType.SYSTEM,
            action = ActionType.STATUS
        ),

        // Dry run prefix
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:dryrun|dry-run|dry\\s+run)\\s+(?<payload>.+)$"),
            app = AppType.SYSTEM,
            action = ActionType.DRY_RUN,
            payloadGroup = "payload",
            isDryRun = true
        ),

        // 2.1 Torch / Flashlight
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:torch|flashlight)\\s+(?:on|enable|1)$"),
            app = AppType.SYSTEM,
            action = ActionType.TORCH,
            payloadGroup = null
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:torch|flashlight)\\s+(?:off|disable|0)$"),
            app = AppType.SYSTEM,
            action = ActionType.TORCH,
            payloadGroup = null
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:toggle\\s+(?:torch|flashlight)|torch|flashlight)\\s*$"),
            app = AppType.SYSTEM,
            action = ActionType.TORCH,
            payloadGroup = null
        ),

        // 2.2 Volume & Sound Profile
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:volume|vol)\\s+(?<val>\\d{1,3})%?\\s*$"),
            app = AppType.SYSTEM,
            action = ActionType.VOLUME,
            payloadGroup = "val"
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:max\\s+volume|volume\\s+max)\\s*$"),
            app = AppType.SYSTEM,
            action = ActionType.VOLUME
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:volume|vol)\\s+(?:up|\\+)\\s*$"),
            app = AppType.SYSTEM,
            action = ActionType.VOLUME
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:volume|vol)\\s+(?:down|\\-)\\s*$"),
            app = AppType.SYSTEM,
            action = ActionType.VOLUME
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:mute|silence)\\s*$"),
            app = AppType.SYSTEM,
            action = ActionType.VOLUME
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:silent\\s+mode|silent)\\s*$"),
            app = AppType.SYSTEM,
            action = ActionType.VOLUME
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:vibrate\\s+mode|vibrate)\\s*$"),
            app = AppType.SYSTEM,
            action = ActionType.VOLUME
        ),

        // 2.3 Diagnostics
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*battery\\s*$"),
            app = AppType.SYSTEM,
            action = ActionType.BATTERY
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*storage\\s*$"),
            app = AppType.SYSTEM,
            action = ActionType.STORAGE
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:device\\s+info|deviceinfo)\\s*$"),
            app = AppType.SYSTEM,
            action = ActionType.DEVICE_INFO
        ),

        // 2.4 Math Evaluator: "calc 2^10 + 50" or "calculate (450 * 18) / 100"
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:calc|calculate|eval)\\s+(?<expr>.+)$"),
            app = AppType.UTILITY,
            action = ActionType.CALC,
            payloadGroup = "expr"
        ),

        // 2.5 Unit Converter: "convert 5 miles to km" / "convert 100 f to c"
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*convert\\s+(?<val>[\\d\\.]+)\\s+(?<from>[a-zA-Z/°]+)\\s+(?:to|in)\\s+(?<to>[a-zA-Z/°]+)\\s*$"),
            app = AppType.UTILITY,
            action = ActionType.CONVERT,
            contactGroup = "from",
            payloadGroup = "to"
        ),

        // 2.6 Notes & Todos
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*note\\s+delete\\s+(?<id>\\d+)\\s*$"),
            app = AppType.NOTES,
            action = ActionType.NOTE,
            payloadGroup = "id"
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*notes\\s+clear\\s*$"),
            app = AppType.NOTES,
            action = ActionType.NOTE
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:notes\\s+list|notes)\\s*$"),
            app = AppType.NOTES,
            action = ActionType.NOTE
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*note\\s+(?<text>.+)$"),
            app = AppType.NOTES,
            action = ActionType.NOTE,
            payloadGroup = "text"
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*todo\\s+done\\s+(?<id>\\d+)\\s*$"),
            app = AppType.TODOS,
            action = ActionType.TODO,
            payloadGroup = "id"
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*todo\\s+delete\\s+(?<id>\\d+)\\s*$"),
            app = AppType.TODOS,
            action = ActionType.TODO,
            payloadGroup = "id"
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*todos\\s+clear\\s*$"),
            app = AppType.TODOS,
            action = ActionType.TODO
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:todos\\s+list|todos|todo\\s+list)\\s*$"),
            app = AppType.TODOS,
            action = ActionType.TODO
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*todo\\s+(?<task>.+)$"),
            app = AppType.TODOS,
            action = ActionType.TODO,
            payloadGroup = "task"
        ),

        // 2.7 Dev Tools: uuid, sha256, base64, copy, paste, ip
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*uuid\\s*$"),
            app = AppType.UTILITY,
            action = ActionType.DEV_TOOL
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*sha256\\s+(?<text>.+)$"),
            app = AppType.UTILITY,
            action = ActionType.DEV_TOOL,
            payloadGroup = "text"
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*base64\\s+decode\\s+(?<text>.+)$"),
            app = AppType.UTILITY,
            action = ActionType.DEV_TOOL,
            payloadGroup = "text"
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*base64\\s+(?:encode\\s+)?(?<text>.+)$"),
            app = AppType.UTILITY,
            action = ActionType.DEV_TOOL,
            payloadGroup = "text"
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*copy\\s+(?<text>.+)$"),
            app = AppType.UTILITY,
            action = ActionType.DEV_TOOL,
            payloadGroup = "text"
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*paste\\s*$"),
            app = AppType.UTILITY,
            action = ActionType.DEV_TOOL
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:ip|local\\s+ip)\\s*$"),
            app = AppType.UTILITY,
            action = ActionType.DEV_TOOL
        ),

        // 3.1 Alarms & Timers
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:show\\s+alarms|alarms)\\s*$"),
            app = AppType.CLOCK,
            action = ActionType.ALARM
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:set\\s+)?alarm\\s+(?:for\\s+|at\\s+)?(?<time>\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)(?:\\s*[:\\-]?\\s*(?<msg>.+))?$"),
            app = AppType.CLOCK,
            action = ActionType.ALARM,
            contactGroup = "time",
            payloadGroup = "msg"
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:set\\s+)?timer\\s+(?:for\\s+)?(?<duration>\\d+\\s*(?:m|min|mins|minutes|s|sec|secs|seconds|h|hour|hours)?)(?:\\s*[:\\-]?\\s*(?<msg>.+))?$"),
            app = AppType.CLOCK,
            action = ActionType.TIMER,
            contactGroup = "duration",
            payloadGroup = "msg"
        ),

        // 3.3 Media Controls: play, pause, next, prev
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:play\\s+music|play|resume)\\s*$"),
            app = AppType.MEDIA,
            action = ActionType.MEDIA
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:pause\\s+music|pause|stop\\s+music)\\s*$"),
            app = AppType.MEDIA,
            action = ActionType.MEDIA
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:next\\s+song|next|next\\s+track)\\s*$"),
            app = AppType.MEDIA,
            action = ActionType.MEDIA
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:prev\\s+song|previous\\s+song|prev|previous)\\s*$"),
            app = AppType.MEDIA,
            action = ActionType.MEDIA
        ),

        // 3.4 Snippets: snippet upi = you@okhdfc
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*snippets\\s*$"),
            app = AppType.UTILITY,
            action = ActionType.SNIPPET
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*snippet\\s+delete\\s+(?<name>[a-zA-Z0-9_]+)\\s*$"),
            app = AppType.UTILITY,
            action = ActionType.SNIPPET,
            payloadGroup = "name"
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*snippet\\s+(?<name>[a-zA-Z0-9_]+)\\s*=\\s*(?<val>.+)$"),
            app = AppType.UTILITY,
            action = ActionType.SNIPPET,
            contactGroup = "name",
            payloadGroup = "val"
        ),

        // 3.5 Macros / Aliases: alias gm = torch off; volume 100; open spotify
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:aliases|alias\\s+list)\\s*$"),
            app = AppType.UTILITY,
            action = ActionType.MACRO
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*alias\\s+delete\\s+(?<name>[a-zA-Z0-9_]+)\\s*$"),
            app = AppType.UTILITY,
            action = ActionType.MACRO,
            payloadGroup = "name"
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*alias\\s+(?<name>[a-zA-Z0-9_]+)\\s*=\\s*(?<seq>.+)$"),
            app = AppType.UTILITY,
            action = ActionType.MACRO,
            contactGroup = "name",
            payloadGroup = "seq"
        ),

        // WhatsApp: "send whatsapp to <contact>: <message>" or "send wa to <contact> - <message>"
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*send\\s+(?:whatsapp|wa)\\s+to\\s+(?<contact>[^:\\-]+?)[:\\-]\\s*(?<message>.+)$"),
            app = AppType.WHATSAPP,
            action = ActionType.SEND_MESSAGE,
            contactGroup = "contact",
            payloadGroup = "message"
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:whatsapp|wa)\\s+to\\s+(?<contact>[^:\\-]+?)[:\\-]\\s*(?<message>.+)$"),
            app = AppType.WHATSAPP,
            action = ActionType.SEND_MESSAGE,
            contactGroup = "contact",
            payloadGroup = "message"
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:whatsapp|wa)\\s+(?<contact>[^:\\-]+?)[:\\-]\\s*(?<message>.+)$"),
            app = AppType.WHATSAPP,
            action = ActionType.SEND_MESSAGE,
            contactGroup = "contact",
            payloadGroup = "message"
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*send\\s+(?<contact>[^:\\-]+?)\\s+on\\s+(?:whatsapp|wa)[:\\-]\\s*(?<message>.+)$"),
            app = AppType.WHATSAPP,
            action = ActionType.SEND_MESSAGE,
            contactGroup = "contact",
            payloadGroup = "message"
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:whatsapp|wa)\\s+(?<contact>[a-zA-Z0-9+_]+)\\s+(?<message>.+)$"),
            app = AppType.WHATSAPP,
            action = ActionType.SEND_MESSAGE,
            contactGroup = "contact",
            payloadGroup = "message"
        ),

        // SMS: "send sms to <contact>: <message>" or "send text to <contact> - <message>"
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*send\\s+(?:sms|text|message)\\s+to\\s+(?<contact>[^:\\-]+?)[:\\-]\\s*(?<message>.+)$"),
            app = AppType.SMS,
            action = ActionType.SEND_MESSAGE,
            contactGroup = "contact",
            payloadGroup = "message"
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*sms\\s+to\\s+(?<contact>[^:\\-]+?)[:\\-]\\s*(?<message>.+)$"),
            app = AppType.SMS,
            action = ActionType.SEND_MESSAGE,
            contactGroup = "contact",
            payloadGroup = "message"
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*sms\\s+(?<contact>[^:\\-]+?)[:\\-]\\s*(?<message>.+)$"),
            app = AppType.SMS,
            action = ActionType.SEND_MESSAGE,
            contactGroup = "contact",
            payloadGroup = "message"
        ),
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*sms\\s+(?<contact>[a-zA-Z0-9+_]+)\\s+(?<message>.+)$"),
            app = AppType.SMS,
            action = ActionType.SEND_MESSAGE,
            contactGroup = "contact",
            payloadGroup = "message"
        ),

        // Phone Call: "call <contact>" or "dial <contact>"
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:call|dial|phone)\\s+(?<contact>.+)$"),
            app = AppType.PHONE,
            action = ActionType.CALL,
            contactGroup = "contact"
        ),

        // App Launching: "open <app>" / "launch <app>" / "start <app>"
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:open|launch|start|run)\\s+(?<appname>.+)$"),
            app = AppType.SYSTEM,
            action = ActionType.OPEN_APP,
            payloadGroup = "appname"
        ),

        // Search: "search <query>"
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:search|google|find)\\s+(?<query>.+)$"),
            app = AppType.SYSTEM,
            action = ActionType.SEARCH,
            payloadGroup = "query"
        )
    )

    override suspend fun parse(input: String): ParserResult {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return ParserResult.Failure("Empty command entered", "Type 'help' for a list of commands")
        }

        for (rule in rules) {
            val matcher = rule.pattern.matcher(trimmed)
            if (matcher.matches()) {
                val contact = rule.contactGroup?.let {
                    try { matcher.group(it)?.trim() } catch (_: Exception) { null }
                }
                val payload = rule.payloadGroup?.let {
                    try { matcher.group(it)?.trim() } catch (_: Exception) { null }
                }

                // If this is a dry run command, parse the inner command
                if (rule.isDryRun && payload != null) {
                    val innerResult = parse(payload)
                    return when (innerResult) {
                        is ParserResult.Success -> {
                            ParserResult.Success(
                                innerResult.command.copy(
                                    action = ActionType.DRY_RUN,
                                    payload = innerResult.command.payload,
                                    rawInput = trimmed
                                )
                            )
                        }
                        else -> innerResult
                    }
                }

                // Custom parsing for unit converter to extract value
                val finalContact = if (rule.action == ActionType.CONVERT) {
                    try {
                        val v = matcher.group("val")
                        val f = matcher.group("from")
                        "$v $f"
                    } catch (_: Exception) { contact }
                } else contact

                val command = Command(
                    app = rule.app,
                    action = rule.action,
                    contact = finalContact,
                    payload = payload,
                    rawInput = trimmed,
                    source = ParseSource.REGEX,
                    confidence = 1.0f
                )
                return ParserResult.Success(command)
            }
        }

        return ParserResult.Failure(
            reason = "Command not recognized",
            suggestion = "Type 'help' to see all supported offline commands (Torch, Volume, Notes, Todos, Calc, Alarms, WhatsApp, etc.)"
        )
    }
}
