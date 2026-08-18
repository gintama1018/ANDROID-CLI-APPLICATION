package com.gintama.nlcli.dispatcher

import android.content.Context
import com.gintama.nlcli.contacts.ContactResolver
import com.gintama.nlcli.data.AppDatabase
import com.gintama.nlcli.data.dao.CommandHistoryDao
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
import kotlinx.coroutines.withContext

class CommandDispatcher(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getInstance(context),
    private val regexParser: CommandParser = RegexParser(),
    private val llmParser: CommandParser = LlmParser(regexParser as RegexParser)
) {

    private val historyDao: CommandHistoryDao = database.commandHistoryDao()
    private val contactResolver: ContactResolver = ContactResolver(context, database.contactCacheDao())

    private val whatsAppExecutor: ICommandExecutor = WhatsAppExecutor(context, contactResolver)
    private val smsExecutor: ICommandExecutor = SmsExecutor(context, contactResolver)
    private val callExecutor: ICommandExecutor = CallExecutor(context, contactResolver)
    private val appLauncherExecutor: ICommandExecutor = AppLauncherExecutor(context)
    private val systemCommandExecutor: ICommandExecutor = SystemCommandExecutor(context, contactResolver, historyDao)

    /**
     * Executes a raw user command string through the complete pipeline:
     * 1. Parse (Regex fast-path -> LLM fallback)
     * 2. Dispatch to matching executor
     * 3. Record execution result in Room database
     */
    suspend fun dispatch(rawInput: String): ExecutionResult = withContext(Dispatchers.IO) {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            return@withContext ExecutionResult(
                success = false,
                message = "No command entered. Type 'help' for instructions."
            )
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
                return@withContext failureResult
            }
            is ParserResult.Failure -> {
                val failureResult = ExecutionResult(
                    success = false,
                    message = parseResult.reason,
                    details = parseResult.suggestion
                )
                saveHistory(trimmed, "unknown", "unknown", null, null, false, failureResult.message, "PARSER")
                return@withContext failureResult
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

        // 3. Persistence Phase (Room DB) - Mask sensitive payload in rawInput and payload columns
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

        executionResult
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
