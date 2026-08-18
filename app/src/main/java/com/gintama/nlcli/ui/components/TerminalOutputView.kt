package com.gintama.nlcli.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gintama.nlcli.ui.theme.TerminalBackground
import com.gintama.nlcli.ui.theme.TerminalCyan
import com.gintama.nlcli.ui.theme.TerminalGreen
import com.gintama.nlcli.ui.theme.TerminalRed
import com.gintama.nlcli.ui.theme.TerminalSurfaceVariant
import com.gintama.nlcli.ui.theme.TextBright
import com.gintama.nlcli.ui.theme.TextMuted
import com.gintama.nlcli.ui.theme.TextSubtle
import com.gintama.nlcli.ui.viewmodel.LineType
import com.gintama.nlcli.ui.viewmodel.TerminalLine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TerminalOutputView(
    lines: List<TerminalLine>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to the latest log line
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(lines, key = { it.id }) { line ->
            TerminalLineItem(line = line)
        }
    }
}

@Composable
fun TerminalLineItem(line: TerminalLine) {
    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timeString = timeFormatter.format(Date(line.timestampMs))

    when (line.type) {
        LineType.INPUT -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSubtle, fontSize = 10.sp),
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = line.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = TerminalGreen,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }

        LineType.SUCCESS -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(TerminalSurfaceVariant.copy(alpha = 0.6f))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "✔",
                        color = TerminalGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextBright,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
                if (!line.details.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = line.details,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.padding(start = 20.dp)
                    )
                }
            }
        }

        LineType.ERROR -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(TerminalRed.copy(alpha = 0.15f))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "✘",
                        color = TerminalRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TerminalRed,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
                if (!line.details.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = line.details,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.padding(start = 20.dp)
                    )
                }
            }
        }

        LineType.SYSTEM -> {
            Text(
                text = line.text,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TerminalCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            )
        }

        LineType.INFO, LineType.SUGGESTION -> {
            Text(
                text = line.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            )
        }
    }
}
