package es.pile.core.domain.repositories

import kotlinx.coroutines.flow.Flow

/**
 * Protects individual documents with a PIN.
 *
 * Only a salted hash of the PIN is stored, the plain PIN is never persisted.
 */
interface DocumentLockRepository {

    /** Stream with the ids of every document currently protected with a PIN. */
    val lockedDocumentIds: Flow<Set<String>>

    /** True when [documentId] is protected with a PIN. */
    suspend fun isLocked(documentId: String): Boolean

    /** True when [pin] is the PIN protecting [documentId]. */
    suspend fun verifyPin(documentId: String, pin: String): Boolean

    /** Protects [documentId] with [pin]. */
    suspend fun lockDocument(documentId: String, pin: String)

    /**
     * Removes the protection of [documentId] once [pin] has been checked.
     *
     * @return true when the document was locked and the PIN matched.
     */
    suspend fun unlockDocument(documentId: String, pin: String): Boolean

    /** Removes the protection of [documentId] without asking for the PIN. */
    suspend fun removeLock(documentId: String)

    /** Salted hash + document id of every lock. Used by the local backup exporter. */
    suspend fun getAllLocks(): List<Pair<String, String>>

    /** Restores a batch of locks coming from a local backup file. */
    suspend fun restoreLocks(locks: List<Pair<String, String>>)
}
