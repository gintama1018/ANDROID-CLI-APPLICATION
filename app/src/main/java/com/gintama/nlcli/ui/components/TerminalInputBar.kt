package com.gintama.nlcli.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gintama.nlcli.ui.theme.PromptColor
import com.gintama.nlcli.ui.theme.TerminalBackground
import com.gintama.nlcli.ui.theme.TerminalBorder
import com.gintama.nlcli.ui.theme.TerminalGreen
import com.gintama.nlcli.ui.theme.TerminalRed
import com.gintama.nlcli.ui.theme.TerminalSurfaceVariant
import com.gintama.nlcli.ui.theme.TextBright
import com.gintama.nlcli.ui.theme.TextSubtle

@Composable
fun TerminalInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onExecute: () -> Unit,
    onHistoryUp: () -> Unit,
    onHistoryDown: () -> Unit,
    isExecuting: Boolean,
    isListening: Boolean = false,
    onVoiceClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TerminalSurfaceVariant)
            .border(
                1.dp,
                if (isListening) TerminalRed else TerminalBorder,
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Prompt indicator `>`
        Text(
            text = ">",
            style = MaterialTheme.typography.titleMedium.copy(
                color = if (isListening) TerminalRed else PromptColor,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp
            ),
            modifier = Modifier.padding(start = 4.dp, end = 6.dp)
        )

        // Text input field
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = TextBright,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            ),
            cursorBrush = SolidColor(TerminalGreen),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onExecute() }),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = if (isListening) "Listening... speak now" else "type or tap mic...",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = if (isListening) TerminalRed else TextSubtle,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )

        // Voice Input (Push-To-Talk) Button
        IconButton(
            onClick = onVoiceClick,
            modifier = Modifier
                .size(32.dp)
                .scale(if (isListening) pulseScale else 1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isListening) TerminalRed.copy(alpha = 0.2f) else TerminalSurfaceVariant)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = if (isListening) "Stop Listening" else "Push to Talk",
                tint = if (isListening) TerminalRed else TerminalGreen,
                modifier = Modifier.size(18.dp)
            )
        }

        // History Navigation Buttons
        IconButton(
            onClick = onHistoryUp,
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = "History Previous",
                tint = TextSubtle,
                modifier = Modifier.size(15.dp)
            )
        }

        IconButton(
            onClick = onHistoryDown,
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = "History Next",
                tint = TextSubtle,
                modifier = Modifier.size(15.dp)
            )
        }

        if (value.isNotEmpty()) {
            IconButton(
                onClick = { onValueChange("") },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear Input",
                    tint = TextSubtle,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Execute Button / Spinner
        if (isExecuting) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .padding(4.dp),
                strokeWidth = 2.dp,
                color = TerminalGreen
            )
        } else {
            IconButton(
                onClick = onExecute,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (value.isNotBlank()) TerminalGreen else TerminalBorder)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Execute Command",
                    tint = if (value.isNotBlank()) TerminalBackground else TextSubtle,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
