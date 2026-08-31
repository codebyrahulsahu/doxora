package es.pile.features.home.data.schedulers

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import es.pile.core.domain.models.TRASH_RETENTION_DAYS
import es.pile.features.home.data.workers.CleanupWorker
import es.pile.features.home.domain.schedulers.CleanupScheduler
import es.pile.features.recycleBin.data.workers.TrashCleanupWorker
import java.util.concurrent.TimeUnit

/**
 * Android-specific implementation of [CleanupScheduler] using the WorkManager API.
 *
 * @property workManager The instance of [androidx.work.WorkManager] used for scheduling tasks.
 */
class WorkManagerCleanupScheduler(
    private val workManager: WorkManager
) : CleanupScheduler {

    override fun scheduleDocumentDeletion(documentId: String) {
        val data = CleanupWorker.buildInputData(documentId)

        val erasureRequest = OneTimeWorkRequestBuilder<CleanupWorker>()
            .setInputData(data)
            .build()

        workManager.enqueueUniqueWork(
            "cleanup_${documentId}",
            ExistingWorkPolicy.KEEP,
            erasureRequest
        )
    }

    override fun scheduleDocumentTrashPurge(documentId: String) {
        val purgeRequest = OneTimeWorkRequestBuilder<TrashCleanupWorker>()
            .setInputData(TrashCleanupWorker.buildInputData(documentId))
            .setInitialDelay(TRASH_RETENTION_DAYS.toLong(), TimeUnit.DAYS)
            .build()

        workManager.enqueueUniqueWork(
            "trash_purge_$documentId",
            ExistingWorkPolicy.REPLACE,
            purgeRequest
        )
    }

    override fun cancelDocumentTrashPurge(documentId: String) {
        workManager.cancelUniqueWork("trash_purge_$documentId")
    }

    override fun scheduleTrashStartupCleanup() {
        val cleanupRequest = OneTimeWorkRequestBuilder<TrashCleanupWorker>().build()

        workManager.enqueueUniqueWork(
            "trash_startup_cleanup",
            ExistingWorkPolicy.KEEP,
            cleanupRequest
        )
    }
}
