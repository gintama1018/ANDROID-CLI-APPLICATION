package com.gintama.nlcli.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gintama.nlcli.accessibility.NLCliAccessibilityService
import com.gintama.nlcli.dispatcher.CommandDispatcher
import com.gintama.nlcli.model.ActionType
import com.gintama.nlcli.model.ExecutionResult
import com.gintama.nlcli.util.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LineType {
    INPUT,
    SUCCESS,
    ERROR,
    INFO,
    SYSTEM,
    SUGGESTION
}

data class TerminalLine(
    val id: Long = System.nanoTime(),
    val type: LineType,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val details: String? = null
)

data class CliUiState(
    val terminalLines: List<TerminalLine> = emptyList(),
    val inputText: String = "",
    val isExecuting: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val hasContactsPermission: Boolean = false,
    val hasSmsPermission: Boolean = false,
    val hasCallPermission: Boolean = false,
    val commandHistory: List<String> = emptyList(),
    val historyPointer: Int = -1
)

class CliViewModel(application: Application) : AndroidViewModel(application) {

    private val dispatcher = CommandDispatcher(application)
    private val _uiState = MutableStateFlow(CliUiState())
    val uiState: StateFlow<CliUiState> = _uiState.asStateFlow()

    init {
        // Add initial terminal welcome banner
        addWelcomeBanner()
        refreshPermissions()

        // Observe background automation results from Accessibility Service
        viewModelScope.launch {
            NLCliAccessibilityService.automationResults.collect { result ->
                handleAutomationResult(result)
            }
        }
    }

    private fun addWelcomeBanner() {
        val welcomeLines = listOf(
            TerminalLine(
                type = LineType.SYSTEM,
                text = "═══════════════════════════════════════════════════\n" +
                       " NLCLI v1.0.0 — Offline Natural Language CLI\n" +
                       " 100% On-Device • Zero Internet Dependency\n" +
                       " Prepared for Sonu (GINTAMA)\n" +
                       "═══════════════════════════════════════════════════"
            ),
            TerminalLine(
                type = LineType.INFO,
                text = "Type 'help' for command syntax or tap suggestions below."
            )
        )
        _uiState.update { it.copy(terminalLines = welcomeLines) }
    }

    fun onInputChange(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun refreshPermissions() {
        val context = getApplication<Application>()
        _uiState.update {
            it.copy(
                isAccessibilityEnabled = PermissionHelper.isAccessibilityServiceEnabled(context),
                hasContactsPermission = PermissionHelper.hasContactsPermission(context),
                hasSmsPermission = PermissionHelper.hasSmsPermission(context),
                hasCallPermission = PermissionHelper.hasCallPermission(context)
            )
        }
    }

    fun executeCommand(rawCommand: String? = null) {
        val commandToRun = (rawCommand ?: _uiState.value.inputText).trim()
        if (commandToRun.isBlank()) return

        // Clear input box
        _uiState.update {
            val updatedHistory = listOf(commandToRun) + it.commandHistory.filter { c -> c != commandToRun }
            it.copy(
                inputText = "",
                isExecuting = true,
                commandHistory = updatedHistory.take(50),
                historyPointer = -1,
                terminalLines = it.terminalLines + TerminalLine(
                    type = LineType.INPUT,
                    text = "> $commandToRun"
                )
            )
        }

        viewModelScope.launch {
            val result = dispatcher.dispatch(commandToRun)

            if (result.details == "CLEAR_SCREEN_ACTION") {
                clearTerminal()
                _uiState.update { it.copy(isExecuting = false) }
                return@launch
            }

            val newLine = TerminalLine(
                type = if (result.success) LineType.SUCCESS else LineType.ERROR,
                text = result.message,
                details = result.details
            )

            _uiState.update {
                it.copy(
                    isExecuting = false,
                    terminalLines = it.terminalLines + newLine
                )
            }
        }
    }

    private fun handleAutomationResult(result: ExecutionResult) {
        val newLine = TerminalLine(
            type = if (result.success) LineType.SUCCESS else LineType.ERROR,
            text = "[ACCESSIBILITY] ${result.message}",
            details = result.details
        )
        _uiState.update {
            it.copy(terminalLines = it.terminalLines + newLine)
        }
    }

    fun navigateHistoryUp() {
        val history = _uiState.value.commandHistory
        if (history.isEmpty()) return

        val newPointer = (_uiState.value.historyPointer + 1).coerceAtMost(history.size - 1)
        _uiState.update {
            it.copy(
                historyPointer = newPointer,
                inputText = history[newPointer]
            )
        }
    }

    fun navigateHistoryDown() {
        val history = _uiState.value.commandHistory
        if (history.isEmpty()) return

        val newPointer = _uiState.value.historyPointer - 1
        if (newPointer < 0) {
            _uiState.update { it.copy(historyPointer = -1, inputText = "") }
        } else {
            _uiState.update {
                it.copy(
                    historyPointer = newPointer,
                    inputText = history[newPointer]
                )
            }
        }
    }

    fun clearTerminal() {
        _uiState.update {
            it.copy(
                terminalLines = listOf(
                    TerminalLine(
                        type = LineType.INFO,
                        text = "Terminal screen cleared. Ready for input."
                    )
                )
            )
        }
    }
}
