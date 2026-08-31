package es.pile.core.domain.repositories

import android.net.Uri

/** Numbers of items written to (or read from) a backup file. */
data class BackupSummary(
    val piles: Int = 0,
    val documents: Int = 0,
    val images: Int = 0,
    val favorites: Int = 0,
    val recognizedTexts: Int = 0,
    val locks: Int = 0,
    val files: Int = 0
)

/**
 * Exports and restores the whole app data using a single local file.
 *
 * There is no cloud involved: the file is written exactly where the user picks it
 * (device storage, an SD card, a USB drive...) and it is only readable by restoring
 * it from the same screen.
 */
interface LocalBackupRepository {

    /**
     * Writes a backup of every pile, document, image, favorite, recognized text,
     * document lock and user setting into [uri].
     */
    suspend fun createBackup(uri: Uri): Result<BackupSummary>

    /**
     * Reads a backup previously created with [createBackup] and merges it into the
     * current database, copying the stored files back into the app storage.
     */
    suspend fun restoreBackup(uri: Uri): Result<BackupSummary>
}
