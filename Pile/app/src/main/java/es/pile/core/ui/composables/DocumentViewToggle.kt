package es.pile.core.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pile.R
import es.pile.core.domain.models.DocumentViewMode

/**
 * A round icon-only chip that toggles the document layout between the vertical
 * "List View" and the grid "Icon View".
 *
 * The label is intentionally hidden: only the icon is displayed (the text is
 * still exposed to accessibility services through the content description).
 */
@Composable
fun DocumentViewToggle(
    viewMode: DocumentViewMode,
    onViewModeChange: (DocumentViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val isListView = viewMode == DocumentViewMode.LIST

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable {
                onViewModeChange(
                    if (isListView) DocumentViewMode.GRID else DocumentViewMode.LIST
                )
            }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = if (isListView) {
                Icons.AutoMirrored.Filled.ViewList
            } else {
                Icons.Filled.GridView
            },
            contentDescription = stringResource(
                if (isListView) R.string.list_view else R.string.icon_view
            ),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}
