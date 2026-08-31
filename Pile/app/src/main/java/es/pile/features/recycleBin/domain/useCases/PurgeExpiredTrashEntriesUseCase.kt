package es.pile.features.recycleBin.domain.useCases

import es.pile.core.domain.models.TRASH_RETENTION_DAYS
import es.pile.core.domain.repositories.TrashRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Permanently deletes every Recycle Bin entry whose retention period (30 days)
 * has already expired.
 *
 * Used as a safety net on app start, so documents are purged even when the
 * delayed WorkManager job was never able to run (e.g. the device was off).
 *
 * @return The number of documents that were purged.
 */
class PurgeExpiredTrashEntriesUseCase(
    private val ioDispatcher: CoroutineDispatcher,
    private val trashRepository: TrashRepository,
    private val deleteTrashedDocumentUseCase: DeleteTrashedDocumentUseCase
) {
    suspend operator fun invoke(): Int = withContext(ioDispatcher) {
        val now = LocalDateTime.now()
        val expired = trashRepository.getAllTrashEntries().filter { entry ->
            entry.trashedAt.plusDays(TRASH_RETENTION_DAYS.toLong()).isBefore(now)
        }

        expired.forEach { deleteTrashedDocumentUseCase(it.documentId) }

        expired.size
    }
}
