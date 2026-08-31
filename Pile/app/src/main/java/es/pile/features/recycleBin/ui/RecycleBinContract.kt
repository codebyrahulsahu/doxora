package es.pile.features.recycleBin.ui

import es.pile.DocumentModel
import es.pile.core.ui.util.UiText
import es.pile.features.recycleBin.domain.models.TrashedDocument

data class RecycleBinState(
    val isLoading: Boolean = true,
    val trashedDocuments: List<TrashedDocument> = emptyList(),
    val documentSizes: Map<String, Long> = emptyMap(),
    val lockedDocumentIds: Set<String> = emptySet(),
    val userMessage: UiText? = null
)

sealed interface RecycleBinEvent {
    data class OnImageDisplayed(val document: DocumentModel) : RecycleBinEvent
    data class OnRestore(val documentId: String) : RecycleBinEvent
    data class OnDeleteForever(val documentId: String) : RecycleBinEvent
    data object OnMessageDismissed : RecycleBinEvent
}
