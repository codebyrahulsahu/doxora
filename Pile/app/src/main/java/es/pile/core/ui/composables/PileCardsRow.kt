package es.pile.core.ui.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.pile.PileModel
import es.pile.R
import es.pile.core.ui.theme.AppIcons
import es.pile.core.ui.theme.ExtendedTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Horizontally scrollable row of color-coded hub cards.
 *
 * Cards can be rearranged by long pressing one of them and dragging it to a
 * new position; the new order is persisted through [onPilesReordered].
 *
 * @param piles The list of hubs to display.
 * @param pileDocumentCounts Map from hub ID to the number of documents inside it.
 * @param onPileClick Invoked with the id of the clicked hub.
 * @param onNewPileClick Invoked when the "New Hub" card is tapped.
 * @param onPilesReordered Invoked with the ids of the hubs ordered by their new position.
 */
@Composable
fun PileCardsRow(
    modifier: Modifier = Modifier,
    piles: List<PileModel>,
    pileDocumentCounts: Map<String, Int>,
    onPileClick: (String) -> Unit,
    onNewPileClick: () -> Unit,
    onPilesReordered: (List<String>) -> Unit = {}
) {
    val lazyListState = rememberLazyListState()
    val hapticFeedback = LocalHapticFeedback.current

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (from.index !in piles.indices) return@rememberReorderableLazyListState

        val reorderedPiles = piles.toMutableList()
        val movedPile = reorderedPiles.removeAt(from.index)
        val targetIndex = to.index.coerceIn(0, reorderedPiles.size)
        reorderedPiles.add(targetIndex, movedPile)

        onPilesReordered(reorderedPiles.map { it.id })
    }

    LazyRow(
        modifier = modifier,
        state = lazyListState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(piles.size, key = { "pile_${piles[it].id}" }) { index ->
            val pile = piles[index]

            ReorderableItem(reorderableState, key = "pile_${pile.id}") { isDragging ->
                PileCard(
                    pileModel = pile,
                    documentCount = pileDocumentCounts[pile.id] ?: 0,
                    onClick = { onPileClick(pile.id) },
                    isDragging = isDragging,
                    modifier = Modifier
                        .longPressDraggableHandle(
                            onDragStarted = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                )
            }
        }

        item(key = "pile_new") {
            NewPileCard(onClick = onNewPileClick)
        }
    }
}

/**
 * A color-coded rectangular folder card representing a single hub with its
 * name and the number of documents inside it.
 */
@Composable
fun PileCard(
    pileModel: PileModel,
    documentCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false
) {
    val colorIndex = pileModel.colorNumber?.toInt()
    val colorFamily = ExtendedTheme.colors.customColorList.getOrNull(colorIndex ?: -1)

    val containerColor = colorFamily?.colorContainer
        ?: MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = colorFamily?.onColorContainer
        ?: MaterialTheme.colorScheme.onSurface

    Card(
        onClick = onClick,
        modifier = modifier
            .width(160.dp)
            .graphicsLayer {
                scaleX = if (isDragging) 1.06f else 1f
                scaleY = if (isDragging) 1.06f else 1f
                shadowElevation = if (isDragging) 12.dp.toPx() else 0f
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Row(
            modifier = Modifier
                .height(88.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(contentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(AppIcons.getById(pileModel.iconId)),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(21.dp)
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = pileModel.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        tint = contentColor.copy(alpha = 0.72f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.pile_item_count, documentCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.72f)
                    )
                }
            }
        }
    }
}

/**
 * Dashed-outline style card used to create a new hub. Always rendered as the
 * last card in [PileCardsRow].
 */
@Composable
fun NewPileCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.width(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .height(88.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = stringResource(R.string.new_pile),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
