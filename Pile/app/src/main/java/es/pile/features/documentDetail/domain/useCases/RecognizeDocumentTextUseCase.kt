package es.pile.features.documentDetail.domain.useCases

import es.pile.DocumentModel
import es.pile.core.domain.repositories.BitmapCacheRepository
import es.pile.core.domain.repositories.DocumentImageRepository
import es.pile.core.domain.repositories.FileRepository
import es.pile.core.domain.repositories.TextRecognitionRepository
import kotlinx.coroutines.flow.firstOrNull

/**
 * Runs text recognition (OCR) over one page of a document.
 *
 * The page is loaded at full quality through the bitmap cache so the recognizer gets
 * as much detail as the stored document has, and the extraction always happens on
 * the device.
 */
class RecognizeDocumentTextUseCase(
    private val bitmapCacheRepository: BitmapCacheRepository,
    private val documentImageRepository: DocumentImageRepository,
    private val fileRepository: FileRepository,
    private val textRecognitionRepository: TextRecognitionRepository
) {
    /**
     * @param document Document to process.
     * @param pageNumber 0-based index of the page to read.
     * @return A [Result] with the recognized text.
     */
    suspend operator fun invoke(document: DocumentModel, pageNumber: Int = 0): Result<String> {
        val documentImage = if (document.isIncomingPdf) {
            null
        } else {
            val imageId = document.imageIds.getOrNull(pageNumber)
                ?: return Result.failure(IllegalStateException("The document has no image for page $pageNumber"))

            documentImageRepository.getDocumentImageById(imageId).firstOrNull()
        }

        val file = if (document.isIncomingPdf) {
            fileRepository.getPDFFile(documentId = document.id)
        } else {
            val imageId = document.imageIds.getOrNull(pageNumber)
                ?: return Result.failure(IllegalStateException("The document has no image for page $pageNumber"))

            fileRepository.getImageFile(documentId = document.id, imageId = imageId)
        }

        if (!file.exists()) {
            return Result.failure(IllegalStateException("The document file is missing"))
        }

        bitmapCacheRepository.loadBitmap(
            file = file,
            document = document,
            pageNumber = pageNumber,
            documentImage = documentImage
        )

        val cacheKey = bitmapCacheRepository.getImageKey(document, pageNumber)
        val bitmap = bitmapCacheRepository.bitmapCache.value[cacheKey]
            ?: return Result.failure(IllegalStateException("The document page could not be loaded"))

        return textRecognitionRepository.recognizeText(bitmap)
    }
}
