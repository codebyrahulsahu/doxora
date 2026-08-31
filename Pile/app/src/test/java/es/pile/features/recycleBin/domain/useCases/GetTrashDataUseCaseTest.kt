package es.pile.features.recycleBin.domain.useCases

import es.pile.DocumentModel
import es.pile.TrashEntry
import es.pile.core.domain.models.DocumentStatusConstants
import es.pile.core.domain.repositories.BitmapCacheRepository
import es.pile.core.domain.repositories.DocumentModelRepository
import es.pile.core.domain.repositories.TrashRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GetTrashDataUseCaseTest {

    private val documentModelRepository: DocumentModelRepository = mockk()
    private val trashRepository: TrashRepository = mockk()
    private val bitmapCacheRepository: BitmapCacheRepository = mockk()

    @Test
    fun `invoke should join documents with their trash entries`() = runTest {
        // Given
        val document = DocumentModel(
            id = "doc1",
            title = "Taxes",
            imageIds = listOf("img1"),
            creationDateTime = LocalDateTime.now(),
            modificationDateTime = LocalDateTime.now(),
            documentStatus = DocumentStatusConstants.TRASHED,
            documentPileIds = emptyList(),
            documentDetails = emptyList(),
            documentNote = "",
            documentOrganizationIds = emptyList(),
            isIncomingPdf = false
        )
        val entry = TrashEntry(
            documentId = "doc1",
            trashedAt = LocalDateTime.now().minusDays(3),
            originalStatus = DocumentStatusConstants.SAVED
        )

        every { documentModelRepository.documentModels } returns flowOf(listOf(document))
        every { trashRepository.trashEntries } returns flowOf(listOf(entry))
        every { bitmapCacheRepository.getCoverKey(document) } returns "cover_doc1"

        // When
        val result = GetTrashDataUseCase(documentModelRepository, trashRepository, bitmapCacheRepository)
            .invoke()
            .first()

        // Then
        assertEquals(1, result.size)
        assertEquals("doc1", result[0].document.id)
        assertEquals(DocumentStatusConstants.SAVED, result[0].originalStatus)
        assertEquals("cover_doc1", result[0].coverImageCacheKey)
        assertNotNull(result[0].trashedAt)
    }

    @Test
    fun `invoke should skip entries without a document`() = runTest {
        // Given
        every { documentModelRepository.documentModels } returns flowOf(emptyList())
        every { trashRepository.trashEntries } returns flowOf(
            listOf(
                TrashEntry(
                    documentId = "ghost",
                    trashedAt = LocalDateTime.now(),
                    originalStatus = DocumentStatusConstants.SAVED
                )
            )
        )

        // When
        val result = GetTrashDataUseCase(documentModelRepository, trashRepository, bitmapCacheRepository)
            .invoke()
            .first()

        // Then
        assertTrue(result.isEmpty())
    }
}
