package es.pile.core.ui.controllers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract

/**
 * Access to the "Document" mode built into the camera app of the device.
 *
 * Android does not expose a standard API to open the camera straight into its
 * document scanning mode, so the mode is requested in two complementary ways:
 *
 * 1. **Vendor document capture actions** ([DOCUMENT_MODE_ACTIONS]): several OEM
 *    camera apps (Samsung, Xiaomi, Oppo/OnePlus, Huawei, Motorola…) publish an
 *    activity that starts the camera directly in document mode. They are only
 *    used when the device really declares them, so nothing changes on devices
 *    that do not ship one.
 * 2. **Document mode extras** ([DOCUMENT_MODE_EXTRAS]): when the plain capture
 *    intent is used, the same hints are attached to it. Camera apps that
 *    understand them open in document mode, the rest simply ignore them and
 *    take a normal picture.
 *
 * When neither is available the plain device camera is opened, still asking
 * for its document mode through the capture hints (see
 * [rememberDocumentImportController]). The Google Play services document
 * scanner is never used.
 */
object NativeDocumentCamera {

    /**
     * Intent actions used by OEM camera apps to capture a page directly with the
     * built in "Document" mode. Resolved against the package manager before
     * being used, so unknown actions are simply skipped.
     */
    private val DOCUMENT_MODE_ACTIONS = listOf(
        "com.samsung.android.camera.action.DOCUMENT_SCAN",
        "com.sec.android.app.camera.action.DOCUMENT_SCAN",
        "miui.intent.action.SCAN_DOCUMENT",
        "com.oplus.camera.action.DOCUMENT_SCAN",
        "com.oneplus.camera.action.DOCUMENT_SCAN",
        "com.huawei.camera.action.DOCUMENT_SCAN",
        "com.motorola.camera.action.DOCUMENT_SCAN"
    )

    /**
     * Extras attached to the capture intent asking the camera app to preselect
     * its document shooting mode. Unknown extras are ignored by the camera.
     */
    private val DOCUMENT_MODE_EXTRAS = mapOf(
        "com.samsung.android.camera.extra.SHOOTING_MODE" to "document",
        "com.android.camera.extra.SHOOTING_MODE" to "document",
        "android.intent.extra.CAMERA_MODE" to "document"
    )

    /**
     * True when the camera app of the device exposes a dedicated document
     * capture activity that can be launched directly.
     */
    fun isDocumentModeAvailable(context: Context): Boolean =
        resolveDocumentModeAction(context) != null

    /**
     * Builds the intent that opens the native camera in document mode writing
     * the captured page into [outputUri], or null when the device does not
     * expose such a mode.
     */
    fun createDocumentModeIntent(context: Context, outputUri: Uri): Intent? {
        val action = resolveDocumentModeAction(context) ?: return null

        return Intent(action)
            .putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
            .withDocumentModeHints()
    }

    /** Returns the first vendor document capture action available on the device. */
    private fun resolveDocumentModeAction(context: Context): String? =
        DOCUMENT_MODE_ACTIONS.firstOrNull { action ->
            val intent = Intent(action)
            intent.resolveActivity(context.packageManager) != null
        }

    /** Adds the document mode hints and the URI permissions to a capture intent. */
    internal fun Intent.withDocumentModeHints(): Intent = apply {
        DOCUMENT_MODE_EXTRAS.forEach { (key, value) -> putExtra(key, value) }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
}

/**
 * Same contract as `ActivityResultContracts.TakePicture`, but the capture intent
 * also asks the camera app to open in its built in "Document" mode.
 *
 * The result is true when the picture was written into the input [Uri].
 */
class TakeDocumentPicture : ActivityResultContract<Uri, Boolean>() {

    override fun createIntent(context: Context, input: Uri): Intent =
        with(NativeDocumentCamera) {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                .putExtra(MediaStore.EXTRA_OUTPUT, input)
                .withDocumentModeHints()
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        resultCode == Activity.RESULT_OK
}
