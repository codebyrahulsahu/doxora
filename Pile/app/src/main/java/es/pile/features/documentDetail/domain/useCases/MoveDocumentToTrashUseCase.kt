package es.pile.features.documentDetail.domain.useCases

import es.pile.DocumentModel
import es.pile.TrashEntry
import es.pile.core.domain.models.DocumentStatusConstants
import es.pile.core.domain.repositories.DocumentModelRepository
import es.pile.core.domain.repositories.TrashRepository
import es.pile.features.home.domain.schedulers.CleanupScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Moves a document into the Recycle Bin.
 *
 * Nothing is deleted yet: the document keeps its images, piles, favorite,
 * PIN and recognized text, and its status switches to
 * [DocumentStatusConstants.TRASHED] so it disappears from every list. A
 * delayed WorkManager job is scheduled to permanently delete it after
 * [es.pile.core.domain.models.TRASH_RETENTION_DAYS] days.
 */
class MoveDocumentToTrashUseCase(
    private val ioDispatcher: CoroutineDispatcher,
    private val documentModelRepository: DocumentModelRepository,
    private val trashRepository: TrashRepository,
    private val cleanupScheduler: CleanupScheduler
) {
    suspend operator fun invoke(document: DocumentModel) = withContext(ioDispatcher) {
        val existingEntry = trashRepository.getTrashEntry(document.id)

        trashRepository.addToTrash(
            TrashEntry(
                documentId = document.id,
                trashedAt = LocalDateTime.now(),
                originalStatus = existingEntry?.originalStatus ?: document.documentStatus
            )
        )

        if (document.documentStatus != DocumentStatusConstants.TRASHED) {
            documentModelRepository.updateStatus(document.id, DocumentStatusConstants.TRASHED)
        }

        cleanupScheduler.scheduleDocumentTrashPurge(document.id)
    }
}
