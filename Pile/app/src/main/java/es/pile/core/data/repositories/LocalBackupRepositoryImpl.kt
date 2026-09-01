package es.pile.core.data.repositories

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import es.pile.DatabaseQueries
import es.pile.core.data.local.backup.BACKUP_FILES_FOLDER
import es.pile.core.data.local.backup.BACKUP_MANIFEST
import es.pile.core.data.local.backup.BACKUP_VERSION
import es.pile.core.data.local.backup.BackupPayload
import es.pile.core.data.local.backup.DocumentFileBackup
import es.pile.core.data.local.backup.DocumentImageBackup
import es.pile.core.data.local.backup.TrashEntryBackup
import es.pile.core.domain.models.DocumentDetail
import es.pile.core.domain.models.ImageCropData
import es.pile.core.domain.models.UserSettings
import es.pile.core.domain.repositories.BackupSummary
import es.pile.core.domain.repositories.FileRepository
import es.pile.core.domain.repositories.LocalBackupRepository
import es.pile.core.domain.repositories.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class LocalBackupRepositoryImpl(
    private val appContext: Context,
    private val ioDispatcher: CoroutineDispatcher,
    private val databaseQueries: DatabaseQueries,
    private val fileRepository: FileRepository,
    private val settingsRepository: SettingsRepository,
    private val contentResolver: ContentResolver = appContext.contentResolver
) : LocalBackupRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    override suspend fun createBackup(uri: Uri): Result<BackupSummary> = withContext(ioDispatcher) {
        runCatching {
            val settings = settingsRepository.userSettings.first()

            val piles = databaseQueries.selectAllPileModels().executeAsList()
            val documents = databaseQueries.selectAllDocumentModels().executeAsList()
            val images = databaseQueries.selectAllDocumentImages().executeAsList()
            val favorites = databaseQueries.selectAllFavoriteDocuments().executeAsList()
            val texts = databaseQueries.selectAllDocumentTexts().executeAsList()
            val locks = databaseQueries.selectAllDocumentLocks().executeAsList()
            val trashEntries = databaseQueries.selectAllTrashEntries().executeAsList()

            val files = mutableListOf<DocumentFileBackup>()

            documents.forEach { document ->
                val directory = fileRepository.getDocumentDirectory(
                    storageType = FileRepository.StorageType.PERSISTENT,
                    documentId = document.id
                )

                if (!directory.exists()) return@forEach

                directory.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        val relativePath = file.relativeTo(directory).path

                        files += DocumentFileBackup(
                            documentId = document.id,
                            relativePath = relativePath,
                            entryName = "$BACKUP_FILES_FOLDER/${document.id}/$relativePath"
                        )
                    }
            }

            val payload = BackupPayload(
                version = BACKUP_VERSION,
                createdAt = Instant.now().toString(),
                settingsJson = json.encodeToString(settings),
                piles = piles.map {
                    es.pile.core.data.local.backup.PileBackup(
                        id = it.id,
                        name = it.name,
                        iconId = it.iconId,
                        colorNumber = it.colorNumber,
                        position = it.position.toInt()
                    )
                },
                documents = documents.map {
                    es.pile.core.data.local.backup.DocumentBackup(
                        id = it.id,
                        title = it.title,
                        imageIdsJson = json.encodeToString(it.imageIds),
                        creationDateTime = it.creationDateTime.toString(),
                        modificationDateTime = it.modificationDateTime.toString(),
                        documentStatus = it.documentStatus,
                        documentPileIdsJson = json.encodeToString(it.documentPileIds),
                        documentDetailsJson = json.encodeToString(it.documentDetails),
                        documentNote = it.documentNote,
                        documentOrganizationIdsJson = json.encodeToString(it.documentOrganizationIds),
                        isIncomingPdf = it.isIncomingPdf
                    )
                },
                documentImages = images.map {
                    DocumentImageBackup(
                        id = it.id,
                        isDraft = it.isDraft,
                        cropJson = it.crop?.let { crop -> json.encodeToString(crop) },
                        filter = it.filter,
                        rotation = it.rotation
                    )
                },
                favorites = favorites.map {
                    es.pile.core.data.local.backup.FavoriteBackup(
                        documentId = it.documentId,
                        createdAt = it.createdAt
                    )
                },
                documentTexts = texts.map {
                    es.pile.core.data.local.backup.DocumentTextBackup(
                        documentId = it.documentId,
                        text = it.text,
                        updatedAt = it.updatedAt
                    )
                },
                documentLocks = locks.map {
                    es.pile.core.data.local.backup.DocumentLockBackup(
                        documentId = it.documentId,
                        pinHash = it.pinHash,
                        createdAt = it.createdAt
                    )
                },
                trashEntries = trashEntries.map {
                    TrashEntryBackup(
                        documentId = it.documentId,
                        trashedAt = it.trashedAt.toString(),
                        originalStatus = it.originalStatus
                    )
                },
                files = files
            )

            val tempFile = File(
                backupCacheDirectory(),
                "doxora document backup-${System.currentTimeMillis()}.zip"
            )

            try {
                ZipOutputStream(BufferedOutputStream(FileOutputStream(tempFile))).use { zip ->
                    zip.putNextEntry(ZipEntry(BACKUP_MANIFEST))
                    zip.write(json.encodeToString(payload).toByteArray(Charsets.UTF_8))
                    zip.closeEntry()

                    files.forEach { entry ->
                        val file = File(
                            fileRepository.getDocumentDirectory(
                                storageType = FileRepository.StorageType.PERSISTENT,
                                documentId = entry.documentId
                            ),
                            entry.relativePath
                        )

                        if (!file.exists()) return@forEach

                        zip.putNextEntry(ZipEntry(entry.entryName))
                        file.inputStream().use { input -> input.copyTo(zip) }
                        zip.closeEntry()
                    }

                    // The profile picture travels inside the backup too; its file
                    // name is already stored in the settings JSON of the manifest
                    settings.profilePicturePath?.let { path ->
                        val pictureFile = fileRepository.getProfilePictureFile(path)

                        if (pictureFile.exists()) {
                            zip.putNextEntry(
                                ZipEntry("$BACKUP_FILES_FOLDER/profile_pictures/${pictureFile.name}")
                            )
                            pictureFile.inputStream().use { input -> input.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }

                    // Every hub picture travels with the backup as well
                    settings.hubPicturePaths.values.distinct().forEach { path ->
                        val hubPictureFile = fileRepository.getProfilePictureFile(path)

                        if (hubPictureFile.exists()) {
                            zip.putNextEntry(
                                ZipEntry(
                                    "$BACKUP_FILES_FOLDER/profile_pictures/${hubPictureFile.name}"
                                )
                            )
                            hubPictureFile.inputStream().use { input -> input.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }

                val outputStream = contentResolver.openOutputStream(uri)
                    ?: throw IOException("The destination file could not be opened")

                outputStream.use { output ->
                    tempFile.inputStream().use { input -> input.copyTo(output) }
                }
            } finally {
                tempFile.delete()
            }

            BackupSummary(
                piles = piles.size,
                documents = documents.size,
                images = images.size,
                favorites = favorites.size,
                recognizedTexts = texts.size,
                locks = locks.size,
                files = files.size
            )
        }
    }

    override suspend fun restoreBackup(uri: Uri): Result<BackupSummary> = withContext(ioDispatcher) {
        runCatching {
            val tempFile = File(backupCacheDirectory(), "pile-restore-${System.currentTimeMillis()}.zip")

            try {
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw IOException("The backup file could not be read")

                inputStream.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }

                var payload: BackupPayload? = null

                ZipInputStream(BufferedInputStream(FileInputStream(tempFile))).use { zip ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

                    var entry = zip.nextEntry
                    while (entry != null) {
                        val name = entry.name

                        when {
                            name == BACKUP_MANIFEST -> {
                                payload = json.decodeFromString<BackupPayload>(
                                    zip.readBytes().decodeToString()
                                )
                            }

                            !entry.isDirectory && name.startsWith("$BACKUP_FILES_FOLDER/") -> {
                                writeBackupFile(name.removePrefix("$BACKUP_FILES_FOLDER/"), zip, buffer)
                            }
                        }

                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }

                val data = payload ?: throw IOException("That file is not a Pile backup")

                if (data.version > BACKUP_VERSION) {
                    throw IOException("That backup was created with a newer version of Pile")
                }

                if (data.settingsJson.isNotBlank()) {
                    val settings = json.decodeFromString<UserSettings>(data.settingsJson)
                    settingsRepository.updateUserSettings(settings)
                }

                data.piles.forEach { pile ->
                    databaseQueries.restorePileModel(
                        pile.id,
                        pile.name,
                        pile.iconId,
                        pile.colorNumber,
                        pile.position.toLong()
                    )
                }

                data.documents.forEach { document ->
                    databaseQueries.restoreDocumentModel(
                        document.id,
                        document.title,
                        json.decodeFromString<List<String>>(document.imageIdsJson),
                        LocalDateTime.parse(document.creationDateTime),
                        LocalDateTime.parse(document.modificationDateTime),
                        document.documentStatus,
                        json.decodeFromString<List<String>>(document.documentPileIdsJson),
                        json.decodeFromString<List<DocumentDetail>>(document.documentDetailsJson),
                        document.documentNote,
                        json.decodeFromString<List<String>>(document.documentOrganizationIdsJson),
                        document.isIncomingPdf
                    )
                }

                data.documentImages.forEach { image ->
                    databaseQueries.restoreDocumentImage(
                        image.id,
                        image.isDraft,
                        image.cropJson?.let { json.decodeFromString<ImageCropData>(it) },
                        image.filter,
                        image.rotation
                    )
                }

                data.favorites.forEach { favorite ->
                    databaseQueries.restoreFavoriteDocument(favorite.documentId, favorite.createdAt)
                }

                data.documentTexts.forEach { text ->
                    databaseQueries.restoreDocumentText(
                        text.documentId,
                        text.text,
                        text.updatedAt
                    )
                }

                data.documentLocks.forEach { lock ->
                    databaseQueries.restoreDocumentLock(
                        lock.documentId,
                        lock.pinHash,
                        lock.createdAt
                    )
                }

                data.trashEntries.forEach { entry ->
                    databaseQueries.restoreTrashEntry(
                        entry.documentId,
                        LocalDateTime.parse(entry.trashedAt),
                        entry.originalStatus
                    )
                }

                BackupSummary(
                    piles = data.piles.size,
                    documents = data.documents.size,
                    images = data.documentImages.size,
                    favorites = data.favorites.size,
                    recognizedTexts = data.documentTexts.size,
                    locks = data.documentLocks.size,
                    files = data.files.size
                )
            } finally {
                tempFile.delete()
            }
        }
    }

    private fun writeBackupFile(relativePath: String, zip: ZipInputStream, buffer: ByteArray) {
        val root = appContext.filesDir.canonicalFile
        val destination = File(root, relativePath).canonicalFile

        if (destination.path != root.path && !destination.path.startsWith(root.path + File.separator)) {
            throw IOException("The backup contains an invalid file path")
        }

        destination.parentFile?.mkdirs()

        destination.outputStream().use { output ->
            var read = zip.read(buffer)
            while (read >= 0) {
                output.write(buffer, 0, read)
                read = zip.read(buffer)
            }
        }
    }

    private fun backupCacheDirectory(): File =
        File(appContext.cacheDir, "backups").apply { mkdirs() }
}
