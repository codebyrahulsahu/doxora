package es.pile.core.domain.repositories

import es.pile.TrashEntry
import kotlinx.coroutines.flow.Flow

/**
 * Stores the metadata of every document that is currently in the Recycle Bin.
 *
 * The document itself (metadata, images, text, favorites and PIN) is kept until
 * the retention period expires, so it can be restored without losing anything.
 */
interface TrashRepository {

    /** Reactive stream of every trash entry, newest first. */
    val trashEntries: Flow<List<TrashEntry>>

    /** One shot read of every trash entry, newest first. */
    suspend fun getAllTrashEntries(): List<TrashEntry>

    /** Returns the entry for [documentId] or null when it is not in the bin. */
    suspend fun getTrashEntry(documentId: String): TrashEntry?

    /** Adds (or replaces) the trash entry for [documentId]. */
    suspend fun addToTrash(entry: TrashEntry)

    /** Removes the trash entry for [documentId]. */
    suspend fun removeFromTrash(documentId: String)
}
