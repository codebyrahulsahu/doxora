package es.pile.features.documentDetail.domain.useCases.export

import android.net.Uri
import es.pile.DocumentModel
import es.pile.core.domain.repositories.FileRepository

/**
 * Use case responsible for exporting a document's PDF file.
 *
 * It retrieves the most recent version of the document's PDF and delegates the
 * transfer either to the system's Downloads folder (when no folder is chosen)
 * or to the folder picked by the user through the repository.
 */
class ExportDocumentUseCase(
    private val getUpToDatePdfUseCase: GetUpToDatePdfUseCase,
    private val fileRepository: FileRepository
) {
    /**
     * Exports the provided [document].
     *
     * @param document The document model to be exported.
     * @param destinationFolderUri Tree [Uri] of the folder chosen by the user.
     * When null the file is saved in the device's public Downloads directory.
     * @return A [Result] containing the name or path of the exported file on success,
     * or an exception on failure.
     */
    suspend operator fun invoke(
        document: DocumentModel,
        destinationFolderUri: Uri? = null
    ): Result<String> {
        val sourceFile = getUpToDatePdfUseCase(document)

        return if (destinationFolderUri != null) {
            fileRepository.exportFileToFolder(
                file = sourceFile,
                folderUri = destinationFolderUri,
                publicName = document.title
            ).map { it.toString() }
        } else {
            fileRepository.exportFileToDownloads(sourceFile, document.title)
        }
    }
}
