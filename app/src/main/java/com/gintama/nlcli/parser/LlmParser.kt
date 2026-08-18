package com.gintama.nlcli.parser

import com.gintama.nlcli.model.ActionType
import com.gintama.nlcli.model.AppType
import com.gintama.nlcli.model.Command
import com.gintama.nlcli.model.ParseSource
import com.gintama.nlcli.model.ParserResult
import com.gintama.nlcli.util.Logger
import org.json.JSONObject

class LlmParser(
    private val regexFallbackParser: RegexParser = RegexParser()
) : CommandParser {

    override suspend fun parse(input: String): ParserResult {
        // Fast path: try regex first
        val regexResult = regexFallbackParser.parse(input)
        if (regexResult is ParserResult.Success) {
            return regexResult
        }

        // Slow path: try on-device LLM / natural intent parser
        return try {
            parseWithLlmEngine(input)
        } catch (e: Exception) {
            Logger.w("LLM parse exception, falling back to regex result", e)
            regexResult
        }
    }

    /**
     * Parses raw input into structured JSON schema:
     * { "app": "whatsapp|sms|phone|system", "action": "send_message|call|open_app", "contact": "...", "message": "..." }
     */
    private fun parseWithLlmEngine(input: String): ParserResult {
        // Natural language heuristic / token analyzer for intent parsing
        val lower = input.lowercase().trim()

        val jsonStr = when {
            (lower.contains("whatsapp") || lower.contains("wa ")) && (lower.contains("tell") || lower.contains("say") || lower.contains("msg") || lower.contains("ping")) -> {
                buildWhatsAppJson(input)
            }
            (lower.contains("sms") || lower.contains("text")) && (lower.contains("tell") || lower.contains("say") || lower.contains("msg")) -> {
                buildSmsJson(input)
            }
            lower.startsWith("ring ") || lower.startsWith("give a ring to ") || lower.contains("phone call to") -> {
                buildCallJson(input)
            }
            lower.contains("open ") || lower.contains("launch ") || lower.contains("take me to ") -> {
                buildAppJson(input)
            }
            else -> null
        }

        if (jsonStr == null) {
            return ParserResult.Failure(
                reason = "Could not parse intent from '$input'",
                suggestion = "Try phrasing like: 'send whatsapp to Rahul: reaching in 10 mins'"
            )
        }

        return validateAndBuildCommand(jsonStr, input)
    }

    private fun buildWhatsAppJson(input: String): String? {
        val regex = Regex("""(?i)(?:tell|message|msg|ping|send)\s+([a-zA-Z0-9\s]+?)\s+(?:on|via)\s+whatsapp\s+(?:that|saying)?\s*[:\-]?\s*(.+)""")
        val match = regex.find(input)
        return if (match != null) {
            val (contact, message) = match.destructured
            """{"app":"whatsapp","action":"send_message","contact":"${contact.trim()}","message":"${message.trim()}"}"""
        } else null
    }

    private fun buildSmsJson(input: String): String? {
        val regex = Regex("""(?i)(?:tell|message|msg|text|send)\s+([a-zA-Z0-9\s]+?)\s+(?:via|on)?\s*sms\s+(?:that|saying)?\s*[:\-]?\s*(.+)""")
        val match = regex.find(input)
        return if (match != null) {
            val (contact, message) = match.destructured
            """{"app":"sms","action":"send_message","contact":"${contact.trim()}","message":"${message.trim()}"}"""
        } else null
    }

    private fun buildCallJson(input: String): String? {
        val contact = input.replace(Regex("""(?i)^(?:ring|give a ring to|phone call to|make a call to)\s+"""), "").trim()
        return if (contact.isNotBlank()) {
            """{"app":"phone","action":"call","contact":"$contact"}"""
        } else null
    }

    private fun buildAppJson(input: String): String? {
        val app = input.replace(Regex("""(?i)^(?:take me to|switch to|open up|launch up)\s+"""), "").trim()
        return if (app.isNotBlank()) {
            """{"app":"system","action":"open_app","appname":"$app"}"""
        } else null
    }

    fun validateAndBuildCommand(jsonString: String, rawInput: String): ParserResult {
        return try {
            val json = JSONObject(jsonString)
            val appStr = json.optString("app", "utility")
            val actionStr = json.optString("action", "help")
            val contact = json.optString("contact").takeIf { it.isNotBlank() }
            val message = json.optString("message").takeIf { it.isNotBlank() }
                ?: json.optString("payload").takeIf { it.isNotBlank() }
                ?: json.optString("appname").takeIf { it.isNotBlank() }

            val appType = AppType.fromString(appStr)
            val actionType = ActionType.fromString(actionStr)

            // Strict schema validation: message sending requires contact and payload
            if (actionType == ActionType.SEND_MESSAGE && (contact.isNullOrBlank() || message.isNullOrBlank())) {
                return ParserResult.Failure(
                    reason = "Incomplete message details in LLM parse output",
                    suggestion = "Both contact name and message body are required"
                )
            }

            if (actionType == ActionType.CALL && contact.isNullOrBlank()) {
                return ParserResult.Failure(
                    reason = "Missing contact name for phone call",
                    suggestion = "Specify contact name, e.g., 'call Alex'"
                )
            }

            val command = Command(
                app = appType,
                action = actionType,
                contact = contact,
                payload = message,
                rawInput = rawInput,
                source = ParseSource.LLM,
                confidence = 0.90f
            )

            ParserResult.Success(command)
        } catch (e: Exception) {
            Logger.e("JSON schema validation failed for: $jsonString", e)
            ParserResult.Failure("Failed to validate parsed command structure")
        }
    }
}
