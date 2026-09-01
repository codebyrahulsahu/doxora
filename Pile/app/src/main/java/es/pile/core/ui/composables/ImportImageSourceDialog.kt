package es.pile.core.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pile.R
import androidx.compose.ui.tooling.preview.Preview
import es.pile.core.ui.theme.PileTheme

/**
 * Asks where the new images come from: the gallery (photo picker) or the device
 * folders (file browser). Both sources go through the Document Resizer prompt.
 *
 * @param onDismiss Called when the dialog is cancelled.
 * @param onGallery Called to import the images from the gallery.
 * @param onDeviceFiles Called to import the images from the device folders.
 */
@Composable
fun ImportImageSourceDialog(
    onDismiss: () -> Unit,
    onGallery: () -> Unit,
    onDeviceFiles: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_image_source)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onGallery,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Photo,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = stringResource(R.string.import_from_gallery),
                        modifier = Modifier.weight(1f)
                    )
                }

                TextButton(
                    onClick = onDeviceFiles,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = stringResource(R.string.import_from_device),
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
private fun ImportImageSourceDialogPreview() {
    PileTheme {
        ImportImageSourceDialog(onDismiss = {}, onGallery = {}, onDeviceFiles = {})
    }
}
