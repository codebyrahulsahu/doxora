package es.pile.core.ui.controllers

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import io.github.aakira.napier.Napier

/** Mime types accepted when documents are imported from the device folders. */
private val DEVICE_FILES_MIME_TYPES = arrayOf("image/*")

/**
 * Data class holding the callbacks to trigger document import actions.
 * Passed down to UI components like FABs or Menus.
 *
 * @property launchPdfPicker Opens the system picker to import a PDF file.
 * @property launchGallery Opens the photo picker (gallery) to import images.
 * @property launchDeviceFiles Opens the file browser to import images stored in
 * the device folders (Downloads, WhatsApp, SD card…).
 * @property launchCamera Captures a new page with the camera document mode.
 */
data class ImportActions(
    val launchPdfPicker: () -> Unit,
    val launchGallery: () -> Unit,
    val launchDeviceFiles: () -> Unit,
    val launchCamera: () -> Unit
)

/**
 * A headless composable controller that manages ActivityResultLaunchers
 * for document imports.
 *
 * Images can be imported both from the gallery (photo picker) and from the
 * device folders (file browser); both paths report the picked images through
 * [onImagesSelected].
 *
 * "Take a photo" strictly opens the default camera app of the device in its
 * built in document scanning ("Document") mode when the device exposes one,
 * and otherwise the plain device camera, still requesting its document mode
 * through capture hints. The document scanner of Google Play services is
 * never triggered.
 *
 * @param cameraUri URI where the captured page is written.
 * @param onUriConsumed Callback when the URI is consumed.
 * @param onCameraClick Callback asking for a new capture URI ([cameraUri]).
 * @param onPdfSelected Callback when a PDF is selected.
 * @param onImagesSelected Callback when images (or captured pages) are selected.
 * @return An ImportActions object with callbacks to trigger document import actions.
 */
@Composable
fun rememberDocumentImportController(
    cameraUri: Uri? = null,
    onUriConsumed: () -> Unit = {},
    onCameraClick: () -> Unit = {},
    onPdfSelected: (Uri) -> Unit = {},
    onImagesSelected: (List<Uri>) -> Unit = {}
): ImportActions {
    val context = LocalContext.current
    val currentOnImagesSelected by rememberUpdatedState(onImagesSelected)
    val currentOnUriConsumed by rememberUpdatedState(onUriConsumed)

    // Plain capture, also asking the camera app for its document mode.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = TakeDocumentPicture()
    ) { success ->
        if (success && cameraUri != null) {
            currentOnImagesSelected(listOf(cameraUri))
        }
        currentOnUriConsumed()
    }

    // Capture done by the document mode of the native camera app.
    val documentCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val capturedUri = result.data?.data ?: cameraUri

        if (result.resultCode == Activity.RESULT_OK && capturedUri != null) {
            currentOnImagesSelected(listOf(capturedUri))
        }
        currentOnUriConsumed()
    }

    // REACCIÓN: Cuando el ViewModel genera el URI, lanzamos la cámara automáticamente
    LaunchedEffect(cameraUri) {
        cameraUri?.let { uri ->
            val documentModeIntent = NativeDocumentCamera.createDocumentModeIntent(context, uri)

            if (documentModeIntent != null) {
                runCatching { documentCameraLauncher.launch(documentModeIntent) }
                    .onFailure { error ->
                        Napier.e("Error launching the native camera document mode", error)
                        cameraLauncher.launch(uri)
                    }
            } else {
                cameraLauncher.launch(uri)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            currentOnImagesSelected(uris)
        }
    }

    // Images stored in the device folders, picked with the system file browser.
    val deviceFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            currentOnImagesSelected(uris)
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let(onPdfSelected)
    }

    return remember(context, onCameraClick) {
        ImportActions(
            launchPdfPicker = {
                pdfLauncher.launch(arrayOf("application/pdf"))
            },
            launchGallery = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            },
            launchDeviceFiles = {
                deviceFilesLauncher.launch(DEVICE_FILES_MIME_TYPES)
            },
            launchCamera = {
                // "Take a photo" strictly uses the document scanning mode of the
                // device's default camera: ask for a capture URI and open it
                // right away (never the Google Play services scanner).
                onCameraClick()
            }
        )
    }
}
