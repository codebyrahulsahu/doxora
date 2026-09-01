package es.pile.core.domain.useCases

import es.pile.DocumentImage
import es.pile.DocumentModel
import es.pile.core.domain.models.DocumentResizeMode
import es.pile.core.domain.models.DocumentResizeTargetSize
import es.pile.core.domain.repositories.BitmapCacheRepository
import es.pile.core.domain.repositories.DocumentImageRepository
import es.pile.core.domain.repositories.DocumentModelRepository
import es.pile.core.domain.repositories.FileRepository
import es.pile.core.domain.repositories.SettingsRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.UUID

/**
 * Outcome of a Document Resizer run over the selected documents.
 *
 * @property resizedCount Documents whose pages were successfully resized.
 * @property skippedCount Documents that could not be resized (imported PDFs,
 * documents without page images or documents whose files failed to compress).
 */
data class DocumentResizeResult(
    val resizedCount: Int,
    val skippedCount: Int
)

/**
 * Use case behind the Document Resizer action shown in the selection top bar.
 *
 * It compresses every page image of the selected documents to a custom
 * target size with zero quality loss (JPEG quality is never reduced; only
 * dimensions are scaled down when needed) and saves the result either over
 * the original files of the document or as a brand new duplicate document,
 * depending on the chosen [DocumentResizeMode].
 *
 * Imported PDFs are skipped: their content is the PDF file itself, so there
 * are no page images to compress.
 */
class ResizeDocumentsUseCase(
    private val ioDispatcher: CoroutineDispatcher,
    private val fileRepository: FileRepository,
    private val documentModelRepository: DocumentModelRepository,
    private val documentImageRepository: DocumentImageRepository,
    private val bitmapCacheRepository: BitmapCacheRepository,
    private val settingsRepository: SettingsRepository
) {
    /**
     * Resizes every document in [documents] to [targetSizeKb].
     *
     * @param documents The documents selected by the user.
     * @param mode Whether the result replaces the original files in the app or
     * is saved as a duplicate document.
     * @param targetSizeKb Custom target file size in kilobytes typed in the
     * resizer prompt. When null, the last stored size is used.
     * @return A [DocumentResizeResult] with how many documents were resized.
     */
    suspend operator fun invoke(
        documents: List<DocumentModel>,
        mode: DocumentResizeMode,
        targetSizeKb: Int? = null
    ): DocumentResizeResult = withContext(ioDispatcher) {
        val resolvedTargetSizeKb = (
            targetSizeKb
                ?: settingsRepository.userSettings.first().documentResizerTargetSizeKb
            ).coerceAtLeast(DocumentResizeTargetSize.MIN_KB)

        // Remember the custom size so the prompt can pre-fill it next time.
        if (targetSizeKb != null) {
            settingsRepository.updateDocumentResizerTargetSizeKb(resolvedTargetSizeKb)
        }

        var resized = 0
        var skipped = 0

        documents.forEach { document ->
            // Imported PDFs have no page images that could be compressed.
            if (document.isIncomingPdf || document.imageIds.isEmpty()) {
                skipped++
                return@forEach
            }

            val success = runCatching {
                when (mode) {
                    DocumentResizeMode.SAVE_AS_ORIGINAL ->
                        resizeInPlace(document, resolvedTargetSizeKb)

                    DocumentResizeMode.SAVE_AS_DUPLICATE ->
                        resizeAsDuplicate(document, resolvedTargetSizeKb)
                }
            }
                .onFailure { error -> Napier.e("Error resizing document ${document.id}", error) }
                .getOrDefault(false)

            if (success) resized++ else skipped++
        }

        // The stored files changed, so every cached bitmap or thumbnail of the
        // resized documents is stale.
        if (resized > 0 && mode == DocumentResizeMode.SAVE_AS_ORIGINAL) {
            bitmapCacheRepository.clearCache()
        }

        DocumentResizeResult(resizedCount = resized, skippedCount = skipped)
    }

    /**
     * Compresses every stored page of [document] in place ("Save as original
     * file in app") and refreshes its modification date so the generated PDF
     * is rebuilt from the new files.
     */
    private suspend fun resizeInPlace(document: DocumentModel, targetSizeKb: Int): Boolean {
        var anyProcessed = false

        document.imageIds.forEach { imageId ->
            val processed = fileRepository.resizeStoredImageToTargetSize(
                documentId = document.id,
                imageId = imageId,
                targetSizeKb = targetSizeKb
            )
            anyProcessed = anyProcessed || processed
        }

        if (!anyProcessed) return false

        documentModelRepository.updateDocumentModel(
            document.copy(modificationDateTime = LocalDateTime.now())
        )
        return true
    }

    /**
     * Stores the compressed pages of [document] as a brand new duplicate
     * document ("Save as duplicate file"), keeping the original untouched.
     */
    private suspend fun resizeAsDuplicate(document: DocumentModel, targetSizeKb: Int): Boolean {
        val duplicateId = UUID.randomUUID().toString()

        try {
            val duplicateImages = document.imageIds.mapNotNull { imageId ->
                val newFile = fileRepository.copyStoredImageResizedToTargetSize(
                    sourceDocumentId = document.id,
                    sourceImageId = imageId,
                    targetDocumentId = duplicateId,
                    targetSizeKb = targetSizeKb
                ) ?: return@mapNotNull null

                // The page keeps its crop, filter and rotation adjustments.
                val sourceImage = documentImageRepository.getDocumentImageById(imageId).first()

                DocumentImage(
                    id = newFile.name,
                    isDraft = false,
                    crop = sourceImage?.crop,
                    filter = sourceImage?.filter ?: 0,
                    rotation = sourceImage?.rotation ?: 0
                )
            }

            if (duplicateImages.isEmpty()) {
                fileRepository.deleteDocumentStorage(documentId = duplicateId)
                return false
            }

            duplicateImages.forEach { documentImageRepository.insertDocumentImage(it) }

            val now = LocalDateTime.now()
            documentModelRepository.insertDocumentModel(
                document.copy(
                    id = duplicateId,
                    title = duplicateTitle(document.title),
                    imageIds = duplicateImages.map { it.id },
                    creationDateTime = now,
                    modificationDateTime = now
                )
            )
            return true
        } catch (e: Exception) {
            fileRepository.deleteDocumentStorage(documentId = duplicateId)
            throw e
        }
    }

    /** Title given to the duplicate so it can be told apart from the original. */
    private fun duplicateTitle(title: String): String =
        if (title.isBlank()) DUPLICATE_SUFFIX else "$title $DUPLICATE_SUFFIX"

    companion object {
        /** Appended to the title of every duplicate created by the resizer. */
        private const val DUPLICATE_SUFFIX = "(resized)"
    }
}
