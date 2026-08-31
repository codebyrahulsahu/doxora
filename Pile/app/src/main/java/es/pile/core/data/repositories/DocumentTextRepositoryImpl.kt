package es.pile.core.data.repositories

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import es.pile.DatabaseQueries
import es.pile.core.domain.repositories.DocumentTextRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant

class DocumentTextRepositoryImpl(
    private val databaseQueries: DatabaseQueries,
    private val ioDispatcher: CoroutineDispatcher
) : DocumentTextRepository {

    override fun getDocumentText(documentId: String): Flow<String?> =
        databaseQueries.selectDocumentText(documentId)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.text }

    override suspend fun saveDocumentText(documentId: String, text: String): Unit = withContext(ioDispatcher) {
        databaseQueries.upsertDocumentText(
            documentId = documentId,
            text = text,
            updatedAt = Instant.now().toString()
        )
    }

    override suspend fun deleteDocumentText(documentId: String): Unit = withContext(ioDispatcher) {
        databaseQueries.removeDocumentText(documentId)
    }

    override suspend fun getAllDocumentTexts(): Map<String, String> = withContext(ioDispatcher) {
        databaseQueries.selectAllDocumentTexts().executeAsList()
            .associate { it.documentId to it.text }
    }

    override suspend fun restoreDocumentTexts(texts: Map<String, String>): Unit = withContext(ioDispatcher) {
        texts.forEach { (documentId, text) ->
            databaseQueries.upsertDocumentText(
                documentId = documentId,
                text = text,
                updatedAt = Instant.now().toString()
            )
        }
    }
}
