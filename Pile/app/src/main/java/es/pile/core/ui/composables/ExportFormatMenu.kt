package es.pile.core.ui.composables

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pile.R
import es.pile.core.domain.models.DocumentExportFormat

/**
 * Popup menu with the three export formats supported by the app.
 *
 * Every "Export" button in the app shows this menu when it is tapped, so the
 * user always chooses between PDF, JPG and PNG before anything is written.
 *
 * @param expanded Whether the menu is visible.
 * @param onDismissRequest Called when the menu should be closed.
 * @param onFormatSelected Called with the format picked by the user.
 */
@Composable
fun ExportFormatMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onFormatSelected: (DocumentExportFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.export_as_pdf)) },
            onClick = {
                onDismissRequest()
                onFormatSelected(DocumentExportFormat.PDF)
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.export_as_jpg)) },
            onClick = {
                onDismissRequest()
                onFormatSelected(DocumentExportFormat.JPG)
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.export_as_png)) },
            onClick = {
                onDismissRequest()
                onFormatSelected(DocumentExportFormat.PNG)
            }
        )
    }
}
