package com.gintama.nlcli.dispatcher

import android.content.Context
import com.gintama.nlcli.accessibility.NLCliAccessibilityService
import com.gintama.nlcli.contacts.ContactResolver
import com.gintama.nlcli.data.AppDatabase
import com.gintama.nlcli.data.dao.CommandHistoryDao
import com.gintama.nlcli.data.dao.MacroDao
import com.gintama.nlcli.data.dao.SnippetDao
import com.gintama.nlcli.data.entity.CommandHistoryEntity
import com.gintama.nlcli.executor.AppLauncherExecutor
import com.gintama.nlcli.executor.CallExecutor
import com.gintama.nlcli.executor.ICommandExecutor
import com.gintama.nlcli.executor.SmsExecutor
import com.gintama.nlcli.executor.SystemCommandExecutor
import com.gintama.nlcli.executor.WhatsAppExecutor
import com.gintama.nlcli.model.ActionType
import com.gintama.nlcli.model.AppType
import com.gintama.nlcli.model.Command
import com.gintama.nlcli.model.ExecutionResult
import com.gintama.nlcli.model.ParserResult
import com.gintama.nlcli.parser.CommandParser
import com.gintama.nlcli.parser.LlmParser
import com.gintama.nlcli.parser.RegexParser
import com.gintama.nlcli.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class CommandDispatcher(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getInstance(context),
    private val regexParser: CommandParser = RegexParser(),
    private val llmParser: CommandParser = LlmParser(regexParser as RegexParser)
) {

    private val historyDao: CommandHistoryDao = database.commandHistoryDao()
    private val snippetDao: SnippetDao = database.snippetDao()
    private val macroDao: MacroDao = database.macroDao()
    private val contactResolver: ContactResolver = ContactResolver(context, database.contactCacheDao())

    private val whatsAppExecutor: ICommandExecutor = WhatsAppExecutor(context, contactResolver)
    private val smsExecutor: ICommandExecutor = SmsExecutor(context, contactResolver)
    private val callExecutor: ICommandExecutor = CallExecutor(context, contactResolver)
    private val appLauncherExecutor: ICommandExecutor = AppLauncherExecutor(context)
    private val systemCommandExecutor: ICommandExecutor = SystemCommandExecutor(context, contactResolver, historyDao)

    /**
     * Executes a raw user command string through the complete pipeline:
     * 1. Macro & Snippet pre-processing
     * 2. Inline chaining split (`;`)
     * 3. Parse (Regex fast-path -> LLM fallback)
     * 4. Dispatch to matching executor
     * 5. Record execution result in Room database
     */
    suspend fun dispatch(rawInput: String): ExecutionResult = withContext(Dispatchers.IO) {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            return@withContext ExecutionResult(
                success = false,
                message = "No command entered. Type 'help' for instructions."
            )
        }

        // 1. Macro Expansion (e.g. `gm` -> `torch off; volume 100; open spotify`)
        val macroExpanded = expandMacro(trimmed)

        // 2. Snippet Substitution (e.g. `wa Rahul: pay {upi}` -> `wa Rahul: pay user@bank`)
        val snippetExpanded = substituteSnippets(macroExpanded)

        // 3. Inline Chaining (split on unescaped `;`)
        val segments = splitChainedCommands(snippetExpanded)
        if (segments.size > 1) {
            return@withContext executeChainedPipeline(segments)
        }

        // Single command dispatch
        executeSingleCommand(segments.firstOrNull() ?: trimmed)
    }

    private suspend fun executeChainedPipeline(commands: List<String>): ExecutionResult {
        val results = mutableListOf<ExecutionResult>()
        var overallSuccess = true

        for ((index, cmdText) in commands.withIndex()) {
            // If previous command triggered a WhatsApp automation send, wait briefly for UI transition
            if (NLCliAccessibilityService.isAutomationBusy) {
                var waitCount = 0
                while (NLCliAccessibilityService.isAutomationBusy && waitCount < 30) {
                    delay(200L)
                    waitCount++
                }
            }

            val result = executeSingleCommand(cmdText)
            results.add(result)
            if (!result.success) {
                overallSuccess = false
            }

            // Small pace delay between chained actions
            if (index < commands.size - 1) {
                delay(300L)
            }
        }

        val aggregatedMessage = results.mapIndexed { idx, res ->
            val icon = if (res.success) "✔" else "✘"
            "$icon [${idx + 1}/${results.size}] ${res.message}"
        }.joinToString("\n")

        return ExecutionResult(
            success = overallSuccess,
            message = aggregatedMessage,
            details = "Executed ${commands.size} chained commands sequentially"
        )
    }

    private suspend fun executeSingleCommand(input: String): ExecutionResult {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return ExecutionResult(false, "Empty command segment")
        }

        // 1. Parsing Phase
        val parseResult = when (val res = regexParser.parse(trimmed)) {
            is ParserResult.Success -> res
            else -> llmParser.parse(trimmed)
        }

        val command = when (parseResult) {
            is ParserResult.Success -> parseResult.command
            is ParserResult.Ambiguous -> {
                val failureResult = ExecutionResult(
                    success = false,
                    message = "Ambiguous command: ${parseResult.reason}",
                    details = parseResult.candidates.joinToString("\n") { "• ${it.rawInput}" }
                )
                saveHistory(trimmed, "unknown", "unknown", null, null, false, failureResult.message, "PARSER")
                return failureResult
            }
            is ParserResult.Failure -> {
                val failureResult = ExecutionResult(
                    success = false,
                    message = parseResult.reason,
                    details = parseResult.suggestion
                )
                saveHistory(trimmed, "unknown", "unknown", null, null, false, failureResult.message, "PARSER")
                return failureResult
            }
        }

        // 2. Execution Phase
        val executionResult = try {
            when {
                command.action == ActionType.DRY_RUN -> {
                    systemCommandExecutor.execute(command)
                }
                command.app == AppType.WHATSAPP -> {
                    whatsAppExecutor.execute(command)
                }
                command.app == AppType.SMS -> {
                    smsExecutor.execute(command)
                }
                command.app == AppType.PHONE -> {
                    callExecutor.execute(command)
                }
                command.app == AppType.SYSTEM && command.action == ActionType.OPEN_APP -> {
                    appLauncherExecutor.execute(command)
                }
                else -> {
                    systemCommandExecutor.execute(command)
                }
            }.copy(command = command)
        } catch (e: Exception) {
            Logger.e("Exception during command dispatch", e)
            ExecutionResult(
                success = false,
                message = "Execution failed: ${e.localizedMessage ?: "Unknown error"}",
                details = e.stackTraceToString(),
                command = command
            )
        }

        // 3. Persistence Phase (Room DB) - Mask sensitive payload
        val safeRawInput = Logger.maskCommandInput(command.rawInput, command.payload)
        saveHistory(
            rawInput = safeRawInput,
            app = command.app.rawValue,
            action = command.action.rawValue,
            contact = command.contact,
            payload = Logger.sanitizeForLog(command.payload),
            success = executionResult.success,
            resultMessage = executionResult.message,
            source = command.source.name
        )

        return executionResult
    }

    private suspend fun expandMacro(input: String): String {
        val trimmed = input.trim()
        if (trimmed.contains("=") || trimmed.startsWith("alias", ignoreCase = true)) {
            return input
        }
        val macro = macroDao.getMacro(trimmed)
        if (macro != null && macro.commandSequence.isNotBlank()) {
            Logger.d("Expanded alias '$trimmed' ➔ '${macro.commandSequence}'")
            return macro.commandSequence
        }
        return input
    }

    private suspend fun substituteSnippets(input: String): String {
        if (!input.contains("{") || !input.contains("}")) {
            return input
        }
        var result = input
        val snippets = snippetDao.getAllSnippets()
        for (snippet in snippets) {
            val token = "{${snippet.name}}"
            if (result.contains(token, ignoreCase = true)) {
                result = result.replace(token, snippet.value, ignoreCase = true)
            }
        }
        return result
    }

    private fun splitChainedCommands(input: String): List<String> {
        // Do not split if this is a macro or snippet definition assignment
        if (input.startsWith("alias", ignoreCase = true) && input.contains("=")) {
            return listOf(input)
        }
        return input.split(";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private suspend fun saveHistory(
        rawInput: String,
        app: String,
        action: String,
        contact: String?,
        payload: String?,
        success: Boolean,
        resultMessage: String,
        source: String
    ) {
        try {
            historyDao.insert(
                CommandHistoryEntity(
                    rawInput = rawInput,
                    app = app,
                    action = action,
                    contact = contact,
                    sanitizedPayload = payload,
                    success = success,
                    resultMessage = resultMessage,
                    source = source,
                    timestampMs = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Logger.w("Failed to save command to history", e)
        }
    }
}
