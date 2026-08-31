package es.pile.features.recycleBin.domain.useCases

import es.pile.TrashEntry
import es.pile.core.domain.models.DocumentStatusConstants
import es.pile.core.domain.models.TRASH_RETENTION_DAYS
import es.pile.core.domain.repositories.TrashRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

class PurgeExpiredTrashEntriesUseCaseTest {

    private val trashRepository: TrashRepository = mockk()
    private val deleteTrashedDocumentUseCase: DeleteTrashedDocumentUseCase = mockk(relaxed = true)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val purgeExpiredTrashEntriesUseCase = PurgeExpiredTrashEntriesUseCase(
        testDispatcher,
        trashRepository,
        deleteTrashedDocumentUseCase
    )

    @Test
    fun `invoke should only delete expired entries`() = runTest {
        // Given
        val now = LocalDateTime.now()
        val expired = TrashEntry(
            documentId = "expired",
            trashedAt = now.minusDays(TRASH_RETENTION_DAYS.toLong() + 1),
            originalStatus = DocumentStatusConstants.SAVED
        )
        val stillFresh = TrashEntry(
            documentId = "fresh",
            trashedAt = now.minusDays(1),
            originalStatus = DocumentStatusConstants.SAVED
        )

        coEvery { trashRepository.getAllTrashEntries() } returns listOf(expired, stillFresh)

        // When
        val purged = purgeExpiredTrashEntriesUseCase()

        // Then
        assertEquals(1, purged)
        coVerify { deleteTrashedDocumentUseCase("expired") }
        coVerify(exactly = 0) { deleteTrashedDocumentUseCase("fresh") }
    }

    @Test
    fun `invoke should return zero when nothing is expired`() = runTest {
        // Given
        coEvery { trashRepository.getAllTrashEntries() } returns emptyList()

        // When
        val purged = purgeExpiredTrashEntriesUseCase()

        // Then
        assertEquals(0, purged)
    }
}
