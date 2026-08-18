package com.gintama.nlcli.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gintama.nlcli.ui.components.PermissionBanner
import com.gintama.nlcli.ui.components.QuickActionChips
import com.gintama.nlcli.ui.components.TerminalInputBar
import com.gintama.nlcli.ui.components.TerminalOutputView
import com.gintama.nlcli.ui.theme.TerminalBackground
import com.gintama.nlcli.ui.theme.TerminalBorder
import com.gintama.nlcli.ui.theme.TerminalGreen
import com.gintama.nlcli.ui.theme.TerminalSurface
import com.gintama.nlcli.ui.theme.TextBright
import com.gintama.nlcli.ui.theme.TextMuted
import com.gintama.nlcli.ui.viewmodel.CliViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandBarScreen(
    viewModel: CliViewModel,
    onNavigateToHistory: () -> Unit,
    onRequestContactsPermission: () -> Unit,
    onRequestAudioPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TerminalBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NLCLI",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TerminalGreen,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        // Offline status badge
                        Surface(
                            color = TerminalSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(TerminalGreen)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "OFFLINE",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshPermissions() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Status",
                            tint = TextMuted
                        )
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "View History",
                            tint = TerminalGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TerminalSurface,
                    titleContentColor = TextBright
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Permission Banner (if Accessibility or Contacts permission is missing)
            PermissionBanner(
                isAccessibilityEnabled = uiState.isAccessibilityEnabled,
                hasContactsPermission = uiState.hasContactsPermission,
                onRequestContactsPermission = onRequestContactsPermission
            )

            // Main Terminal Output View (scrollable)
            TerminalOutputView(
                lines = uiState.terminalLines,
                modifier = Modifier.weight(1f)
            )

            // Quick Action Chips
            QuickActionChips(
                onActionSelected = { template ->
                    viewModel.onInputChange(template)
                }
            )

            // Terminal Input Prompt with Push-to-Talk Mic
            TerminalInputBar(
                value = uiState.inputText,
                onValueChange = { viewModel.onInputChange(it) },
                onExecute = { viewModel.executeCommand() },
                onHistoryUp = { viewModel.navigateHistoryUp() },
                onHistoryDown = { viewModel.navigateHistoryDown() },
                isExecuting = uiState.isExecuting,
                isListening = uiState.isListening,
                onVoiceClick = { viewModel.toggleVoiceInput(onRequestAudioPermission) }
            )
        }
    }
}
