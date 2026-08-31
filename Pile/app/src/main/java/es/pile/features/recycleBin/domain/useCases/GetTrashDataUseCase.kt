package es.pile.features.recycleBin.domain.useCases

import es.pile.core.domain.repositories.BitmapCacheRepository
import es.pile.core.domain.repositories.DocumentModelRepository
import es.pile.core.domain.repositories.TrashRepository
import es.pile.features.recycleBin.domain.models.TrashedDocument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Combines the trashed document metadata with the trash entries so the
 * Recycle Bin screen can render documents ordered by deletion date.
 */
class GetTrashDataUseCase(
    private val documentModelRepository: DocumentModelRepository,
    private val trashRepository: TrashRepository,
    private val bitmapCacheRepository: BitmapCacheRepository
) {
    operator fun invoke(): Flow<List<TrashedDocument>> = combine(
        documentModelRepository.documentModels,
        trashRepository.trashEntries
    ) { documents, entries ->
        val documentsById = documents.associateBy { it.id }

        entries.mapNotNull { entry ->
            val document = documentsById[entry.documentId] ?: return@mapNotNull null

            TrashedDocument(
                document = document,
                trashedAt = entry.trashedAt,
                originalStatus = entry.originalStatus,
                coverImageCacheKey = bitmapCacheRepository.getCoverKey(document)
            )
        }
    }
}
