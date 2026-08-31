package es.pile.core.data.repositories

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import es.pile.DatabaseQueries
import es.pile.core.domain.repositories.DocumentLockRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.time.Instant

class DocumentLockRepositoryImpl(
    private val databaseQueries: DatabaseQueries,
    private val ioDispatcher: CoroutineDispatcher
) : DocumentLockRepository {

    override val lockedDocumentIds: Flow<Set<String>> =
        databaseQueries.selectAllDocumentLocks()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { locks -> locks.map { it.documentId }.toSet() }

    override suspend fun isLocked(documentId: String): Boolean = withContext(ioDispatcher) {
        databaseQueries.selectDocumentLock(documentId).executeAsOneOrNull() != null
    }

    override suspend fun verifyPin(documentId: String, pin: String): Boolean = withContext(ioDispatcher) {
        val storedHash = databaseQueries.selectDocumentLock(documentId)
            .executeAsOneOrNull()?.pinHash ?: return@withContext false

        storedHash == hashPin(documentId, pin)
    }

    override suspend fun lockDocument(documentId: String, pin: String): Unit = withContext(ioDispatcher) {
        databaseQueries.upsertDocumentLock(
            documentId = documentId,
            pinHash = hashPin(documentId, pin),
            createdAt = Instant.now().toString()
        )
    }

    override suspend fun unlockDocument(documentId: String, pin: String): Boolean = withContext(ioDispatcher) {
        val storedHash = databaseQueries.selectDocumentLock(documentId)
            .executeAsOneOrNull()?.pinHash ?: return@withContext false

        if (storedHash != hashPin(documentId, pin)) return@withContext false

        databaseQueries.removeDocumentLock(documentId)
        true
    }

    override suspend fun removeLock(documentId: String): Unit = withContext(ioDispatcher) {
        databaseQueries.removeDocumentLock(documentId)
    }

    override suspend fun getAllLocks(): List<Pair<String, String>> = withContext(ioDispatcher) {
        databaseQueries.selectAllDocumentLocks().executeAsList()
            .map { it.documentId to it.pinHash }
    }

    override suspend fun restoreLocks(locks: List<Pair<String, String>>) = withContext(ioDispatcher) {
        val createdAt = Instant.now().toString()
        locks.forEach { (documentId, pinHash) ->
            databaseQueries.upsertDocumentLock(
                documentId = documentId,
                pinHash = pinHash,
                createdAt = createdAt
            )
        }
    }

    /**
     * The PIN is never stored, only this salted digest. The salt mixes a constant with the
     * document id so the same PIN produces a different hash for every document.
     */
    private fun hashPin(documentId: String, pin: String): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest("$PIN_SALT|$documentId|$pin".toByteArray(Charsets.UTF_8))

        return bytes.joinToString("") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val PIN_SALT = "es.pile.document.lock.v1"
    }
}
