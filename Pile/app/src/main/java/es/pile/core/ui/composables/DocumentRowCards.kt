package es.pile.core.ui.composables

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.pile.DocumentModel
import es.pile.R
import es.pile.core.domain.models.DocumentCoverItem
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Renders a vertical list of documents as rounded rows with a real thumbnail of the
 * document cover, the file name, the date it was added and its physical size.
 *
 * @param documents Documents to display, already ordered by the caller.
 * @param documentSizes Map from document ID to size in bytes.
 * @param bitmapCache Bitmaps already loaded, indexed by cover cache key.
 * @param onLoadBitmap Called when a cover is not cached yet.
 * @param lockedDocumentIds Ids of the documents protected with a PIN.
 * @param backgroundColor Background painted behind every row (used for
 * grouped lists).
 */
fun LazyListScope.itemDocumentsVerticalList(
    backgroundColor: Color = Color.Transparent,
    documents: List<DocumentCoverItem>,
    documentSizes: Map<String, Long>,
    bitmapCache: Map<String, Bitmap> = emptyMap(),
    onLoadBitmap: suspend (document: DocumentModel) -> Unit = {},
    lockedDocumentIds: Set<String> = emptySet(),
    onDocumentClick: (documentId: String) -> Unit = {},
    favoriteDocumentIds: Set<String> = emptySet(),
    onFavoriteToggle: (documentId: String) -> Unit = {}
) {
    documents.forEach { documentItem ->
        item(key = "document_${documentItem.document.id}") {
            val coverKey = documentItem.coverImageCacheKey
            val coverBitmap = bitmapCache[coverKey]

            if (coverBitmap == null) {
                LaunchedEffect(key1 = coverKey) {
                    onLoadBitmap(documentItem.document)
                }
            }

            DocumentRowCard(
                documentModel = documentItem.document,
                sizeBytes = documentSizes[documentItem.document.id],
                coverBitmap = coverBitmap,
                onClick = onDocumentClick,
                isFavorite = documentItem.document.id in favoriteDocumentIds,
                onFavoriteToggle = onFavoriteToggle,
                isLocked = documentItem.document.id in lockedDocumentIds,
                modifier = Modifier
                    .background(backgroundColor)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp)
            )
        }
    }
}

/**
 * A single vertical list entry: document thumbnail + name + added date + size.
 */
@Composable
fun DocumentRowCard(
    documentModel: DocumentModel,
    sizeBytes: Long?,
    onClick: (String) -> Unit,
    coverBitmap: Bitmap? = null,
    isFavorite: Boolean = false,
    onFavoriteToggle: (String) -> Unit = {},
    isLocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { onClick(documentModel.id) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DocumentCover(
                isPdf = documentModel.isIncomingPdf,
                coverBitmap = coverBitmap
            )

            Spacer(Modifier.width(14.dp))

            val title = if (documentModel.title.isBlank()) {
                stringResource(R.string.untitled_document)
            } else {
                documentModel.title
            }

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (isLocked) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = stringResource(R.string.document_locked),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(
                            R.string.added_date,
                            formattedDate(documentModel)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "  ·  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = sizeBytes?.let(::formatFileSize)
                            ?: stringResource(R.string.size_unknown),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = { onFavoriteToggle(documentModel.id) },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (isFavorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFavorite) {
                        stringResource(R.string.remove_from_favorites)
                    } else {
                        stringResource(R.string.add_to_favorites)
                    }
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Shows a real visual preview of the document content.
 *
 * While the thumbnail is still being decoded a soft file-type icon is used as a
 * placeholder so the row never looks empty.
 */
@Composable
fun DocumentCover(
    isPdf: Boolean,
    coverBitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isPdf) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (isPdf) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Box(
        modifier = modifier
            .size(54.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (coverBitmap != null) {
            Image(
                bitmap = coverBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                painter = painterResource(
                    if (isPdf) R.drawable.pdf_icon else R.drawable.monochrome_photos_24px
                ),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(if (isPdf) 38.dp else 26.dp)
            )
        }
    }
}

@Composable
private fun formattedDate(documentModel: DocumentModel): String {
    val locale = LocalLocale.current.platformLocale
    return documentModel.creationDateTime.toLocalDate()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
}

internal fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"

    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)

    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.getDefault(), "%.1f MB", mb)

    val gb = mb / 1024.0
    return String.format(Locale.getDefault(), "%.1f GB", gb)
}
