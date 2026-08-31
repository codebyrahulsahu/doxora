package es.pile.features.recycleBin.domain.useCases

import es.pile.core.domain.repositories.DocumentImageRepository
import es.pile.core.domain.repositories.DocumentLockRepository
import es.pile.core.domain.repositories.DocumentModelRepository
import es.pile.core.domain.repositories.DocumentTextRepository
import es.pile.core.domain.repositories.FavoritesRepository
import es.pile.core.domain.repositories.FileRepository
import es.pile.core.domain.repositories.TrashRepository
import es.pile.features.home.domain.schedulers.CleanupScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Permanently deletes a document (and everything attached to it) from the
 * device: database rows, PIN lock, favorites, recognized text and files.
 *
 * This is the only deletion path that really removes the document; it is used
 * when the user empties an entry from the Recycle Bin and when the 30 days
 * retention period expires.
 */
class DeleteTrashedDocumentUseCase(
    private val ioDispatcher: CoroutineDispatcher,
    private val documentModelRepository: DocumentModelRepository,
    private val documentImageRepository: DocumentImageRepository,
    private val documentLockRepository: DocumentLockRepository,
    private val favoritesRepository: FavoritesRepository,
    private val documentTextRepository: DocumentTextRepository,
    private val trashRepository: TrashRepository,
    private val fileRepository: FileRepository,
    private val cleanupScheduler: CleanupScheduler
) {
    suspend operator fun invoke(documentId: String) = withContext(ioDispatcher) {
        val document = documentModelRepository.getDocumentModelById(documentId).first()

        document?.imageIds?.forEach { imageId ->
            documentImageRepository.deleteDocumentImage(imageId)
        }

        documentLockRepository.removeLock(documentId)
        favoritesRepository.setFavorite(documentId, false)
        documentTextRepository.deleteDocumentText(documentId)
        documentModelRepository.deleteDocumentModel(documentId)
        trashRepository.removeFromTrash(documentId)
        fileRepository.deleteDocumentStorage(documentId = documentId)
        cleanupScheduler.cancelDocumentTrashPurge(documentId)
    }
}
