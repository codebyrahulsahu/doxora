package es.pile.features.documentDetail.domain.useCases.export

import android.net.Uri
import es.pile.DocumentModel
import es.pile.core.domain.repositories.FileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExportDocumentUseCaseTest {

    private val getUpToDatePdfUseCase: GetUpToDatePdfUseCase = mockk()
    private val fileRepository: FileRepository = mockk()
    private val exportDocumentUseCase = ExportDocumentUseCase(getUpToDatePdfUseCase, fileRepository)

    @Test
    fun `invoke should export document pdf correctly`() = runTest {
        // Given
        val doc = mockk<DocumentModel> {
            every { title } returns "Document Title"
        }
        val sourceFile = File("path/to/source.pdf")
        coEvery { getUpToDatePdfUseCase(doc) } returns sourceFile
        coEvery { fileRepository.exportFileToDownloads(sourceFile, "Document Title") } returns Result.success("ExportedPath")

        // When
        val result = exportDocumentUseCase(doc)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("ExportedPath", result.getOrNull())
    }

    @Test
    fun `invoke should export the pdf into the folder chosen by the user`() = runTest {
        // Given
        val doc = mockk<DocumentModel> {
            every { title } returns "Document Title"
        }
        val sourceFile = File("path/to/source.pdf")
        val folderUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADocuments")
        val exportedUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADocuments/document/primary%3ADocuments%2FDocument%20Title.pdf")

        coEvery { getUpToDatePdfUseCase(doc) } returns sourceFile
        coEvery {
            fileRepository.exportFileToFolder(sourceFile, folderUri, "Document Title")
        } returns Result.success(exportedUri)

        // When
        val result = exportDocumentUseCase(doc, folderUri)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(exportedUri.toString(), result.getOrNull())
        coVerify(exactly = 1) {
            fileRepository.exportFileToFolder(sourceFile, folderUri, "Document Title")
        }
    }
}
