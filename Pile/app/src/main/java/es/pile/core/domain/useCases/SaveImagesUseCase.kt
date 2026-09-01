package es.pile.core.domain.useCases

import android.net.Uri
import es.pile.DocumentImage
import es.pile.core.domain.models.ImageCompressionChoice
import es.pile.core.domain.models.ImageResolution
import es.pile.core.domain.repositories.FileRepository
import es.pile.core.domain.repositories.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Use case responsible for saving images to the internal storage of the app.
 *
 * It checks the user's preferred image resolution setting and the Document Resizer
 * toggle, and saves the images accordingly. When the user answered the compression
 * prompt for this specific import, that explicit choice always wins over the settings.
 */
class SaveImagesUseCase(
    private val ioDispatcher: CoroutineDispatcher,
    private val fileRepository: FileRepository,
    private val settingsRepository: SettingsRepository
) {
    /**
     * Saves a list of images to the internal storage of the app. And creates a [DocumentImage] list
     * with the file name.
     *
     * When the user made an explicit [compression] choice in the compression prompt it is
     * honoured as-is: either the images are compressed to the chosen target size or they
     * are stored untouched. Without an explicit choice, the Document Resizer setting and
     * the image resolution setting decide what happens (legacy behaviour).
     *
     * @param storageType The type of storage (PERSISTENT or CACHE).
     * @param uris The list of URIs of the images to be saved.
     * @param documentId The unique identifier of the document where the images will be stored.
     * @param compression Explicit compression choice made by the user for this import,
     * or null to fall back to the stored settings.
     * @return A list of [File] objects representing the saved images.
     */
    suspend operator fun invoke(
        storageType: FileRepository.StorageType,
        uris: List<Uri>,
        documentId: String,
        compression: ImageCompressionChoice? = null
    ): List<File> = withContext(ioDispatcher) {
        val userSettings = settingsRepository.userSettings.first()

        return@withContext when {
            // Explicit answer from the compression prompt: compress to the chosen size.
            compression != null && compression.compress -> {
                fileRepository.saveImagesToTargetSize(
                    storageType = storageType,
                    uris = uris,
                    documentId = documentId,
                    targetSizeKb = compression.targetSizeKb
                )
            }

            // Explicit answer from the compression prompt: keep the original files.
            compression != null -> {
                fileRepository.saveImageToStorage(
                    storageType = storageType,
                    uris = uris,
                    documentId = documentId
                )
            }

            userSettings.isDocumentResizerEnabled -> {
                fileRepository.saveImagesToTargetSize(
                    storageType = storageType,
                    uris = uris,
                    documentId = documentId,
                    targetSizeKb = userSettings.documentResizerTargetSizeKb
                )
            }

            userSettings.imageResolution == ImageResolution.ORIGINAL -> {
                fileRepository.saveImageToStorage(
                    storageType = storageType,
                    uris = uris,
                    documentId = documentId
                )
            }

            else -> {
                fileRepository.saveResizeRotateImagesToStorage(
                    storageType = storageType,
                    uris = uris,
                    documentId = documentId
                )
            }
        }
    }
}