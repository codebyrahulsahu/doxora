package es.pile.features.recycleBin.data.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import es.pile.features.recycleBin.domain.useCases.DeleteTrashedDocumentUseCase
import es.pile.features.recycleBin.domain.useCases.PurgeExpiredTrashEntriesUseCase

/**
 * Background worker responsible for permanently deleting Recycle Bin entries.
 *
 * With a [DOCUMENT_ID] in the input data it deletes exactly that document
 * (scheduled with a 30 days delay when the document is moved to the bin).
 * Without input data it deletes every entry whose retention period has
 * already expired (scheduled on every app start).
 */
class TrashCleanupWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val deleteTrashedDocumentUseCase: DeleteTrashedDocumentUseCase,
    private val purgeExpiredTrashEntriesUseCase: PurgeExpiredTrashEntriesUseCase
) : CoroutineWorker(context, workerParams) {

    companion object {
        /** Key for the document identifier in the [inputData] map. */
        const val DOCUMENT_ID = "TRASH_DOC_ID_KEY"

        /** Helper to create the [androidx.work.Data] required to start this worker. */
        fun buildInputData(documentId: String) = workDataOf(DOCUMENT_ID to documentId)
    }

    override suspend fun doWork(): Result {
        return try {
            val documentId = inputData.getString(DOCUMENT_ID)

            if (documentId.isNullOrBlank()) {
                purgeExpiredTrashEntriesUseCase()
            } else {
                deleteTrashedDocumentUseCase(documentId)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
