package es.pile.core.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val isMaterialColor: Boolean = true,
    val isLocalAiEnabled: Boolean = false,
    val selectedModel: String? = null,
    val imageResolution: ImageResolution = ImageResolution.ORIGINAL,
    /**
     * Whether new document images are compressed to a custom target file size
     * by the Document Resizer (keeps the best possible quality while fitting
     * the configured [documentResizerTargetSizeKb]).
     */
    val isDocumentResizerEnabled: Boolean = false,
    /** Custom target file size used by the Document Resizer, in kilobytes. */
    val documentResizerTargetSizeKb: Int = 512,
    val profileName: String? = null,
    val profileEmail: String? = null,
    /** File name (inside the app's internal storage) of the profile picture, if any. */
    val profilePicturePath: String? = null,
    /** Layout used to show the documents in the main screen. */
    val documentViewMode: DocumentViewMode = DocumentViewMode.LIST,
    /**
     * Profile picture of every hub, mapped by hub id.
     *
     * The value is the file name (inside the app's internal storage) of the
     * picture uploaded for the person that hub belongs to.
     */
    val hubPicturePaths: Map<String, String> = emptyMap(),
)
