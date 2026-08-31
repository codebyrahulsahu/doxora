package es.pile.features.recycleBin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pile.DocumentModel
import es.pile.R
import es.pile.core.domain.repositories.BitmapCacheRepository
import es.pile.core.domain.repositories.DocumentLockRepository
import es.pile.core.domain.useCases.GetDocumentSizesUseCase
import es.pile.core.domain.useCases.RequestCoverThumbnailUseCase
import es.pile.core.ui.util.UiText
import es.pile.features.recycleBin.domain.useCases.DeleteTrashedDocumentUseCase
import es.pile.features.recycleBin.domain.useCases.GetTrashDataUseCase
import es.pile.features.recycleBin.domain.useCases.PurgeExpiredTrashEntriesUseCase
import es.pile.features.recycleBin.domain.useCases.RestoreTrashedDocumentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecycleBinViewModel(
    private val getTrashDataUseCase: GetTrashDataUseCase,
    private val restoreTrashedDocumentUseCase: RestoreTrashedDocumentUseCase,
    private val deleteTrashedDocumentUseCase: DeleteTrashedDocumentUseCase,
    private val purgeExpiredTrashEntriesUseCase: PurgeExpiredTrashEntriesUseCase,
    private val getDocumentSizesUseCase: GetDocumentSizesUseCase,
    private val requestCoverThumbnailUseCase: RequestCoverThumbnailUseCase,
    private val documentLockRepository: DocumentLockRepository,
    private val bitmapCacheRepository: BitmapCacheRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RecycleBinState())
    val state: StateFlow<RecycleBinState> = _state.asStateFlow()

    val bitmapCache = bitmapCacheRepository.bitmapCache

    init {
        viewModelScope.launch {
            // Safety net: drop anything that already passed the retention period
            // before showing the list.
            purgeExpiredTrashEntriesUseCase()
        }

        viewModelScope.launch {
            documentLockRepository.lockedDocumentIds.collect { ids ->
                _state.update { it.copy(lockedDocumentIds = ids) }
            }
        }

        viewModelScope.launch {
            getTrashDataUseCase().collect { documents ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        trashedDocuments = documents
                    )
                }

                val sizes = getDocumentSizesUseCase(documents.map { it.document })
                _state.update { it.copy(documentSizes = sizes) }
            }
        }
    }

    fun handleEvent(event: RecycleBinEvent) {
        when (event) {
            is RecycleBinEvent.OnImageDisplayed -> requestCoverThumbnail(event.document)
            is RecycleBinEvent.OnRestore -> restoreDocument(event.documentId)
            is RecycleBinEvent.OnDeleteForever -> deleteForever(event.documentId)
            RecycleBinEvent.OnMessageDismissed -> _state.update { it.copy(userMessage = null) }
        }
    }

    private fun requestCoverThumbnail(document: DocumentModel) {
        viewModelScope.launch {
            requestCoverThumbnailUseCase(document)
        }
    }

    private fun restoreDocument(documentId: String) {
        viewModelScope.launch {
            try {
                restoreTrashedDocumentUseCase(documentId)
                _state.update {
                    it.copy(userMessage = UiText.StringResource(R.string.document_restored))
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(userMessage = UiText.StringResource(R.string.error_restoring_document))
                }
            }
        }
    }

    private fun deleteForever(documentId: String) {
        viewModelScope.launch {
            try {
                deleteTrashedDocumentUseCase(documentId)
                _state.update {
                    it.copy(userMessage = UiText.StringResource(R.string.document_deleted_forever))
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(userMessage = UiText.StringResource(R.string.error_deleting_document))
                }
            }
        }
    }
}
