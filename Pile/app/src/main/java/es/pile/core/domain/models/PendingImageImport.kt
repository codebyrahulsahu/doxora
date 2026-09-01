package es.pile.core.domain.models

import android.net.Uri

/**
 * Images picked by the user that are waiting for the compression prompt to be
 * answered before they are actually imported.
 *
 * @property uris The images selected in the gallery, camera or scanner.
 * @property defaultChoice Compression pre-selected in the prompt, derived from
 * the Document Resizer settings.
 */
data class PendingImageImport(
    val uris: List<Uri>,
    val defaultChoice: ImageCompressionChoice
)
