package es.pile.core.ui.controllers

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import io.github.aakira.napier.Napier

/** Maximum number of pages that can be captured in a single scanning session. */
private const val SCANNER_PAGE_LIMIT = 20

/**
 * Data class holding the callbacks to trigger document import actions.
 * Passed down to UI components like FABs or Menus.
 */
data class ImportActions(
    val launchPdfPicker: () -> Unit,
    val launchGallery: () -> Unit,
    val launchCamera: () -> Unit
)

/**
 * A headless composable controller that manages ActivityResultLaunchers
 * for document imports.
 *
 * "Take a photo" opens the built in document scanner (automatic border
 * detection, perspective correction, filters and multi page capture). When the
 * scanner cannot be started on the device the plain camera is used instead, so
 * the feature always works.
 *
 * @param cameraUri URI for the camera (fallback capture).
 * @param onUriConsumed Callback when the URI is consumed.
 * @param onCameraClick Callback when the camera fallback has to be used.
 * @param onPdfSelected Callback when a PDF is selected.
 * @param onImagesSelected Callback when images (or scanned pages) are selected.
 * @param onScannerError Callback when the scanner could not be started.
 * @return An ImportActions object with callbacks to trigger document import actions.
 */
@Composable
fun rememberDocumentImportController(
    cameraUri: Uri? = null,
    onUriConsumed: () -> Unit = {},
    onCameraClick: () -> Unit = {},
    onPdfSelected: (Uri) -> Unit = {},
    onImagesSelected: (List<Uri>) -> Unit = {},
    onScannerError: () -> Unit = {}
): ImportActions {
    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraUri != null) {
            onImagesSelected(listOf(cameraUri))
        }
        onUriConsumed()
    }

    // REACCIÓN: Cuando el ViewModel genera el URI, lanzamos la cámara automáticamente
    LaunchedEffect(cameraUri) {
        cameraUri?.let {
            cameraLauncher.launch(it)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImagesSelected(uris)
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let(onPdfSelected)
    }

    // Built in document scanner: returns one image per scanned page.
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult

        val scannedPages = GmsDocumentScanningResult
            .fromActivityResultIntent(result.data)
            ?.pages
            ?.map { page -> page.imageUri }
            .orEmpty()

        if (scannedPages.isNotEmpty()) onImagesSelected(scannedPages)
    }

    return remember(context, onCameraClick, onImagesSelected, onScannerError) {
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
            launchCamera = {
                val activity = context.findActivity()

                if (activity == null) {
                    onCameraClick()
                } else {
                    val scannerOptions = GmsDocumentScannerOptions.Builder()
                        .setGalleryImportAllowed(true)
                        .setPageLimit(SCANNER_PAGE_LIMIT)
                        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                        .build()

                    GmsDocumentScanning.getClient(scannerOptions)
                        .getStartScanIntent(activity)
                        .addOnSuccessListener { intentSender ->
                            runCatching {
                                scannerLauncher.launch(
                                    IntentSenderRequest.Builder(intentSender).build()
                                )
                            }.onFailure { error ->
                                Napier.e("Error launching the document scanner", error)
                                onScannerError()
                                onCameraClick()
                            }
                        }
                        .addOnFailureListener { error ->
                            // Play Services missing/outdated: fall back to the camera.
                            Napier.e("Document scanner unavailable", error)
                            onScannerError()
                            onCameraClick()
                        }
                }
            }
        )
    }
}

/** Finds the [Activity] hosting this composable, if any. */
private fun Context.findActivity(): Activity? {
    var currentContext = this

    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }

    return null
}
