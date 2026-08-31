package es.pile.features.documentDetail.domain.useCases.export

import android.net.Uri
import es.pile.DocumentModel
import es.pile.core.domain.models.DocumentExportFormat
import es.pile.core.domain.repositories.DocumentImageRepository
import es.pile.core.domain.repositories.FileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Exports every page/scan of a document as image files (JPG or PNG).
 *
 * When [destinationFolderUri] is provided the images are written into the
 * folder chosen by the user; otherwise they are saved in the device's public
 * Downloads directory.
 *
 * @return The number of images exported.
 */
class ExportDocumentImagesUseCase(
    private val ioDispatcher: CoroutineDispatcher,
    private val fileRepository: FileRepository,
    private val documentImageRepository: DocumentImageRepository
) {
    suspend operator fun invoke(
        document: DocumentModel,
        format: DocumentExportFormat,
        destinationFolderUri: Uri? = null
    ): Int =
        withContext(ioDispatcher) {
            require(format != DocumentExportFormat.PDF) {
                "ExportDocumentImagesUseCase only supports image formats"
            }

            val png = format == DocumentExportFormat.PNG
            val mimeType = if (png) "image/png" else "image/jpeg"
            val extension = if (png) ".png" else ".jpg"
            val imageLabel = if (png) "PNG" else "JPG"

            val documentImages = document.imageIds.mapNotNull { imageId ->
                documentImageRepository.getDocumentImageById(imageId).first()
            }

            val imageFiles = fileRepository.createDocumentImages(
                document = document,
                documentImages = documentImages,
                png = png
            )

            var exported = 0
            imageFiles.forEachIndexed { index, imageFile ->
                val publicName = "${document.title}_page_${index + 1}_$imageLabel"

                if (destinationFolderUri != null) {
                    fileRepository.exportFileToFolder(
                        file = imageFile,
                        folderUri = destinationFolderUri,
                        publicName = publicName,
                        mimeType = mimeType,
                        extension = extension
                    ).getOrElse { error -> throw error }
                } else {
                    fileRepository.exportFileToDownloads(
                        file = imageFile,
                        publicName = publicName,
                        mimeType = mimeType,
                        extension = extension
                    ).getOrElse { error ->
                        throw error
                    }
                }
                exported++
            }

            exported
        }
}
