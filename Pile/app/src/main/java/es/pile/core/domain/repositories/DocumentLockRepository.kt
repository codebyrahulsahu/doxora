package es.pile.core.domain.repositories

import kotlinx.coroutines.flow.Flow

/**
 * Protects individual documents with a PIN.
 *
 * Only a salted hash of the secret is stored, the plain PIN is never
 * persisted.
 */
interface DocumentLockRepository {

    /** Stream with the ids of every document currently protected. */
    val lockedDocumentIds: Flow<Set<String>>

    /** True when [documentId] is protected with a PIN. */
    suspend fun isLocked(documentId: String): Boolean

    /** True when [secret] is the PIN protecting [documentId]. */
    suspend fun verifySecret(documentId: String, secret: String): Boolean

    /** Protects [documentId] with [secret]. */
    suspend fun lockDocument(documentId: String, secret: String)

    /**
     * Removes the protection of [documentId] once [secret] has been checked.
     *
     * @return true when the document was locked and the secret matched.
     */
    suspend fun unlockDocument(documentId: String, secret: String): Boolean

    /** Removes the protection of [documentId] without asking for the secret. */
    suspend fun removeLock(documentId: String)

    /** Salted hash + document id of every lock. Used by the local backup exporter. */
    suspend fun getAllLocks(): List<Pair<String, String>>

    /** Restores a batch of locks coming from a local backup file. */
    suspend fun restoreLocks(locks: List<Pair<String, String>>)
}
