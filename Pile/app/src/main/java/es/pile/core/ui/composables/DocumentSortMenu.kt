package es.pile.core.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pile.R
import es.pile.core.domain.models.DocumentSortOrder

/**
 * A round icon-only sort chip with a dropdown to choose between the supported
 * [DocumentSortOrder] options.
 *
 * The "Sort" label is hidden: only the icon is shown (the text is still
 * available to accessibility services as the content description).
 */
@Composable
fun DocumentSortMenu(
    sortOrder: DocumentSortOrder,
    onSortOrderChange: (DocumentSortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable { expanded = true }
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Sort,
                contentDescription = stringResource(R.string.sort_documents),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }


        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SortMenuItem(
                label = stringResource(R.string.sort_newest),
                selected = sortOrder == DocumentSortOrder.NEWEST,
                onClick = {
                    onSortOrderChange(DocumentSortOrder.NEWEST)
                    expanded = false
                }
            )

            SortMenuItem(
                label = stringResource(R.string.sort_oldest),
                selected = sortOrder == DocumentSortOrder.OLDEST,
                onClick = {
                    onSortOrderChange(DocumentSortOrder.OLDEST)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun SortMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = {
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.check_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        onClick = onClick
    )
}
