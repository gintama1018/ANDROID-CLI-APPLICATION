package com.gintama.nlcli.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gintama.nlcli.ui.theme.TerminalBorder
import com.gintama.nlcli.ui.theme.TerminalCyan
import com.gintama.nlcli.ui.theme.TerminalGreen
import com.gintama.nlcli.ui.theme.TerminalSurface
import com.gintama.nlcli.ui.theme.TextBright

data class QuickAction(val label: String, val template: String)

val DefaultQuickActions = listOf(
    QuickAction("💬 WhatsApp", "send whatsapp to Rahul: reaching in 10 mins"),
    QuickAction("📞 Call", "call Mom"),
    QuickAction("✉️ SMS", "send sms to Alex: hey"),
    QuickAction("🚀 Open App", "open YouTube"),
    QuickAction("🧪 Dry Run", "dryrun send whatsapp to Boss: here is the file"),
    QuickAction("⚡ Status", "status"),
    QuickAction("❓ Help", "help"),
    QuickAction("🧹 Clear", "clear")
)

@Composable
fun QuickActionChips(
    onActionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    actions: List<QuickAction> = DefaultQuickActions
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { action ->
            FilterChip(
                selected = false,
                onClick = { onActionSelected(action.template) },
                label = {
                    Text(
                        text = action.label,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = TerminalSurface,
                    labelColor = TextBright
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = false,
                    borderColor = TerminalBorder,
                    borderWidth = 1.dp
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}
