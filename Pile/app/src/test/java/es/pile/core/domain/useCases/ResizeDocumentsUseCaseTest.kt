package es.pile.core.domain.useCases

import es.pile.DocumentImage
import es.pile.DocumentModel
import es.pile.core.domain.models.DocumentResizeMode
import es.pile.core.domain.models.DocumentStatusConstants
import es.pile.core.domain.models.UserSettings
import es.pile.core.domain.repositories.BitmapCacheRepository
import es.pile.core.domain.repositories.DocumentImageRepository
import es.pile.core.domain.repositories.DocumentModelRepository
import es.pile.core.domain.repositories.FileRepository
import es.pile.core.domain.repositories.SettingsRepository
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ResizeDocumentsUseCaseTest {

    private val fileRepository: FileRepository = mockk(relaxed = true)
    private val documentModelRepository: DocumentModelRepository = mockk(relaxed = true)
    private val documentImageRepository: DocumentImageRepository = mockk(relaxed = true)
    private val bitmapCacheRepository: BitmapCacheRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk {
        every { userSettings } returns flowOf(UserSettings(documentResizerTargetSizeKb = 512))
    }

    private val testDispatcher = UnconfinedTestDispatcher()

    private val useCase = ResizeDocumentsUseCase(
        ioDispatcher = testDispatcher,
        fileRepository = fileRepository,
        documentModelRepository = documentModelRepository,
        documentImageRepository = documentImageRepository,
        bitmapCacheRepository = bitmapCacheRepository,
        settingsRepository = settingsRepository
    )

    private fun document(
        id: String = "doc-1",
        imageIds: List<String> = listOf("img_a.jpg", "img_b.jpg"),
        isIncomingPdf: Boolean = false
    ) = DocumentModel(
        id = id,
        title = "My document",
        imageIds = imageIds,
        creationDateTime = LocalDateTime.now(),
        modificationDateTime = LocalDateTime.now(),
        documentStatus = DocumentStatusConstants.SAVED,
        documentPileIds = listOf("pile-1"),
        documentDetails = emptyList(),
        documentNote = "note",
        documentOrganizationIds = emptyList(),
        isIncomingPdf = isIncomingPdf
    )

    @Test
    fun `save as original resizes every stored page in place`() = runTest {
        // Given
        val doc = document()
        coEvery {
            fileRepository.resizeStoredImageToTargetSize(
                documentId = doc.id,
                imageId = any(),
                targetSizeKb = 512
            )
        } returns true

        // When
        val result = useCase(listOf(doc), DocumentResizeMode.SAVE_AS_ORIGINAL)

        // Then: both pages are compressed in place and the document is refreshed.
        assertEquals(1, result.resizedCount)
        assertEquals(0, result.skippedCount)

        coVerify {
            fileRepository.resizeStoredImageToTargetSize(
                documentId = doc.id, imageId = "img_a.jpg", targetSizeKb = 512
            )
            fileRepository.resizeStoredImageToTargetSize(
                documentId = doc.id, imageId = "img_b.jpg", targetSizeKb = 512
            )
        }
        coVerify { documentModelRepository.updateDocumentModel(any()) }
        coVerify { bitmapCacheRepository.clearCache() }
        // No duplicate is created.
        coVerify(exactly = 0) { documentModelRepository.insertDocumentModel(any()) }
    }

    @Test
    fun `save as duplicate creates a new resized document and keeps the original`() = runTest {
        // Given
        val doc = document(imageIds = listOf("img_a.jpg"))

        coEvery {
            fileRepository.copyStoredImageResizedToTargetSize(
                sourceDocumentId = doc.id,
                sourceImageId = "img_a.jpg",
                targetDocumentId = any(),
                targetSizeKb = 512
            )
        } returns File("img_new.jpg")

        every { documentImageRepository.getDocumentImageById("img_a.jpg") } returns flowOf(
            DocumentImage(id = "img_a.jpg", isDraft = false, crop = null, filter = 2, rotation = 90)
        )

        val insertedDocument: CapturingSlot<DocumentModel> = slot()
        coEvery { documentModelRepository.insertDocumentModel(capture(insertedDocument)) } returns Unit

        val insertedImage: CapturingSlot<DocumentImage> = slot()
        coEvery { documentImageRepository.insertDocumentImage(capture(insertedImage)) } returns Unit

        // When
        val result = useCase(listOf(doc), DocumentResizeMode.SAVE_AS_DUPLICATE)

        // Then
        assertEquals(1, result.resizedCount)
        assertEquals(0, result.skippedCount)

        // The duplicate is a new document with the resized page and the same hub.
        val duplicate = insertedDocument.captured
        assertTrue(duplicate.id != doc.id)
        assertEquals(listOf("img_new.jpg"), duplicate.imageIds)
        assertEquals(doc.documentPileIds, duplicate.documentPileIds)
        assertTrue(duplicate.title.contains("(resized)"))

        // The page adjustments (filter/rotation) are carried over.
        assertEquals(2L, insertedImage.captured.filter)
        assertEquals(90L, insertedImage.captured.rotation)

        // The original document is never touched.
        coVerify(exactly = 0) { documentModelRepository.updateDocumentModel(any()) }
        coVerify(exactly = 0) {
            fileRepository.resizeStoredImageToTargetSize(
                documentId = any(), imageId = any(), targetSizeKb = any()
            )
        }
    }

    @Test
    fun `imported PDFs are skipped by the resizer`() = runTest {
        // Given
        val pdfDocument = document(id = "pdf-doc", imageIds = emptyList(), isIncomingPdf = true)

        // When
        val result = useCase(listOf(pdfDocument), DocumentResizeMode.SAVE_AS_ORIGINAL)

        // Then
        assertEquals(0, result.resizedCount)
        assertEquals(1, result.skippedCount)
        coVerify(exactly = 0) { documentModelRepository.updateDocumentModel(any()) }
    }
}
