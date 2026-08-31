package es.pile.core.domain.useCases

import es.pile.DocumentModel
import es.pile.core.domain.repositories.BitmapCacheRepository
import es.pile.core.domain.repositories.DocumentImageRepository
import es.pile.core.domain.repositories.FileRepository
import kotlinx.coroutines.flow.firstOrNull

/**
 * Resolves the first page (or first image) of a document and asks the bitmap cache to
 * keep a downscaled version of it, so the lists can show a real thumbnail of the file
 * instead of a generic placeholder.
 */
class RequestCoverThumbnailUseCase(
    private val documentImageRepository: DocumentImageRepository,
    private val bitmapCacheRepository: BitmapCacheRepository,
    private val fileRepository: FileRepository
) {
    suspend operator fun invoke(document: DocumentModel) {
        if (document.isIncomingPdf) {
            val file = fileRepository.getPDFFile(documentId = document.id)

            bitmapCacheRepository.loadCoverThumbnail(
                file = file,
                document = document
            )
        } else {
            val imageId = document.imageIds.firstOrNull() ?: return
            val file = fileRepository.getImageFile(documentId = document.id, imageId = imageId)

            val documentImage = documentImageRepository.getDocumentImageById(imageId).firstOrNull()

            bitmapCacheRepository.loadCoverThumbnail(
                file = file,
                document = document,
                documentImage = documentImage
            )
        }
    }
}
