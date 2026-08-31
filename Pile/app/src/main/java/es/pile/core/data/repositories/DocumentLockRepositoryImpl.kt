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
            .map { locks ->
                // Pattern locks are no longer supported: clear any leftover row
                // so the document is not left locked forever.
                locks.filter { DocumentLockCodec.isPattern(it.pinHash) }
                    .forEach { runCatching { databaseQueries.removeDocumentLock(it.documentId) } }

                locks.filterNot { DocumentLockCodec.isPattern(it.pinHash) }
                    .map { it.documentId }
                    .toSet()
            }

    override suspend fun isLocked(documentId: String): Boolean = withContext(ioDispatcher) {
        databaseQueries.selectDocumentLock(documentId).executeAsOneOrNull() != null
    }

    override suspend fun verifySecret(documentId: String, secret: String): Boolean =
        withContext(ioDispatcher) {
            val lock = databaseQueries.selectDocumentLock(documentId)
                .executeAsOneOrNull() ?: return@withContext false

            lock.pinHash == DocumentLockCodec.encode(hashSecret(documentId, secret))
        }

    override suspend fun lockDocument(documentId: String, secret: String): Unit =
        withContext(ioDispatcher) {
            databaseQueries.upsertDocumentLock(
                documentId = documentId,
                pinHash = DocumentLockCodec.encode(hashSecret(documentId, secret)),
                createdAt = Instant.now().toString()
            )
        }

    override suspend fun unlockDocument(documentId: String, secret: String): Boolean =
        withContext(ioDispatcher) {
            val lock = databaseQueries.selectDocumentLock(documentId)
                .executeAsOneOrNull() ?: return@withContext false

            if (lock.pinHash != DocumentLockCodec.encode(hashSecret(documentId, secret))) {
                return@withContext false
            }

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

    override suspend fun restoreLocks(locks: List<Pair<String, String>>): Unit =
        withContext(ioDispatcher) {
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
     * The secret is never stored, only this salted digest. The salt mixes a constant
     * with the document id so the same secret produces a different hash for every
     * document. The input is exactly the same used before pattern locks existed, so
     * legacy PIN locks keep verifying.
     */
    private fun hashSecret(documentId: String, secret: String): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest("$PIN_SALT|$documentId|$secret".toByteArray(Charsets.UTF_8))

        return bytes.joinToString("") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val PIN_SALT = "es.pile.document.lock.v1"
    }
}

/**
 * Encodes the secret inside the value stored in the database (`pinHash` column).
 *
 * Stored format: `"pin:<hex>"`. Rows written before the prefix existed are
 * always treated as PIN locks, and pattern locks (no longer supported) are
 * detected through [isPattern] so they can be cleared. Local backups round-trip
 * the stored value untouched, so they keep working.
 */
object DocumentLockCodec {
    private const val PIN_PREFIX = "pin:"
    private const val PATTERN_PREFIX = "pattern:"

    fun encode(hash: String): String = PIN_PREFIX + hash

    /** True when [stored] is a legacy pattern lock that must be cleared. */
    fun isPattern(stored: String?): Boolean = stored?.startsWith(PATTERN_PREFIX) == true
}
