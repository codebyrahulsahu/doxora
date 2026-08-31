package es.pile.features.home.domain.schedulers

/**
 * Interface defining the contract for scheduling deferred document cleanup operations.
 */
interface CleanupScheduler {
    /**
     * Schedules a background task to permanently delete a document and its
     * associated resources after a predefined period.
     *
     * @param documentId The unique identifier of the document to be cleaned up.
     */
    fun scheduleDocumentDeletion(documentId: String)

    /**
     * Schedules the permanent deletion of a Recycle Bin entry after the 30 days
     * retention period. Replaces any previous schedule for the same document,
     * so moving an already trashed document to the bin again resets the timer.
     *
     * @param documentId The unique identifier of the trashed document.
     */
    fun scheduleDocumentTrashPurge(documentId: String)

    /**
     * Cancels the pending deletion of a Recycle Bin entry (used when the
     * document is restored).
     */
    fun cancelDocumentTrashPurge(documentId: String)

    /**
     * Enqueues a one-time cleanup that permanently deletes every Recycle Bin
     * entry that has already expired. Acts as a safety net for devices that
     * were off when the delayed job was due.
     */
    fun scheduleTrashStartupCleanup()
}
