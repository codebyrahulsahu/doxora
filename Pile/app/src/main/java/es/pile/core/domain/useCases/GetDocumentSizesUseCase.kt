package es.pile.core.domain.useCases

import es.pile.DocumentModel
import es.pile.core.domain.repositories.FileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Use case responsible for computing the total physical size (in bytes) of a
 * list of documents.
 *
 * The size is calculated by walking the document's persistent storage folder
 * (which contains the original PDF or the scanned images) and summing the
 * length of every file inside it.
 */
class GetDocumentSizesUseCase(
    private val ioDispatcher: CoroutineDispatcher,
    private val fileRepository: FileRepository
) {
    /**
     * Returns a map from [DocumentModel.id] to the total size in bytes.
     *
     * Documents without an existing folder are mapped to 0 bytes.
     */
    suspend operator fun invoke(documents: List<DocumentModel>): Map<String, Long> =
        withContext(ioDispatcher) {
            documents.associate { document ->
                val directory = fileRepository.getDocumentDirectory(
                    storageType = FileRepository.StorageType.PERSISTENT,
                    documentId = document.id
                )

                val size = if (directory.exists()) {
                    directory.walkTopDown()
                        .filter { it.isFile }
                        .sumOf { it.length() }
                } else {
                    0L
                }

                document.id to size
            }
        }
}
