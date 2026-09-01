package es.pile.core.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.pile.R
import es.pile.core.domain.models.DocumentResizeMode
import es.pile.core.domain.models.DocumentResizeTargetSize
import es.pile.core.ui.theme.PileTheme

/**
 * Prompt shown when the Document Resizer is tapped in the selection top bar.
 *
 * It lets the user set a custom target file size in KB or MB, then offers
 * exactly two options for the resized document:
 *  1. Save as original file in app (the resized pages replace the original files).
 *  2. Save as duplicate file (a new resized copy is created in the app).
 *
 * Pages are compressed with zero quality loss: JPEG quality is never reduced.
 *
 * @param initialTargetSizeKb Last target size used by the resizer, used to
 * pre-fill the input field.
 * @param onDismiss Called when the prompt is cancelled: nothing is resized.
 * @param onConfirm Called with the chosen [DocumentResizeMode] and the
 * custom target size in kilobytes.
 */
@Composable
fun DocumentResizeOptionsDialog(
    initialTargetSizeKb: Int,
    onDismiss: () -> Unit,
    onConfirm: (mode: DocumentResizeMode, targetSizeKb: Int) -> Unit
) {
    val sanitizedInitial = initialTargetSizeKb.coerceAtLeast(DocumentResizeTargetSize.MIN_KB)
    val initialUnit = DocumentResizeTargetSize.preferredUnit(sanitizedInitial)

    var sizeText by rememberSaveable {
        mutableStateOf(DocumentResizeTargetSize.displayValue(sanitizedInitial, initialUnit))
    }
    var unit by rememberSaveable { mutableStateOf(initialUnit) }

    val parsedSizeKb = DocumentResizeTargetSize.parse(sizeText, unit)
    val isValidSize = parsedSizeKb != null && DocumentResizeTargetSize.isValid(parsedSizeKb)
    val showError = sizeText.isNotBlank() && !isValidSize

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.document_resizer)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.document_resizer_options_body),
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = sizeText,
                    onValueChange = { sizeText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.document_resizer_target_size)) },
                    placeholder = { Text(stringResource(R.string.document_resizer_target_size_hint)) },
                    supportingText = {
                        Text(
                            text = stringResource(
                                if (showError) {
                                    R.string.document_resizer_invalid_size
                                } else {
                                    R.string.document_resizer_lossless_hint
                                }
                            )
                        )
                    },
                    isError = showError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.document_resizer_unit_label),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = unit == DocumentResizeTargetSize.Unit.KB,
                        onClick = {
                            sizeText = DocumentResizeTargetSize.convertDisplay(
                                sizeText,
                                unit,
                                DocumentResizeTargetSize.Unit.KB
                            )
                            unit = DocumentResizeTargetSize.Unit.KB
                        },
                        label = { Text(stringResource(R.string.document_resizer_unit_kb)) }
                    )
                    FilterChip(
                        selected = unit == DocumentResizeTargetSize.Unit.MB,
                        onClick = {
                            sizeText = DocumentResizeTargetSize.convertDisplay(
                                sizeText,
                                unit,
                                DocumentResizeTargetSize.Unit.MB
                            )
                            unit = DocumentResizeTargetSize.Unit.MB
                        },
                        label = { Text(stringResource(R.string.document_resizer_unit_mb)) }
                    )
                }

                TextButton(
                    onClick = {
                        parsedSizeKb?.let { onConfirm(DocumentResizeMode.SAVE_AS_ORIGINAL, it) }
                    },
                    enabled = isValidSize,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.SaveAs,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = stringResource(R.string.resize_save_as_original),
                        modifier = Modifier.weight(1f)
                    )
                }

                TextButton(
                    onClick = {
                        parsedSizeKb?.let { onConfirm(DocumentResizeMode.SAVE_AS_DUPLICATE, it) }
                    },
                    enabled = isValidSize,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.FileCopy,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = stringResource(R.string.resize_save_as_duplicate),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Preview
@Composable
private fun DocumentResizeOptionsDialogPreview() {
    PileTheme {
        DocumentResizeOptionsDialog(
            initialTargetSizeKb = 512,
            onDismiss = {},
            onConfirm = { _, _ -> }
        )
    }
}
