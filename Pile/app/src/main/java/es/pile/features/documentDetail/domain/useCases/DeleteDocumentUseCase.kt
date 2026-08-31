package es.pile.features.documentDetail.domain.useCases

import es.pile.DocumentModel
import es.pile.core.domain.repositories.DocumentImageRepository
import es.pile.core.domain.repositories.DocumentLockRepository
import es.pile.core.domain.repositories.DocumentModelRepository
import es.pile.core.domain.repositories.DocumentTextRepository
import es.pile.core.domain.repositories.FileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Use case responsible for deleting a document from the repository and associated files.
 *
 * It also cleans up every piece of data attached to the document: its PIN lock and the
 * text that could have been recognized from it.
 */
class DeleteDocumentUseCase(
    private val ioDispatcher: CoroutineDispatcher,
    private val documentModelRepository: DocumentModelRepository,
    private val documentImageRepository: DocumentImageRepository,
    private val fileRepository: FileRepository,
    private val documentLockRepository: DocumentLockRepository,
    private val documentTextRepository: DocumentTextRepository
) {
    /**
     * Deletes a document from the repository and associated files.
     *
     * @param document The document to be deleted.
     * @return A [Result] indicating the success or failure of the operation.
     */
    suspend operator fun invoke(document: DocumentModel) = withContext(ioDispatcher) {
        documentModelRepository.deleteDocumentModel(document.id)

        val imageIds = document.imageIds
        imageIds.forEach { imageId ->
            documentImageRepository.deleteDocumentImage(imageId)
        }

        documentLockRepository.removeLock(document.id)
        documentTextRepository.deleteDocumentText(document.id)

        fileRepository.deleteDocumentStorage(documentId = document.id)
    }
}
