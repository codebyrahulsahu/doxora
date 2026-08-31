package es.pile.features.recycleBin.domain.useCases

import es.pile.core.domain.repositories.DocumentModelRepository
import es.pile.core.domain.repositories.TrashRepository
import es.pile.features.home.domain.schedulers.CleanupScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Moves a document back from the Recycle Bin to its previous state.
 *
 * The document keeps all of its data (images, piles, favorite, PIN and
 * recognized text), so restoring only flips the status back and removes the
 * trash entry plus its pending deletion work.
 */
class RestoreTrashedDocumentUseCase(
    private val ioDispatcher: CoroutineDispatcher,
    private val documentModelRepository: DocumentModelRepository,
    private val trashRepository: TrashRepository,
    private val cleanupScheduler: CleanupScheduler
) {
    suspend operator fun invoke(documentId: String) = withContext(ioDispatcher) {
        val entry = trashRepository.getTrashEntry(documentId) ?: return@withContext

        documentModelRepository.updateStatus(documentId, entry.originalStatus)
        trashRepository.removeFromTrash(documentId)
        cleanupScheduler.cancelDocumentTrashPurge(documentId)
    }
}
