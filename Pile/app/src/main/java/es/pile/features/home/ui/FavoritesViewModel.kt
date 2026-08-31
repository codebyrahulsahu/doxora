package es.pile.features.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pile.DocumentModel
import es.pile.core.domain.models.DocumentCoverItem
import es.pile.core.domain.models.DocumentStatusConstants
import es.pile.core.domain.repositories.BitmapCacheRepository
import es.pile.core.domain.repositories.DocumentLockRepository
import es.pile.core.domain.repositories.DocumentModelRepository
import es.pile.core.domain.repositories.FavoritesRepository
import es.pile.core.domain.useCases.GetDocumentSizesUseCase
import es.pile.core.domain.useCases.RequestCoverThumbnailUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val documentRepository: DocumentModelRepository,
    private val favoritesRepository: FavoritesRepository,
    private val documentLockRepository: DocumentLockRepository,
    private val getDocumentSizesUseCase: GetDocumentSizesUseCase,
    private val requestCoverThumbnailUseCase: RequestCoverThumbnailUseCase,
    private val bitmapCacheRepository: BitmapCacheRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FavoritesState())
    val state: StateFlow<FavoritesState> = _state.asStateFlow()

    val bitmapCache = bitmapCacheRepository.bitmapCache

    init {
        viewModelScope.launch {
            documentLockRepository.lockedDocumentIds.collect { ids ->
                _state.update { it.copy(lockedDocumentIds = ids) }
            }
        }

        viewModelScope.launch {
            combine(
                documentRepository.documentModels,
                favoritesRepository.favoriteDocumentIds
            ) { documents, favoriteIds ->
                val favorites = favoriteIds.toSet()

                documents
                    .filter { it.id in favorites && it.documentStatus != DocumentStatusConstants.TEMPORARY }
                    .map { documentModel ->
                        DocumentCoverItem(
                            document = documentModel,
                            coverImageCacheKey = bitmapCacheRepository.getCoverKey(documentModel)
                        )
                    }
            }.collect { documents ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        documents = documents,
                        favoriteDocumentIds = documents.map { item -> item.document.id }.toSet()
                    )
                }

                _state.update { current ->
                    current.copy(
                        documentSizes = getDocumentSizesUseCase(current.documents.map { it.document })
                    )
                }
            }
        }
    }

    fun handleEvent(event: FavoritesEvent) {
        when (event) {
            is FavoritesEvent.OnImageDisplayed -> requestCoverThumbnail(event.document)

            is FavoritesEvent.OnFavoriteToggled -> viewModelScope.launch {
                favoritesRepository.setFavorite(
                    event.documentId,
                    event.documentId !in state.value.favoriteDocumentIds
                )
            }

            is FavoritesEvent.OnSortOrderChanged ->
                _state.update { it.copy(sortOrder = event.sortOrder) }
        }
    }

    private fun requestCoverThumbnail(document: DocumentModel) {
        viewModelScope.launch {
            requestCoverThumbnailUseCase(document)
        }
    }
}
