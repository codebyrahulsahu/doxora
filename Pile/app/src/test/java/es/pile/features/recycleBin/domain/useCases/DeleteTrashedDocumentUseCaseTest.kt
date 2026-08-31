package es.pile.features.recycleBin.domain.useCases

import es.pile.DocumentModel
import es.pile.core.domain.repositories.DocumentImageRepository
import es.pile.core.domain.repositories.DocumentLockRepository
import es.pile.core.domain.repositories.DocumentModelRepository
import es.pile.core.domain.repositories.DocumentTextRepository
import es.pile.core.domain.repositories.FavoritesRepository
import es.pile.core.domain.repositories.FileRepository
import es.pile.core.domain.repositories.TrashRepository
import es.pile.features.home.domain.schedulers.CleanupScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteTrashedDocumentUseCaseTest {

    private val documentModelRepository: DocumentModelRepository = mockk()
    private val documentImageRepository: DocumentImageRepository = mockk(relaxed = true)
    private val documentLockRepository: DocumentLockRepository = mockk(relaxed = true)
    private val favoritesRepository: FavoritesRepository = mockk(relaxed = true)
    private val documentTextRepository: DocumentTextRepository = mockk(relaxed = true)
    private val trashRepository: TrashRepository = mockk(relaxed = true)
    private val fileRepository: FileRepository = mockk(relaxed = true)
    private val cleanupScheduler: CleanupScheduler = mockk()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val deleteTrashedDocumentUseCase = DeleteTrashedDocumentUseCase(
        testDispatcher,
        documentModelRepository,
        documentImageRepository,
        documentLockRepository,
        favoritesRepository,
        documentTextRepository,
        trashRepository,
        fileRepository,
        cleanupScheduler
    )

    @Test
    fun `invoke should remove every trace of the document`() = runTest {
        // Given
        val document = mockk<DocumentModel> {
            every { id } returns "doc1"
            every { imageIds } returns listOf("img1", "img2")
        }

        coEvery { documentModelRepository.getDocumentModelById("doc1") } returns flowOf(document)

        // When
        deleteTrashedDocumentUseCase("doc1")

        // Then
        coVerify { documentImageRepository.deleteDocumentImage("img1") }
        coVerify { documentImageRepository.deleteDocumentImage("img2") }
        coVerify { documentLockRepository.removeLock("doc1") }
        coVerify { favoritesRepository.setFavorite("doc1", false) }
        coVerify { documentTextRepository.deleteDocumentText("doc1") }
        coVerify { documentModelRepository.deleteDocumentModel("doc1") }
        coVerify { trashRepository.removeFromTrash("doc1") }
        coVerify { fileRepository.deleteDocumentStorage(FileRepository.StorageType.PERSISTENT, "doc1") }
        coVerify { cleanupScheduler.cancelDocumentTrashPurge("doc1") }
    }

    @Test
    fun `invoke should still clean storage when the document row is missing`() = runTest {
        // Given
        coEvery { documentModelRepository.getDocumentModelById("doc1") } returns flowOf(null)

        // When
        deleteTrashedDocumentUseCase("doc1")

        // Then
        coVerify { documentLockRepository.removeLock("doc1") }
        coVerify { documentModelRepository.deleteDocumentModel("doc1") }
        coVerify { trashRepository.removeFromTrash("doc1") }
        coVerify { fileRepository.deleteDocumentStorage(FileRepository.StorageType.PERSISTENT, "doc1") }
    }
}
