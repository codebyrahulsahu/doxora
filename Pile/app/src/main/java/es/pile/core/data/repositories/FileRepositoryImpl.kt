package es.pile.core.data.repositories

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import es.pile.DocumentImage
import es.pile.DocumentModel
import es.pile.core.data.util.ImageTransformationHelper
import es.pile.core.data.util.PdfRenderHelper
import es.pile.core.domain.models.ImageFilterType
import es.pile.core.domain.repositories.FileRepository
import es.pile.core.domain.repositories.FileRepository.StorageType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.util.UUID


class FileRepositoryImpl(
    private val appContext: Context,
    private val appDirectory: File = appContext.filesDir,
    private val cacheDirectory: File = appContext.cacheDir,
    private val contentResolver: ContentResolver = appContext.contentResolver,
    private val ioDispatcher: CoroutineDispatcher,
    private val pdfRenderHelper: PdfRenderHelper,
    private val imageTransformationHelper: ImageTransformationHelper
) : FileRepository {
    private fun getPDFFileName(documentId: String): String {
        val cleanId = documentId.removeSuffix(".pdf")
        return "$cleanId.pdf"
    }

    private fun getImageFileName(imageId: String): String {
        val cleanId = imageId.removePrefix("img_").removeSuffix(".jpg")
        return "img_$cleanId.jpg"
    }

    private fun getStorage(storageType: StorageType): File = when (storageType) {
        StorageType.PERSISTENT -> appDirectory
        StorageType.CACHE -> cacheDirectory
    }

    override fun getDocumentDirectory(
        storageType: StorageType,
        documentId: String
    ): File = File(getStorage(storageType), documentId)

    override fun getPDFFile(storageType: StorageType, documentId: String): File =
        File(getDocumentDirectory(storageType, documentId), getPDFFileName(documentId))

    override fun getImageFile(
        storageType: StorageType,
        documentId: String,
        imageId: String
    ): File = File(getDocumentDirectory(storageType, documentId), getImageFileName(imageId))

    override suspend fun deleteDocumentStorage(
        storageType: StorageType,
        documentId: String
    ): Boolean = withContext(ioDispatcher) {
        getDocumentDirectory(storageType, documentId).deleteRecursively()
    }

    override suspend fun deleteDocumentImage(
        storageType: StorageType,
        documentId: String,
        imageId: String
    ): Boolean = withContext(ioDispatcher) {
        getImageFile(storageType, documentId, imageId).delete()
    }

    override suspend fun createTempImageUri(): Uri = withContext(ioDispatcher) {
        val imageDir = File(appContext.cacheDir, "images")
        if (!imageDir.exists()) imageDir.mkdirs()

        val imageFile = File.createTempFile("IMG_", ".jpg", imageDir)

        FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.provider",
            imageFile
        )
    }

    override suspend fun getUriForFile(file: File): Uri = withContext(ioDispatcher) {
        val authority = "${appContext.packageName}.provider"

        FileProvider.getUriForFile(
            appContext,
            authority,
            file
        )
    }

    override suspend fun isPdfOutdated(document: DocumentModel): Boolean =
        withContext(ioDispatcher) {
            if (document.isIncomingPdf) return@withContext false

            val pdfFile = getPDFFile(StorageType.PERSISTENT, document.id)
            if (!pdfFile.exists()) return@withContext true

            val pdfFileLastModification = Instant.ofEpochMilli(pdfFile.lastModified())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()

            val documentLastModification = document.modificationDateTime

            return@withContext pdfFileLastModification.isBefore(documentLastModification)
        }

    override suspend fun saveResizeRotateImagesToStorage(
        storageType: StorageType,
        uris: List<Uri>,
        documentId: String,
        maxSize: Int,
        quality: Int
    ): List<File> = withContext(ioDispatcher) {
        val storageDir = File(getStorage(storageType), documentId).apply { if (!exists()) mkdirs() }

        uris.map { uri ->
            async {
                saveResizeRotateImageToStorage(
                    uri = uri,
                    storageDir = storageDir,
                    maxSize = maxSize,
                    quality = quality
                )
            }
        }.awaitAll().filterNotNull()
    }

    override suspend fun saveImageToStorage(
        storageType: StorageType,
        uris: List<Uri>,
        documentId: String
    ): List<File> = withContext(ioDispatcher) {
        val storageDir = File(getStorage(storageType), documentId).apply { if (!exists()) mkdirs() }

        uris.map { uri ->
            async {
                val fileName = getImageFileName(UUID.randomUUID().toString())
                val destFile = File(storageDir, fileName)

                copyContentUriToFile(uri, destFile)

                destFile
            }
        }.awaitAll()
    }

    override suspend fun saveImagesToTargetSize(
        storageType: StorageType,
        uris: List<Uri>,
        documentId: String,
        targetSizeKb: Int
    ): List<File> = withContext(ioDispatcher) {
        val storageDir = File(getStorage(storageType), documentId).apply { if (!exists()) mkdirs() }

        // Never try to compress below a sane minimum to avoid useless degradation.
        val targetBytes = (targetSizeKb.toLong() * 1024L).coerceAtLeast(MIN_COMPRESSED_IMAGE_BYTES)

        uris.map { uri ->
            async {
                saveImageToTargetSize(uri, storageDir, targetBytes)
            }
        }.awaitAll().filterNotNull()
    }

    override suspend fun copyImageToInternalStorage(
        documentId: String,
        documentImage: DocumentImage
    ) = withContext(ioDispatcher) {
        val imageFile = getImageFile(StorageType.CACHE, documentId, documentImage.id)
        imageFile.copyTo(
            File(
                getDocumentDirectory(StorageType.PERSISTENT, documentId),
                imageFile.name
            )
        )
    }

    override suspend fun getFileNameFromUri(uri: Uri): String? {
        var fileName: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) fileName = cursor.getString(nameIndex)
            }
        }
        fileName = fileName?.substringBeforeLast('.')
        return fileName
    }

    override suspend fun getPageCount(documentId: String): Result<Int> =
        pdfRenderHelper.getPageCount(getPDFFile(StorageType.PERSISTENT, documentId))

    override suspend fun createPdfFromImages(
        documentId: String,
        images: List<DocumentImage>
    ): File = withContext(ioDispatcher) {
        val pdfDocument = PdfDocument()
        val generatedPdfFile = getPDFFile(StorageType.PERSISTENT, documentId)

        try {
            images.forEachIndexed { index, documentImage ->
                val imageFile = getImageFile(StorageType.PERSISTENT, documentId, documentImage.id)

                if (!imageFile.exists()) return@forEachIndexed

                val bitmap = imageTransformationHelper.transform(
                    file = imageFile,
                    rotation = documentImage.rotation.toInt(),
                    cropData = documentImage.crop,
                    filter = ImageFilterType.fromId(documentImage.filter.toInt()),
                    reqSize = 0 // When exporting full resolution
                )?.bitmap ?: return@forEachIndexed

                val pageInfo =
                    PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()

                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
                bitmap.recycle()
            }

            FileOutputStream(generatedPdfFile).use { pdfDocument.writeTo(it) }
        } catch (e: Exception) {
            if (generatedPdfFile.exists()) generatedPdfFile.delete()
            throw e
        } finally {
            pdfDocument.close()
        }
        return@withContext generatedPdfFile
    }

    override suspend fun copyPdfToInternalStorage(uri: Uri, documentId: String): File =
        withContext(ioDispatcher) {
            val documentFolder = File(appDirectory, documentId)
            documentFolder.mkdir()

            val destinationFile = File(documentFolder, getPDFFileName(documentId))

            copyContentUriToFile(uri, destinationFile)

            destinationFile
        }

    override suspend fun createTempPdfCopyWithName(sourceFile: File, displayName: String): File =
        withContext(ioDispatcher) {
            val safeName = sanitizeFileName(displayName, ".pdf")

            val exportDir = File(appContext.cacheDir, "export_pdfs").apply { mkdirs() }

            exportDir.listFiles()?.forEach { it.delete() }

            val destinationFile = File(exportDir, safeName)

            sourceFile.copyTo(destinationFile, overwrite = true)

            return@withContext destinationFile
        }

    override suspend fun exportFileToDownloads(
        file: File,
        publicName: String,
        mimeType: String,
        extension: String
    ): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val safeName = sanitizeFileName(publicName, extension)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: throw IOException("Failed to create new MediaStore record.")

                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    file.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: throw IOException("The output stream for URI $uri could not be opened")
                uri.toString()
            } else {
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()

                val destinationFile = File(downloadsDir, safeName)

                file.copyTo(destinationFile, overwrite = true)

                // Notify MediaScanner so the file shows up in Downloads/File Manager immediately
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.DATA, destinationFile.absolutePath)
                }
                contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

                Uri.fromFile(destinationFile).toString()
            }
        }
    }

    override suspend fun exportFileToFolder(
        file: File,
        folderUri: Uri,
        publicName: String,
        mimeType: String,
        extension: String
    ): Result<Uri> = withContext(ioDispatcher) {
        runCatching {
            val safeName = sanitizeFileName(publicName, extension)
            val displayName = safeName.removeSuffix(extension)

            // A tree URI (the one returned by the folder picker) has to be
            // converted into its document URI before creating children in it.
            val parentDocumentUri = if (DocumentsContract.isTreeUri(folderUri)) {
                DocumentsContract.buildDocumentUriUsingTree(
                    folderUri,
                    DocumentsContract.getTreeDocumentId(folderUri)
                )
            } else {
                folderUri
            }

            val documentUri = DocumentsContract.createDocument(
                contentResolver,
                parentDocumentUri,
                mimeType,
                displayName
            ) ?: throw IOException("The file could not be created in the selected folder")

            contentResolver.openOutputStream(documentUri)?.use { outputStream ->
                file.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw IOException("The output stream for URI $documentUri could not be opened")

            documentUri
        }
    }

    override suspend fun createDocumentImages(
        document: DocumentModel,
        documentImages: List<DocumentImage>,
        png: Boolean
    ): List<File> = withContext(ioDispatcher) {
        val exportDir = File(appContext.cacheDir, "export_images/${document.id}")
            .apply {
                deleteRecursively()
                mkdirs()
            }

        val compressFormat = if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        val extension = if (png) ".png" else ".jpg"

        if (document.isIncomingPdf) {
            val pdfFile = getPDFFile(StorageType.PERSISTENT, document.id)
            val pageCount = pdfRenderHelper.getPageCount(pdfFile).getOrDefault(0)

            (0 until pageCount).mapNotNull { index ->
                val bitmap = pdfRenderHelper.renderPageToBitmap(
                    file = pdfFile,
                    pageIndex = index,
                    width = PDF_EXPORT_MAX_WIDTH
                ) ?: return@mapNotNull null

                val file = File(exportDir, "page_${index + 1}$extension")
                FileOutputStream(file).use { output ->
                    bitmap.compress(compressFormat, IMAGE_EXPORT_QUALITY, output)
                }
                bitmap.recycle()

                file
            }
        } else {
            documentImages.mapIndexedNotNull { index, documentImage ->
                val sourceFile = getImageFile(
                    storageType = StorageType.PERSISTENT,
                    documentId = document.id,
                    imageId = documentImage.id
                )

                if (!sourceFile.exists()) return@mapIndexedNotNull null

                val bitmap = imageTransformationHelper.transform(
                    file = sourceFile,
                    rotation = documentImage.rotation.toInt(),
                    cropData = documentImage.crop,
                    filter = ImageFilterType.fromId(documentImage.filter.toInt()),
                    reqSize = 0 // Full resolution on export
                )?.bitmap ?: return@mapIndexedNotNull null

                val file = File(exportDir, "page_${index + 1}$extension")
                FileOutputStream(file).use { output ->
                    bitmap.compress(compressFormat, IMAGE_EXPORT_QUALITY, output)
                }
                bitmap.recycle()

                file
            }
        }
    }

    override suspend fun getExifRotation(file: File): Int =
        imageTransformationHelper.getExifRotation(file)

    override suspend fun getExifRotation(uri: Uri): Int =
        imageTransformationHelper.getExifRotation(uri)

    override suspend fun saveProfilePicture(
        uri: Uri,
        previousFileName: String?
    ): String = withContext(ioDispatcher) {
        val folder = File(appDirectory, PROFILE_PICTURE_FOLDER).apply { mkdirs() }

        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("The selected image could not be read")

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("The selected file is not a valid image")
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = imageTransformationHelper.calculateInSampleSize(
                bounds,
                PROFILE_PICTURE_MAX_SIZE
            )
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: throw IOException("The selected image could not be decoded")

        val rotation = imageTransformationHelper.getExifRotation(uri)

        val orientedBitmap = if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }

        val destination = File(folder, "profile_${System.currentTimeMillis()}.jpg")
        destination.outputStream().use { output ->
            orientedBitmap.compress(Bitmap.CompressFormat.JPEG, PROFILE_PICTURE_QUALITY, output)
        }

        // Remove the replaced picture so internal storage does not fill up with orphans
        if (previousFileName != null && previousFileName != destination.name) {
            File(folder, previousFileName).delete()
        }

        destination.name
    }

    override fun getProfilePictureFile(fileName: String): File =
        File(File(appDirectory, PROFILE_PICTURE_FOLDER), fileName)


    /**
     * Helper function to save an image from a URI with resizing and rotating based on EXIF data.
     *
     * @param uri URI of the image to be saved.
     * @param storageDir Directory where the image will be saved.
     * @param maxSize Maximum size of the image in pixels (default: 1200).
     * @param quality Quality of the saved image (default: 85).
     * @return File object representing the saved image
     */
    private suspend fun saveResizeRotateImageToStorage(
        uri: Uri,
        storageDir: File,
        maxSize: Int = 1200,
        quality: Int = 85
    ): File? = withContext(ioDispatcher) {
        try {
            val rotation = getExifRotation(uri)

            val fileName = getImageFileName(UUID.randomUUID().toString())
            val destFile = File(storageDir, fileName)

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            options.inSampleSize = imageTransformationHelper.calculateInSampleSize(options, maxSize)
            options.inJustDecodeBounds = false

            val bitmap = contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            bitmap?.let { original ->
                var finalBitmap = scaleBitmap(original, maxSize)

                if (rotation != 0) finalBitmap = rotateBitmap(finalBitmap, rotation)

                FileOutputStream(destFile).use { out ->
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                }
                if (finalBitmap != original) finalBitmap.recycle()
                original.recycle()
            }

            destFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    /**
     * Helper function to save an image from a URI compressed to a custom target file size.
     *
     * Keeps the original dimensions and the highest possible quality: the JPEG quality
     * is reduced only as much as needed to fit [targetBytes], and images that already
     * fit the target are stored unchanged. Very heavy images are progressively
     * downscaled as a last resort.
     *
     * @param uri URI of the image to be saved.
     * @param storageDir Directory where the image will be saved.
     * @param targetBytes Maximum size in bytes of the saved image.
     * @return File object representing the saved image, or null if it could not be processed.
     */
    private suspend fun saveImageToTargetSize(
        uri: Uri,
        storageDir: File,
        targetBytes: Long
    ): File? = withContext(ioDispatcher) {
        try {
            val fileName = getImageFileName(UUID.randomUUID().toString())
            val destFile = File(storageDir, fileName)

            // Already fits the target: keep the original bytes untouched.
            val originalSize = getContentUriSize(uri)
            if (originalSize != null && originalSize <= targetBytes) {
                copyContentUriToFile(uri, destFile)
                return@withContext destFile
            }

            val rotation = getExifRotation(uri)

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            val bitmap = contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return@withContext null

            var finalBitmap = if (rotation != 0) rotateBitmap(bitmap, rotation) else bitmap

            val compressed = compressBitmapToTargetSize(finalBitmap, targetBytes)

            FileOutputStream(destFile).use { out ->
                out.write(compressed)
            }

            if (finalBitmap != bitmap) finalBitmap.recycle()
            bitmap.recycle()

            destFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Compresses a [Bitmap] to a JPEG [ByteArray] that fits [targetBytes].
     *
     * A binary search over the JPEG quality (1-100) finds the highest quality whose
     * encoded size is still below the target, so quality is preserved as much as
     * possible. If even the lowest quality is too big, the bitmap is progressively
     * downscaled (keeping the aspect ratio) until it fits.
     *
     * @param source The bitmap to compress (not recycled by this function).
     * @param targetBytes Maximum size in bytes of the result.
     * @return The compressed JPEG bytes.
     */
    private fun compressBitmapToTargetSize(source: Bitmap, targetBytes: Long): ByteArray {
        var working = source
        var result = compressToBytes(working, findMaxQuality(working, targetBytes))

        var scaleFactor = 0.85f
        var attempts = 0
        while (result.size.toLong() > targetBytes && attempts < MAX_DOWNSCALE_ATTEMPTS) {
            val newWidth = (working.width * scaleFactor).toInt().coerceAtLeast(MIN_IMAGE_SIDE_PIXELS)
            val newHeight = (working.height * scaleFactor).toInt().coerceAtLeast(MIN_IMAGE_SIDE_PIXELS)

            if (newWidth >= working.width || newHeight >= working.height) break

            val scaled = Bitmap.createScaledBitmap(working, newWidth, newHeight, true)
            if (scaled != working) working.recycle()
            working = scaled

            result = compressToBytes(working, findMaxQuality(working, targetBytes))
            attempts++
        }

        if (working != source) working.recycle()
        return result
    }

    /**
     * Finds the highest JPEG quality (1-100) whose encoded size fits [targetBytes].
     *
     * @param bitmap The bitmap to encode.
     * @param targetBytes Maximum size in bytes.
     * @return The best quality value (at least 1).
     */
    private fun findMaxQuality(bitmap: Bitmap, targetBytes: Long): Int {
        var low = MIN_JPEG_QUALITY
        var high = MAX_JPEG_QUALITY
        var best = MIN_JPEG_QUALITY

        while (low <= high) {
            val mid = (low + high) / 2
            val size = compressToBytes(bitmap, mid).size.toLong()

            if (size <= targetBytes) {
                best = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return best
    }

    /**
     * Encodes a [Bitmap] as a JPEG [ByteArray] with the given quality.
     */
    private fun compressToBytes(bitmap: Bitmap, quality: Int): ByteArray {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        return output.toByteArray()
    }

    /**
     * Returns the size in bytes of the file behind a [Uri], or null when unknown.
     *
     * Providers that do not support queries (for example the FileProvider used for
     * scanner/camera captures) simply report an unknown size.
     */
    private fun getContentUriSize(uri: Uri): Long? {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        cursor.getLong(sizeIndex)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }


    /**
     * Scales a [Bitmap] to a specified maximum size.
     *
     * @param source The [Bitmap] to be scaled.
     * @param maxSize The maximum size of the bitmap in pixels.
     * @return The scaled [Bitmap].
     */
    private fun scaleBitmap(source: Bitmap, maxSize: Int): Bitmap {
        val width = source.width
        val height = source.height

        if (width <= maxSize && height <= maxSize) return source

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int

        if (width > height) {
            targetWidth = maxSize
            targetHeight = (maxSize / ratio).toInt()
        } else {
            targetHeight = maxSize
            targetWidth = (maxSize * ratio).toInt()
        }

        return source.scale(targetWidth, targetHeight)
    }

    /**
     * Rotates a [Bitmap] by a specified number of degrees.
     *
     * @param source The [Bitmap] to be rotated.
     * @param degrees The number of degrees to rotate the [Bitmap].
     * @return The rotated [Bitmap].
     */
    private fun rotateBitmap(source: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return source
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        source.recycle()
        return rotated
    }

    /**
     * Copies the content of a URI to a destination file.
     *
     * @param sourceUri The URI to copy from.
     * @param destinationFile The file to copy to.
     * @throws IllegalStateException if the input stream could not be opened.
     */
    private fun copyContentUriToFile(sourceUri: Uri, destinationFile: File) {
        val inputStream = contentResolver.openInputStream(sourceUri)
            ?: throw IllegalStateException("The stream for URI $sourceUri could not be opened")

        inputStream.use { input ->
            destinationFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    /**
     * Sanitizes a file name by removing invalid characters and ensuring the
     * requested [extension] is present.
     */
    private fun sanitizeFileName(name: String, extension: String): String {
        val cleanName = name.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
        return if (cleanName.endsWith(extension, ignoreCase = true)) {
            cleanName
        } else {
            "$cleanName$extension"
        }
    }

    private companion object {
        /** Max width used when rendering PDF pages for the image export. */
        const val PDF_EXPORT_MAX_WIDTH = 2480

        /** Quality used when exporting JPG images. */
        const val IMAGE_EXPORT_QUALITY = 90

        /** Folder inside internal storage holding the profile pictures. */
        const val PROFILE_PICTURE_FOLDER = "profile_pictures"

        /** Max side in pixels of a stored profile picture. */
        const val PROFILE_PICTURE_MAX_SIZE = 512

        /** JPEG quality of a stored profile picture. */
        const val PROFILE_PICTURE_QUALITY = 90

        /** Smallest target size (in bytes) accepted by the Document Resizer. */
        const val MIN_COMPRESSED_IMAGE_BYTES = 16L * 1024L

        /** Lowest JPEG quality used by the Document Resizer. */
        const val MIN_JPEG_QUALITY = 1

        /** Highest JPEG quality used by the Document Resizer. */
        const val MAX_JPEG_QUALITY = 100

        /** How many times the Document Resizer may downscale a very heavy image. */
        const val MAX_DOWNSCALE_ATTEMPTS = 12

        /** Smallest image side (in pixels) kept while downscaling a very heavy image. */
        const val MIN_IMAGE_SIDE_PIXELS = 64
    }
}
