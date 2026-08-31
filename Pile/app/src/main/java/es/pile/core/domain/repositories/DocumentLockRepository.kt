package es.pile.core.domain.repositories

import es.pile.core.domain.models.DocumentLockType
import kotlinx.coroutines.flow.Flow

/**
 * Protects individual documents with a PIN or with a draw pattern.
 *
 * Only a salted hash of the secret is stored, the plain PIN/pattern is never
 * persisted. The kind of secret is encoded together with the hash so the UI
 * can ask for the right input when unlocking.
 */
interface DocumentLockRepository {

    /** Stream with the ids of every document currently protected. */
    val lockedDocumentIds: Flow<Set<String>>

    /** True when [documentId] is protected with a PIN or a pattern. */
    suspend fun isLocked(documentId: String): Boolean

    /** Kind of secret protecting [documentId] ([DocumentLockType.PIN] when it is not locked). */
    suspend fun getLockType(documentId: String): DocumentLockType

    /** True when [secret] is the PIN/pattern protecting [documentId]. */
    suspend fun verifySecret(documentId: String, secret: String): Boolean

    /** Protects [documentId] with [secret] (a PIN or a pattern depending on [type]). */
    suspend fun lockDocument(
        documentId: String,
        secret: String,
        type: DocumentLockType = DocumentLockType.PIN
    )

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
