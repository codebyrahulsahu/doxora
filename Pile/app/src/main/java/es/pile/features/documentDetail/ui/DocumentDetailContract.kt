package es.pile.features.documentDetail.ui

import es.pile.DocumentModel
import es.pile.PileModel
import es.pile.core.domain.models.DocumentDetail
import es.pile.core.domain.models.DocumentExportFormat
import es.pile.core.domain.models.DocumentLockType
import es.pile.core.ui.util.UiText


data class DocumentDetailState(
    val documentModel: DocumentModel? = null,
    val localDocumentDetails: List<DocumentDetail>? = null,
    val documentPileModels: List<PileModel>? = null,
    val pageCacheKeys: List<String> = emptyList(),
    val pdfPageCount: Int? = null,
    val allPiles: List<PileModel>? = null,
    val isDetailsEditing: Boolean = false,
    val isExporting: Boolean = false,
    val recognizedText: String? = null,
    val isRecognizingText: Boolean = false,
    val isFavorite: Boolean = false,
    val isLocked: Boolean = false,
    val isUnlocked: Boolean = false,
    val lockType: DocumentLockType = DocumentLockType.PIN,
    val userMessage: UiText? = null
)

sealed interface DocumentDetailEvent {
    data class OnImageDisplayed(val pageNumber: Int) : DocumentDetailEvent
    data class OnUpdateEditingMode(val isEditing: Boolean) : DocumentDetailEvent

    data class OnRenameDocument(val newName: String) : DocumentDetailEvent
    data class OnUpdateDetails(val event: DetailsActionEvent) : DocumentDetailEvent
    data class OnUpdateNote(val newNote: String) : DocumentDetailEvent
    data class OnUpdatePileSelection(val pileId: String) : DocumentDetailEvent
    data class OnNewPile(val pileName: String, val iconId: String, val color: Long) :
        DocumentDetailEvent

    data object OnDeleteDocument : DocumentDetailEvent

    data object OnOpenDocument : DocumentDetailEvent
    data object OnShare : DocumentDetailEvent
    data class OnExportDocument(val format: DocumentExportFormat) : DocumentDetailEvent

    // Text recognition (OCR)
    data object OnRecognizeText : DocumentDetailEvent
    data class OnUpdateRecognizedText(val newText: String) : DocumentDetailEvent
    data object OnDeleteRecognizedText : DocumentDetailEvent

    // Favorites and per document PIN / pattern protection
    data object OnFavoriteToggled : DocumentDetailEvent
    data class OnLockDocument(val secret: String, val type: DocumentLockType) : DocumentDetailEvent

    data object OnMessageDismissed : DocumentDetailEvent
}

sealed interface DetailsActionEvent {
    data object OnNew : DetailsActionEvent

    data class OnUpdateText(val id: String, val newName: String, val newValue: String) :
        DetailsActionEvent

    data class OnIndexMove(val fromIndex: Int, val toIndex: Int) : DetailsActionEvent
    data class OnIdMove(val fromId: String, val toId: String) : DetailsActionEvent

    data class OnRemove(val index: Int) : DetailsActionEvent
    data object OnRestore : DetailsActionEvent
    data object OnPurge : DetailsActionEvent
}
