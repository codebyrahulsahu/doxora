package es.pile.features.documentDetail.domain.useCases.export

import es.pile.DocumentImage
import es.pile.DocumentModel
import es.pile.core.domain.models.DocumentExportFormat
import es.pile.core.domain.repositories.DocumentImageRepository
import es.pile.core.domain.repositories.FileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExportDocumentImagesUseCaseTest {

    private val fileRepository: FileRepository = mockk()
    private val documentImageRepository: DocumentImageRepository = mockk()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val exportDocumentImagesUseCase = ExportDocumentImagesUseCase(
        testDispatcher,
        fileRepository,
        documentImageRepository
    )

    @Test
    fun `invoke should export every page as jpg and return the count`() = runTest {
        // Given
        val document = DocumentModel(
            id = "doc1",
            title = "Invoice",
            imageIds = listOf("img1", "img2"),
            creationDateTime = LocalDateTime.now(),
            modificationDateTime = LocalDateTime.now(),
            documentStatus = 1,
            documentPileIds = emptyList(),
            documentDetails = emptyList(),
            documentNote = "",
            documentOrganizationIds = emptyList(),
            isIncomingPdf = false
        )
        val images = listOf(
            DocumentImage(id = "img1", isDraft = false, crop = null, filter = 0, rotation = 0),
            DocumentImage(id = "img2", isDraft = false, crop = null, filter = 0, rotation = 0)
        )

        coEvery { documentImageRepository.getDocumentImageById("img1") } returns flowOf(images[0])
        coEvery { documentImageRepository.getDocumentImageById("img2") } returns flowOf(images[1])
        coEvery { fileRepository.createDocumentImages(document, images, false) } returns listOf(
            File("/tmp/page_1.jpg"),
            File("/tmp/page_2.jpg")
        )
        coEvery { fileRepository.exportFileToDownloads(any(), any(), "image/jpeg", ".jpg") } returns
                Result.success("/downloads/page_1.jpg")

        // When
        val exported = exportDocumentImagesUseCase(document, DocumentExportFormat.JPG)

        // Then
        assertEquals(2, exported)
        coVerify(exactly = 2) {
            fileRepository.exportFileToDownloads(any(), any(), "image/jpeg", ".jpg")
        }
    }

    @Test
    fun `invoke should reject the pdf format`() = runTest {
        // Given
        val document = DocumentModel(
            id = "doc1",
            title = "Invoice",
            imageIds = emptyList(),
            creationDateTime = LocalDateTime.now(),
            modificationDateTime = LocalDateTime.now(),
            documentStatus = 1,
            documentPileIds = emptyList(),
            documentDetails = emptyList(),
            documentNote = "",
            documentOrganizationIds = emptyList(),
            isIncomingPdf = false
        )

        // When / Then
        assertFailsWith<IllegalArgumentException> {
            exportDocumentImagesUseCase(document, DocumentExportFormat.PDF)
        }
    }
}
