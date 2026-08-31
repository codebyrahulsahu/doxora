package es.pile.core.data.local.backup

import kotlinx.serialization.Serializable

/** Current version of the on device backup format. */
const val BACKUP_VERSION = 1

/** Name of the JSON manifest stored inside the backup file. */
const val BACKUP_MANIFEST = "backup.json"

/** Folder inside the backup file where every document file is stored. */
const val BACKUP_FILES_FOLDER = "files"

@Serializable
data class PileBackup(
    val id: String,
    val name: String,
    val iconId: String,
    val colorNumber: Long?,
    val position: Int = 0
)

@Serializable
data class DocumentBackup(
    val id: String,
    val title: String,
    val imageIdsJson: String,
    val creationDateTime: String,
    val modificationDateTime: String,
    val documentStatus: Int,
    val documentPileIdsJson: String,
    val documentDetailsJson: String,
    val documentNote: String,
    val documentOrganizationIdsJson: String,
    val isIncomingPdf: Boolean
)

@Serializable
data class DocumentImageBackup(
    val id: String,
    val isDraft: Boolean,
    val cropJson: String?,
    val filter: Long,
    val rotation: Long
)

@Serializable
data class FavoriteBackup(
    val documentId: String,
    val createdAt: String
)

@Serializable
data class DocumentTextBackup(
    val documentId: String,
    val text: String,
    val updatedAt: String
)

@Serializable
data class DocumentLockBackup(
    val documentId: String,
    val pinHash: String,
    val createdAt: String
)

@Serializable
data class TrashEntryBackup(
    val documentId: String,
    val trashedAt: String,
    val originalStatus: Int
)

@Serializable
data class DocumentFileBackup(
    val documentId: String,
    val relativePath: String,
    val entryName: String
)

/**
 * Everything the app knows about, serialized so it can be written to a single file
 * that never leaves the device.
 */
@Serializable
data class BackupPayload(
    val version: Int = BACKUP_VERSION,
    val createdAt: String = "",
    val settingsJson: String = "",
    val piles: List<PileBackup> = emptyList(),
    val documents: List<DocumentBackup> = emptyList(),
    val documentImages: List<DocumentImageBackup> = emptyList(),
    val favorites: List<FavoriteBackup> = emptyList(),
    val documentTexts: List<DocumentTextBackup> = emptyList(),
    val documentLocks: List<DocumentLockBackup> = emptyList(),
    val trashEntries: List<TrashEntryBackup> = emptyList(),
    val files: List<DocumentFileBackup> = emptyList()
)
