package es.pile.features.recycleBin.domain.models

import es.pile.DocumentModel
import java.time.LocalDateTime

/**
 * A document currently sitting in the Recycle Bin together with the metadata
 * needed to show it and (later) restore it.
 *
 * @property originalStatus The document status before it was deleted, so it can
 * be restored exactly as it was.
 */
data class TrashedDocument(
    val document: DocumentModel,
    val trashedAt: LocalDateTime,
    val originalStatus: Int,
    val coverImageCacheKey: String
)
