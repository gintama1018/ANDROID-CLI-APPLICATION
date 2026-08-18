package com.gintama.nlcli.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.gintama.nlcli.data.entity.CommandHistoryEntity
import com.gintama.nlcli.ui.theme.CormorantGaramond
import com.gintama.nlcli.ui.theme.TerminalAmber
import com.gintama.nlcli.ui.theme.TerminalBackground
import com.gintama.nlcli.ui.theme.TerminalBorder
import com.gintama.nlcli.ui.theme.TerminalCyan
import com.gintama.nlcli.ui.theme.TerminalGreen
import com.gintama.nlcli.ui.theme.TerminalRed
import com.gintama.nlcli.ui.theme.TerminalSurface
import com.gintama.nlcli.ui.theme.TerminalSurfaceVariant
import com.gintama.nlcli.ui.theme.TextBright
import com.gintama.nlcli.ui.theme.TextMuted
import com.gintama.nlcli.ui.theme.TextSubtle
import com.gintama.nlcli.ui.viewmodel.HistoryFilter
import com.gintama.nlcli.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit,
    onReplayCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.historyItems.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TerminalBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Command History",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextBright
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextBright
                        )
                    }
                },
                actions = {
                    if (items.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearAllHistory() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear All History",
                                tint = TerminalRed
                            )
                        }
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
                .consumeWindowInsets(paddingValues)
                .imePadding()
                .padding(horizontal = 12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Search command history...",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSubtle, fontFamily = FontFamily.Monospace)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSubtle,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = TerminalSurfaceVariant,
                    unfocusedContainerColor = TerminalSurfaceVariant,
                    focusedBorderColor = TerminalGreen,
                    unfocusedBorderColor = TerminalBorder,
                    focusedTextColor = TextBright,
                    unfocusedTextColor = TextBright
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilter.entries.forEach { f ->
                    val isSelected = filter == f
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFilter(f) },
                        label = {
                            Text(
                                text = when (f) {
                                    HistoryFilter.ALL -> "All"
                                    HistoryFilter.SUCCESS_ONLY -> "Success"
                                    HistoryFilter.FAILED_ONLY -> "Failed"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TerminalGreen,
                            selectedLabelColor = TerminalBackground,
                            containerColor = TerminalSurfaceVariant,
                            labelColor = TextMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) TerminalGreen else TerminalBorder
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No command logs found.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSubtle,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        HistoryItemCard(
                            item = item,
                            onReplay = { onReplayCommand(item.rawInput) },
                            onDelete = { viewModel.deleteItem(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    item: CommandHistoryEntity,
    onReplay: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val formattedTime = timeFormatter.format(Date(item.timestampMs))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalSurfaceVariant)
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Success / Failure indicator badge
            Surface(
                color = if (item.success) TerminalGreen.copy(alpha = 0.15f) else TerminalRed.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = if (item.success) "SUCCESS" else "FAILED",
                    color = if (item.success) TerminalGreen else TerminalRed,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // App badge
            Surface(
                color = TerminalSurface,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = item.app.uppercase(),
                    color = TerminalCyan,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = formattedTime,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSubtle,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Raw input command
        Text(
            text = "> ${item.rawInput}",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextBright,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Result message
        Text(
            text = item.resultMessage,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Actions: Run again
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = onReplay,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(TerminalSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Run Again",
                    tint = TerminalGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
