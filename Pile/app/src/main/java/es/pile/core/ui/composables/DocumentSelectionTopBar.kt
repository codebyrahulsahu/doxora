package es.pile.core.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pile.R
import es.pile.core.domain.models.DocumentExportFormat
import es.pile.core.domain.models.DocumentResizeMode

/**
 * Top bar shown while the multi selection mode is active (both in the home
 * screen and inside a hub): it replaces the search bar and reveals the actions
 * available for every selected document.
 *
 * The Export action always opens the [ExportFormatMenu] with the three
 * supported formats before anything is exported.
 *
 * The Document Resizer lives here (and only here): it is offered exclusively
 * while a document is actively selected. Tapping it opens the
 * [DocumentResizeOptionsDialog] with a custom target size (KB or MB) and
 * exactly two save options: save the resized pages as the original file in
 * the app, or save them as a duplicate file.
 */
@Composable
fun DocumentSelectionTopBar(
    selectedCount: Int,
    enabled: Boolean,
    onClose: () -> Unit,
    onExportFormatSelected: (DocumentExportFormat) -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onResizeConfirmed: (DocumentResizeMode, targetSizeKb: Int) -> Unit,
    initialTargetSizeKb: Int = 512,
    modifier: Modifier = Modifier
) {
    var exportMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var resizeOptionsExpanded by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 8.dp)
                .padding(top = 4.dp, bottom = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close_menu)
                    )
                }

                Text(
                    text = stringResource(R.string.documents_selected, selectedCount),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SelectionActionButton(
                        icon = Icons.Filled.FileDownload,
                        label = stringResource(R.string.export),
                        onClick = { exportMenuExpanded = true },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExportFormatMenu(
                        expanded = exportMenuExpanded,
                        onDismissRequest = { exportMenuExpanded = false },
                        onFormatSelected = onExportFormatSelected
                    )
                }

                SelectionActionButton(
                    icon = Icons.Filled.Share,
                    label = stringResource(R.string.share),
                    onClick = onShare,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                )

                SelectionActionButton(
                    icon = Icons.Filled.PhotoSizeSelectLarge,
                    label = stringResource(R.string.resize),
                    onClick = { resizeOptionsExpanded = true },
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                )

                SelectionActionButton(
                    icon = Icons.Filled.Delete,
                    label = stringResource(R.string.delete),
                    onClick = onDelete,
                    enabled = enabled,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Document Resizer prompt: custom target size plus two save options.
    if (resizeOptionsExpanded) {
        DocumentResizeOptionsDialog(
            initialTargetSizeKb = initialTargetSizeKb,
            onDismiss = { resizeOptionsExpanded = false },
            onConfirm = { mode, targetSizeKb ->
                resizeOptionsExpanded = false
                onResizeConfirmed(mode, targetSizeKb)
            }
        )
    }
}

@Composable
private fun SelectionActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val contentAlpha = if (enabled) 1f else 0.38f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint.copy(alpha = contentAlpha),
                modifier = Modifier.size(18.dp)
            )

            Spacer(Modifier.width(6.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )
        }
    }
}
