package es.pile.core.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.pile.R
import es.pile.core.domain.models.DocumentResizeMode
import es.pile.core.ui.theme.PileTheme

/**
 * Prompt shown when the Document Resizer is tapped in the selection top bar.
 *
 * It offers exactly two options for the resized document:
 *  1. Save as original file in app (the resized pages replace the original files).
 *  2. Save as duplicate file (a new resized copy is created in the app).
 *
 * @param onDismiss Called when the prompt is cancelled: nothing is resized.
 * @param onModeSelected Called with the chosen [DocumentResizeMode].
 */
@Composable
fun DocumentResizeOptionsDialog(
    onDismiss: () -> Unit,
    onModeSelected: (DocumentResizeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.document_resizer)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.document_resizer_options_body),
                    style = MaterialTheme.typography.bodyMedium
                )

                TextButton(
                    onClick = { onModeSelected(DocumentResizeMode.SAVE_AS_ORIGINAL) },
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
                    onClick = { onModeSelected(DocumentResizeMode.SAVE_AS_DUPLICATE) },
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
        DocumentResizeOptionsDialog(onDismiss = {}, onModeSelected = {})
    }
}
