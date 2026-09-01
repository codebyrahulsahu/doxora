package es.pile.features.pileDetail.ui

import android.graphics.Bitmap
import android.net.Uri
import es.pile.DocumentModel
import es.pile.PileModel
import es.pile.core.domain.models.DocumentCoverItem
import es.pile.core.domain.models.DocumentExportFormat
import es.pile.core.domain.models.DocumentResizeMode
import es.pile.core.domain.models.DocumentSortOrder
import es.pile.core.ui.util.UiText
import java.io.File


data class PileDetailState(
    val pile: PileModel? = null,
    val documentCoverItems: List<DocumentCoverItem> = emptyList(),
    val documentSizes: Map<String, Long> = emptyMap(),
    val sortOrder: DocumentSortOrder = DocumentSortOrder.NEWEST,
    val temporaryDocument: DocumentModel? = null,
    val cameraUri: Uri? = null,
    val isLoading: Boolean = true,
    val showDraftWarning: Boolean = false,
    val isLoadingNewDocument: Boolean = false,
    val favoriteDocumentIds: Set<String> = emptySet(),
    val lockedDocumentIds: Set<String> = emptySet(),
    /** Documents selected while the multi selection mode is active. */
    val selectedDocumentIds: Set<String> = emptySet(),
    /** True while a selection action (export, share or delete) is running. */
    val isSelectionWorking: Boolean = false,
    /** Profile picture uploaded for this hub, if any. */
    val hubPictureFile: File? = null,
    /**
     * Picture just picked for this hub, waiting to be adjusted with the cropper
     * before it is stored.
     */
    val hubPictureToCrop: Bitmap? = null,
    /** True while the picked picture is being decoded for the cropper. */
    val isPreparingHubPicture: Boolean = false,
    /** True while the cropped picture is being stored. */
    val isWorkingOnHubPicture: Boolean = false,
    val errorMessage: UiText? = null
) {
    /** True while one or more documents are selected inside the hub. */
    val isSelectionMode: Boolean get() = selectedDocumentIds.isNotEmpty()
}

sealed interface PileDetailEvent {
    data class OnImageDisplayed(val document: DocumentModel) : PileDetailEvent

    data class OnFavoriteToggled(val documentId: String) : PileDetailEvent
    data class OnPileChange(val name: String, val iconId: String, val color: Long) :
        PileDetailEvent

    data object OnDeletePile : PileDetailEvent

    data class OnPdfImported(val uri: Uri) : PileDetailEvent
    data class OnImagesImported(val uris: List<Uri>) : PileDetailEvent
    data object OnCameraClick : PileDetailEvent
    data object OnCameraUriConsumed : PileDetailEvent

    data object OnConfirmImport : PileDetailEvent
    data object OnDismissDraftWarning : PileDetailEvent

    data class OnSortOrderChanged(val sortOrder: DocumentSortOrder) : PileDetailEvent

    // Hub profile picture
    /** A picture was picked for the person this hub belongs to. */
    data class OnHubPicturePicked(val uri: Uri) : PileDetailEvent

    /** The picked picture was cropped and can be stored as the hub picture. */
    data class OnHubPictureCropConfirmed(val bitmap: Bitmap) : PileDetailEvent

    /** The cropper was closed without saving: the picked picture is discarded. */
    data object OnHubPictureCropDismissed : PileDetailEvent
    data object OnHubPictureRemoved : PileDetailEvent

    // Multi selection actions
    data class OnDocumentLongPressed(val documentId: String) : PileDetailEvent
    data class OnDocumentSelectionToggled(val documentId: String) : PileDetailEvent
    data object OnSelectionCleared : PileDetailEvent

    /**
     * Exports the selected documents with the chosen format.
     *
     * @param destinationFolderUri Folder granted by the user (only asked once)
     * or null to fall back to the public Downloads directory.
     */
    data class OnExportSelectedClicked(
        val format: DocumentExportFormat,
        val destinationFolderUri: Uri? = null
    ) : PileDetailEvent

    data object OnShareSelectedClicked : PileDetailEvent
    data object OnDeleteSelectedClicked : PileDetailEvent

    /**
     * Resizes every selected document with the Document Resizer, saving the
     * result as chosen in the prompt, at the custom [targetSizeKb].
     */
    data class OnResizeSelectedClicked(
        val mode: DocumentResizeMode,
        val targetSizeKb: Int
    ) : PileDetailEvent

    data object OnErrorDismissed : PileDetailEvent
}
