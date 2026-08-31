package es.pile.core.data.repositories

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import es.pile.DatabaseQueries
import es.pile.TrashEntry
import es.pile.core.domain.repositories.TrashRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TrashRepositoryImpl(
    private val databaseQueries: DatabaseQueries,
    private val ioDispatcher: CoroutineDispatcher
) : TrashRepository {

    override val trashEntries: Flow<List<TrashEntry>> =
        databaseQueries.selectAllTrashEntries().asFlow().mapToList(ioDispatcher)

    override suspend fun getAllTrashEntries(): List<TrashEntry> = withContext(ioDispatcher) {
        databaseQueries.selectAllTrashEntries().executeAsList()
    }

    override suspend fun getTrashEntry(documentId: String): TrashEntry? = withContext(ioDispatcher) {
        databaseQueries.selectTrashEntry(documentId).executeAsOneOrNull()
    }

    override suspend fun addToTrash(entry: TrashEntry) = withContext(ioDispatcher) {
        databaseQueries.insertTrashEntry(
            documentId = entry.documentId,
            trashedAt = entry.trashedAt,
            originalStatus = entry.originalStatus
        )
    }

    override suspend fun removeFromTrash(documentId: String) = withContext(ioDispatcher) {
        databaseQueries.removeTrashEntry(documentId)
    }
}
