package es.pile.core.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Number of digits every document PIN must have. */
const val DOCUMENT_PIN_LENGTH = 4

/**
 * A simple numeric keypad used to type the PIN that protects a document.
 *
 * @param onDigit Called with the digit that has been pressed ("0".."9").
 * @param onBackspace Called when the last digit must be removed.
 */
@Composable
fun PinPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "backspace")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        when (key) {
                            "" -> Unit

                            "backspace" -> PinPadButton(
                                onClick = onBackspace,
                                enabled = enabled,
                                content = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = null
                                    )
                                }
                            )

                            else -> PinPadButton(
                                onClick = { onDigit(key) },
                                enabled = enabled,
                                content = {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontSize = 24.sp
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinPadButton(
    onClick: () -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(64.dp),
        shape = RoundedCornerShape(24.dp),
        content = content
    )
}

/**
 * Row of dots showing how many digits of the PIN have been typed already.
 */
@Composable
fun PinDots(
    pinLength: Int,
    totalDigits: Int,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    val filledColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    val emptyColor = if (isError) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalDigits) { index ->
            val filled = index < pinLength

            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .then(
                        if (filled) {
                            Modifier.background(filledColor)
                        } else {
                            Modifier.border(1.5.dp, emptyColor, CircleShape)
                        }
                    )
            )
        }
    }
}
