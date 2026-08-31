package es.pile.features.documentDetail.domain.useCases

import es.pile.DocumentModel
import es.pile.TrashEntry
import es.pile.core.domain.models.DocumentStatusConstants
import es.pile.core.domain.repositories.DocumentModelRepository
import es.pile.core.domain.repositories.TrashRepository
import es.pile.features.home.domain.schedulers.CleanupScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class MoveDocumentToTrashUseCaseTest {

    private val documentModelRepository: DocumentModelRepository = mockk()
    private val trashRepository: TrashRepository = mockk()
    private val cleanupScheduler: CleanupScheduler = mockk()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val moveDocumentToTrashUseCase = MoveDocumentToTrashUseCase(
        testDispatcher,
        documentModelRepository,
        trashRepository,
        cleanupScheduler
    )

    @Test
    fun `invoke should move a saved document to the recycle bin`() = runTest {
        // Given
        val doc = mockk<DocumentModel> {
            every { id } returns "doc1"
            every { documentStatus } returns DocumentStatusConstants.SAVED
        }

        coEvery { trashRepository.getTrashEntry("doc1") } returns null
        coEvery { trashRepository.addToTrash(any()) } returns Unit
        coEvery { documentModelRepository.updateStatus("doc1", DocumentStatusConstants.TRASHED) } returns Unit
        coEvery { cleanupScheduler.scheduleDocumentTrashPurge("doc1") } returns Unit

        // When
        moveDocumentToTrashUseCase(doc)

        // Then
        coVerify { trashRepository.addToTrash(any()) }
        coVerify { documentModelRepository.updateStatus("doc1", DocumentStatusConstants.TRASHED) }
        coVerify { cleanupScheduler.scheduleDocumentTrashPurge("doc1") }
    }

    @Test
    fun `invoke should keep the original status when the document is trashed again`() = runTest {
        // Given
        val doc = mockk<DocumentModel> {
            every { id } returns "doc1"
            every { documentStatus } returns DocumentStatusConstants.SAVED
        }

        val existingEntry = TrashEntry(
            documentId = "doc1",
            trashedAt = java.time.LocalDateTime.now().minusDays(2),
            originalStatus = DocumentStatusConstants.SAVED
        )

        coEvery { trashRepository.getTrashEntry("doc1") } returns existingEntry
        coEvery { trashRepository.addToTrash(any()) } returns Unit

        // When
        moveDocumentToTrashUseCase(doc)

        // Then
        coVerify {
            trashRepository.addToTrash(
                match { entry ->
                    entry.documentId == "doc1" && entry.originalStatus == DocumentStatusConstants.SAVED
                }
            )
        }
    }

    @Test
    fun `invoke should schedule the delayed deletion`() = runTest {
        // Given
        val doc = mockk<DocumentModel> {
            every { id } returns "doc1"
            every { documentStatus } returns DocumentStatusConstants.SAVED
        }

        coEvery { trashRepository.getTrashEntry("doc1") } returns null
        coEvery { trashRepository.addToTrash(any()) } returns Unit
        coEvery { documentModelRepository.updateStatus(any(), any()) } returns Unit

        // When
        moveDocumentToTrashUseCase(doc)

        // Then
        coVerify { cleanupScheduler.scheduleDocumentTrashPurge("doc1") }
        assertEquals(DocumentStatusConstants.TRASHED, doc.documentStatus)
    }
}
