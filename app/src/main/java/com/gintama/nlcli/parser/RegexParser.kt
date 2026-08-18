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

        // WhatsApp: "send whatsapp to <contact>: <message>" or "send wa to <contact> - <message>"
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*send\\s+(?:whatsapp|wa)\\s+to\\s+(?<contact>[^:\\-]+?)[:\\-]\\s*(?<message>.+)$"),
            app = AppType.WHATSAPP,
            action = ActionType.SEND_MESSAGE,
            contactGroup = "contact",
            payloadGroup = "message"
        ),
        // WhatsApp: "whatsapp to <contact>: <message>"
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:whatsapp|wa)\\s+to\\s+(?<contact>[^:\\-]+?)[:\\-]\\s*(?<message>.+)$"),
            app = AppType.WHATSAPP,
            action = ActionType.SEND_MESSAGE,
            contactGroup = "contact",
            payloadGroup = "message"
        ),
        // WhatsApp: "whatsapp <contact>: <message>" or "whatsapp <contact> - <message>"
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*(?:whatsapp|wa)\\s+(?<contact>[^:\\-]+?)[:\\-]\\s*(?<message>.+)$"),
            app = AppType.WHATSAPP,
            action = ActionType.SEND_MESSAGE,
            contactGroup = "contact",
            payloadGroup = "message"
        ),
        // WhatsApp: "send <contact> on (whatsapp|wa): <message>"
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*send\\s+(?<contact>[^:\\-]+?)\\s+on\\s+(?:whatsapp|wa)[:\\-]\\s*(?<message>.+)$"),
            app = AppType.WHATSAPP,
            action = ActionType.SEND_MESSAGE,
            contactGroup = "contact",
            payloadGroup = "message"
        ),
        // WhatsApp: "whatsapp <contact> <message>" (space separated without punctuation)
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
        // SMS: "sms to <contact>: <message>"
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*sms\\s+to\\s+(?<contact>[^:\\-]+?)[:\\-]\\s*(?<message>.+)$"),
            app = AppType.SMS,
            action = ActionType.SEND_MESSAGE,
            contactGroup = "contact",
            payloadGroup = "message"
        ),
        // SMS: "sms <contact>: <message>"
        PatternRule(
            pattern = Pattern.compile("^(?i)\\s*sms\\s+(?<contact>[^:\\-]+?)[:\\-]\\s*(?<message>.+)$"),
            app = AppType.SMS,
            action = ActionType.SEND_MESSAGE,
            contactGroup = "contact",
            payloadGroup = "message"
        ),
        // SMS: "sms <contact> <message>" (single word contact)
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

                val command = Command(
                    app = rule.app,
                    action = rule.action,
                    contact = contact,
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
            suggestion = "Examples:\n  • send whatsapp to Rahul: reaching in 10 mins\n  • call Mom\n  • send sms to John: hey\n  • open YouTube\n  • help"
        )
    }
}
