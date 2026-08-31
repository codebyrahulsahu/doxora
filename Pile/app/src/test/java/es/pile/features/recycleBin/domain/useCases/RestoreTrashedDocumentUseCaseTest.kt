package es.pile.features.recycleBin.domain.useCases

import es.pile.TrashEntry
import es.pile.core.domain.models.DocumentStatusConstants
import es.pile.core.domain.repositories.DocumentModelRepository
import es.pile.core.domain.repositories.TrashRepository
import es.pile.features.home.domain.schedulers.CleanupScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDateTime

class RestoreTrashedDocumentUseCaseTest {

    private val documentModelRepository: DocumentModelRepository = mockk()
    private val trashRepository: TrashRepository = mockk()
    private val cleanupScheduler: CleanupScheduler = mockk()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val restoreTrashedDocumentUseCase = RestoreTrashedDocumentUseCase(
        testDispatcher,
        documentModelRepository,
        trashRepository,
        cleanupScheduler
    )

    @Test
    fun `invoke should restore the original status and cancel pending deletion`() = runTest {
        // Given
        val entry = TrashEntry(
            documentId = "doc1",
            trashedAt = LocalDateTime.now().minusDays(2),
            originalStatus = DocumentStatusConstants.SAVED
        )

        coEvery { trashRepository.getTrashEntry("doc1") } returns entry
        coEvery { documentModelRepository.updateStatus("doc1", DocumentStatusConstants.SAVED) } returns Unit
        coEvery { trashRepository.removeFromTrash("doc1") } returns Unit

        // When
        restoreTrashedDocumentUseCase("doc1")

        // Then
        coVerify { documentModelRepository.updateStatus("doc1", DocumentStatusConstants.SAVED) }
        coVerify { trashRepository.removeFromTrash("doc1") }
        coVerify { cleanupScheduler.cancelDocumentTrashPurge("doc1") }
    }

    @Test
    fun `invoke should do nothing when the document is not in the bin`() = runTest {
        // Given
        coEvery { trashRepository.getTrashEntry("doc1") } returns null

        // When
        restoreTrashedDocumentUseCase("doc1")

        // Then
        coVerify(exactly = 0) { documentModelRepository.updateStatus(any(), any()) }
        coVerify(exactly = 0) { trashRepository.removeFromTrash(any()) }
        coVerify(exactly = 0) { cleanupScheduler.cancelDocumentTrashPurge(any()) }
    }
}
