package es.pile.features.home.ui

import es.pile.DocumentModel
import es.pile.core.domain.models.DocumentCoverItem
import es.pile.core.domain.models.DocumentSortOrder

data class FavoritesState(
    val isLoading: Boolean = true,
    val documents: List<DocumentCoverItem> = emptyList(),
    val documentSizes: Map<String, Long> = emptyMap(),
    val favoriteDocumentIds: Set<String> = emptySet(),
    val lockedDocumentIds: Set<String> = emptySet(),
    val sortOrder: DocumentSortOrder = DocumentSortOrder.NEWEST
)

sealed interface FavoritesEvent {
    data class OnImageDisplayed(val document: DocumentModel) : FavoritesEvent
    data class OnFavoriteToggled(val documentId: String) : FavoritesEvent
    data class OnSortOrderChanged(val sortOrder: DocumentSortOrder) : FavoritesEvent
}
