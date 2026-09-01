package es.pile.features.pileDetail.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pile.DocumentModel
import es.pile.R
import es.pile.core.domain.models.DocumentCoverItem
import es.pile.core.domain.models.DocumentExportFormat
import es.pile.core.domain.models.DocumentResizeMode
import es.pile.core.domain.models.DocumentStatusConstants.SAVED
import es.pile.core.domain.models.DocumentStatusConstants.TEMPORARY
import es.pile.core.domain.repositories.BitmapCacheRepository
import es.pile.core.domain.repositories.DocumentLockRepository
import es.pile.core.domain.repositories.DocumentModelRepository
import es.pile.core.domain.repositories.FavoritesRepository
import es.pile.core.domain.repositories.FileRepository
import es.pile.core.domain.repositories.PileModelRepository
import es.pile.core.domain.repositories.SettingsRepository
import es.pile.core.domain.useCases.GetDocumentSizesUseCase
import es.pile.core.domain.useCases.RequestCoverThumbnailUseCase
import es.pile.core.domain.useCases.ResizeDocumentsUseCase
import es.pile.core.ui.util.UiText
import es.pile.features.documentDetail.domain.helper.DocumentOpener
import es.pile.features.documentDetail.domain.useCases.MoveDocumentToTrashUseCase
import es.pile.features.documentDetail.domain.useCases.export.ExportDocumentImagesUseCase
import es.pile.features.documentDetail.domain.useCases.export.ExportDocumentUseCase
import es.pile.features.documentDetail.domain.useCases.export.GetPdfUriUseCase
import es.pile.features.home.domain.useCases.CreateDocumentUseCase
import es.pile.features.pileDetail.domain.usecases.DeletePileUseCase
import es.pile.features.pileDetail.domain.usecases.UpdatePileUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PileDetailViewModel(
    private val pileId: String,
    private val requestCoverThumbnailUseCase: RequestCoverThumbnailUseCase,
    private val createDocumentUseCase: CreateDocumentUseCase,
    private val updatePileUseCase: UpdatePileUseCase,
    private val deletePileUseCase: DeletePileUseCase,
    private val getDocumentSizesUseCase: GetDocumentSizesUseCase,
    private val pileModelRepository: PileModelRepository,
    private val documentModelRepository: DocumentModelRepository,
    private val bitmapCacheRepository: BitmapCacheRepository,
    private val fileRepository: FileRepository,
    private val favoritesRepository: FavoritesRepository,
    private val documentLockRepository: DocumentLockRepository,
    private val settingsRepository: SettingsRepository,
    private val moveDocumentToTrashUseCase: MoveDocumentToTrashUseCase,
    private val getPdfUriUseCase: GetPdfUriUseCase,
    private val exportDocumentUseCase: ExportDocumentUseCase,
    private val exportDocumentImagesUseCase: ExportDocumentImagesUseCase,
    private val resizeDocumentsUseCase: ResizeDocumentsUseCase,
    private val documentOpener: DocumentOpener
) : ViewModel() {
    private val _state = MutableStateFlow(PileDetailState())
    val state: StateFlow<PileDetailState> = _state.asStateFlow()

    val bitmapCache = bitmapCacheRepository.bitmapCache

    private val _navigationEvent = Channel<DocumentModel>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private var pendingImportAction: (() -> Unit)? = null

    init {
        viewModelScope.launch {
            favoritesRepository.favoriteDocumentIds.collect { ids ->
                _state.update { it.copy(favoriteDocumentIds = ids.toSet()) }
            }
        }
        viewModelScope.launch {
            documentLockRepository.lockedDocumentIds.collect { ids ->
                _state.update { it.copy(lockedDocumentIds = ids) }
            }
        }
        viewModelScope.launch {
            settingsRepository.userSettings.collect { settings ->
                val hubPictureFile = settings.hubPicturePaths[pileId]
                    ?.let { path -> fileRepository.getProfilePictureFile(path) }
                    ?.takeIf { file -> file.exists() }

                _state.update { it.copy(hubPictureFile = hubPictureFile) }
            }
        }
        viewModelScope.launch {
            val pileFlow = pileModelRepository.getPileModelById(pileId)
            val allDocumentsFlow = documentModelRepository.documentModels

            combine(
                pileFlow,
                allDocumentsFlow
            ) { pile, allDocuments ->

                val pileDocuments = allDocuments.filter { it.documentPileIds.contains(pileId) && it.documentStatus == SAVED }
                val temporaryDocument = allDocuments.find { it.documentStatus == TEMPORARY }

                val documentCoverItems = pileDocuments.map { documentModel ->
                    DocumentCoverItem(
                        document = documentModel,
                        coverImageCacheKey = bitmapCacheRepository.getCoverKey(documentModel)
                    )
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        pile = pile,
                        documentCoverItems = documentCoverItems,
                        temporaryDocument = temporaryDocument
                    )
                }

                val documentSizes = getDocumentSizesUseCase(pileDocuments)
                _state.update { it.copy(documentSizes = documentSizes) }
            }.collect()
        }
    }

    fun handleEvent(event: PileDetailEvent) {
        when (event) {
            is PileDetailEvent.OnImageDisplayed -> requestCoverThumbnail(event.document)

            is PileDetailEvent.OnFavoriteToggled -> viewModelScope.launch {
                favoritesRepository.setFavorite(
                    event.documentId,
                    event.documentId !in state.value.favoriteDocumentIds
                )
            }
            PileDetailEvent.OnDeletePile -> deletePile()
            is PileDetailEvent.OnPileChange -> updatePile(event.name, event.iconId, event.color)

            is PileDetailEvent.OnPdfImported -> {
                if (state.value.temporaryDocument != null) {
                    pendingImportAction = { importPDF(event.uri) }
                    _state.update { it.copy(showDraftWarning = true) }
                } else {
                    importPDF(event.uri)
                }
            }

            is PileDetailEvent.OnImagesImported -> requestImageImport(event.uris)

            PileDetailEvent.OnCameraClick -> {
                if (state.value.temporaryDocument != null) {
                    pendingImportAction = { createCameraUri() }
                    _state.update { it.copy(showDraftWarning = true) }
                } else {
                    createCameraUri()
                }
            }

            PileDetailEvent.OnCameraUriConsumed -> dismissCameraUri()

            is PileDetailEvent.OnHubPicturePicked -> loadHubPictureForCropping(event.uri)

            is PileDetailEvent.OnHubPictureCropConfirmed -> saveHubPicture(event.bitmap)

            PileDetailEvent.OnHubPictureCropDismissed ->
                _state.update { it.copy(hubPictureToCrop = null) }

            PileDetailEvent.OnHubPictureRemoved -> removeHubPicture()

            is PileDetailEvent.OnDocumentLongPressed -> toggleDocumentSelection(event.documentId)

            is PileDetailEvent.OnDocumentSelectionToggled ->
                toggleDocumentSelection(event.documentId)

            PileDetailEvent.OnSelectionCleared -> clearSelection()

            is PileDetailEvent.OnExportSelectedClicked -> exportSelectedDocuments(
                event.format,
                event.destinationFolderUri
            )

            PileDetailEvent.OnShareSelectedClicked -> shareSelectedDocuments()

            PileDetailEvent.OnDeleteSelectedClicked -> deleteSelectedDocuments()

            is PileDetailEvent.OnResizeSelectedClicked -> resizeSelectedDocuments(event.mode)

            PileDetailEvent.OnConfirmImport -> {
                _state.update { it.copy(showDraftWarning = false) }
                pendingImportAction?.invoke()
                pendingImportAction = null
            }

            PileDetailEvent.OnDismissDraftWarning -> {
                _state.update { it.copy(showDraftWarning = false) }
                pendingImportAction = null
            }

            is PileDetailEvent.OnSortOrderChanged -> {
                _state.update { it.copy(sortOrder = event.sortOrder) }
            }

            PileDetailEvent.OnErrorDismissed -> _state.update { it.copy(errorMessage = null) }
        }
    }

    /** Adds or removes a document from the current selection. */
    private fun toggleDocumentSelection(documentId: String) {
        _state.update {
            it.copy(
                selectedDocumentIds = if (documentId in it.selectedDocumentIds) {
                    it.selectedDocumentIds - documentId
                } else {
                    it.selectedDocumentIds + documentId
                }
            )
        }
    }

    private fun clearSelection() {
        _state.update { it.copy(selectedDocumentIds = emptySet()) }
    }

    private fun selectedDocuments(): List<DocumentModel> {
        val selectedIds = state.value.selectedDocumentIds

        return state.value.documentCoverItems
            .map { it.document }
            .filter { it.id in selectedIds }
    }

    /**
     * Exports every selected document of the hub with the chosen [format].
     */
    private fun exportSelectedDocuments(
        format: DocumentExportFormat,
        destinationFolderUri: Uri?
    ) {
        viewModelScope.launch {
            val documents = selectedDocuments()
            if (documents.isEmpty()) return@launch

            _state.update { it.copy(isSelectionWorking = true) }

            var exportedCount = 0
            documents.forEach { document ->
                runCatching {
                    when (format) {
                        DocumentExportFormat.PDF ->
                            exportDocumentUseCase(document, destinationFolderUri).getOrThrow()

                        DocumentExportFormat.JPG,
                        DocumentExportFormat.PNG ->
                            exportDocumentImagesUseCase(document, format, destinationFolderUri)
                    }
                }.onSuccess { exportedCount++ }
                    .onFailure { error -> Napier.e("Error exporting document", error) }
            }

            _state.update {
                it.copy(
                    isSelectionWorking = false,
                    selectedDocumentIds = emptySet(),
                    errorMessage = when {
                        exportedCount == 0 ->
                            UiText.StringResource(R.string.error_exporting_documents)

                        destinationFolderUri != null -> UiText.StringResource(
                            R.string.documents_exported_to_folder,
                            exportedCount
                        )

                        else -> UiText.StringResource(
                            R.string.documents_exported,
                            exportedCount
                        )
                    }
                )
            }
        }
    }

    /** Opens the system share sheet with a PDF of every selected document. */
    private fun shareSelectedDocuments() {
        viewModelScope.launch {
            val documents = selectedDocuments()
            if (documents.isEmpty()) return@launch

            _state.update { it.copy(isSelectionWorking = true) }

            val uris = documents.mapNotNull { document ->
                runCatching { getPdfUriUseCase(document) }.getOrNull()
            }

            _state.update { it.copy(isSelectionWorking = false) }

            if (uris.isEmpty()) {
                _state.update {
                    it.copy(errorMessage = UiText.StringResource(R.string.error_sharing_documents))
                }
            } else {
                clearSelection()
                documentOpener.sharePdf(uris)
            }
        }
    }

    /** Moves every selected document of the hub into the Recycle Bin. */
    private fun deleteSelectedDocuments() {
        viewModelScope.launch {
            val documents = selectedDocuments()
            if (documents.isEmpty()) return@launch

            documents.forEach { document -> moveDocumentToTrashUseCase(document) }

            _state.update {
                it.copy(
                    selectedDocumentIds = emptySet(),
                    errorMessage = UiText.StringResource(
                        R.string.documents_moved_to_recycle_bin,
                        documents.size
                    )
                )
            }
        }
    }

    /**
     * Loads the picked picture so it can be adjusted with the cropper before it
     * becomes the picture of this hub.
     */
    private fun loadHubPictureForCropping(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isPreparingHubPicture = true) }

            runCatching { fileRepository.loadPictureForCropping(uri) }
                .onSuccess { bitmap ->
                    _state.update {
                        it.copy(isPreparingHubPicture = false, hubPictureToCrop = bitmap)
                    }
                }
                .onFailure { error ->
                    Napier.e("Error loading the hub picture", error)
                    _state.update {
                        it.copy(
                            isPreparingHubPicture = false,
                            hubPictureToCrop = null,
                            errorMessage = UiText.StringResource(R.string.error_loading_picture)
                        )
                    }
                }
        }
    }

    /**
     * Stores the cropped picture uploaded for the person this hub belongs to.
     */
    private fun saveHubPicture(bitmap: Bitmap) {
        viewModelScope.launch {
            _state.update { it.copy(isWorkingOnHubPicture = true, hubPictureToCrop = null) }

            val currentPath = settingsRepository.userSettings.first().hubPicturePaths[pileId]

            runCatching { fileRepository.saveProfilePicture(bitmap, currentPath) }
                .onSuccess { fileName ->
                    settingsRepository.updateHubPicturePath(pileId, fileName)
                    _state.update {
                        it.copy(
                            isWorkingOnHubPicture = false,
                            errorMessage = UiText.StringResource(R.string.hub_picture_updated)
                        )
                    }
                }
                .onFailure { error ->
                    Napier.e("Error saving the hub picture", error)
                    _state.update {
                        it.copy(
                            isWorkingOnHubPicture = false,
                            errorMessage = UiText.StringResource(
                                R.string.error_setting_hub_picture
                            )
                        )
                    }
                }
        }
    }

    /** Removes the picture uploaded for this hub. */
    private fun removeHubPicture() {
        viewModelScope.launch {
            val currentPath = settingsRepository.userSettings.first().hubPicturePaths[pileId]
                ?: return@launch

            runCatching { fileRepository.getProfilePictureFile(currentPath).delete() }

            settingsRepository.updateHubPicturePath(pileId, null)

            _state.update {
                it.copy(errorMessage = UiText.StringResource(R.string.hub_picture_removed))
            }
        }
    }

    private fun requestCoverThumbnail(document: DocumentModel) {
        viewModelScope.launch {
            requestCoverThumbnailUseCase(document)
        }
    }

    private fun updatePile(name: String, iconId: String, color: Long) {
        val pile = state.value.pile ?: return

        viewModelScope.launch {
            updatePileUseCase(id = pile.id, name = name, iconId = iconId, color = color)
        }
    }

    private fun deletePile() {
        val pile = state.value.pile ?: return

        viewModelScope.launch {
            val picturePath = settingsRepository.userSettings.first().hubPicturePaths[pile.id]

            if (picturePath != null) {
                runCatching { fileRepository.getProfilePictureFile(picturePath).delete() }
                settingsRepository.updateHubPicturePath(pile.id, null)
            }

            deletePileUseCase(pile.id)
        }
    }

    private fun createCameraUri() {
        viewModelScope.launch {
            _state.update {
                it.copy(cameraUri = fileRepository.createTempImageUri())
            }
        }
    }

    private fun dismissCameraUri() = _state.update { it.copy(cameraUri = null) }

    private fun importPDF(uri: Uri) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoadingNewDocument = true) }
                val newDoc = createDocumentUseCase.createFromPdf(uri, listOf(pileId))
                _navigationEvent.send(newDoc)
            } catch (e: Exception) {
                Napier.e("Error importing PDF from Pile Detail", e)
                _state.update {
                    it.copy(errorMessage = UiText.StringResource(R.string.error_importing_pdf))
                }
            } finally {
                _state.update { it.copy(isLoadingNewDocument = false) }
            }
        }
    }

    /**
     * Imports the picked images right away (the draft warning is shown first
     * when an unsaved draft document exists).
     */
    private fun requestImageImport(uris: List<Uri>) {
        if (state.value.temporaryDocument != null) {
            pendingImportAction = { importImages(uris) }
            _state.update { it.copy(showDraftWarning = true) }
        } else {
            importImages(uris)
        }
    }

    private fun importImages(uris: List<Uri>) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoadingNewDocument = true) }
                val newDoc = createDocumentUseCase.createFromImages(
                    uris,
                    initialPileIds = listOf(pileId)
                )
                _navigationEvent.send(newDoc)
            } catch (e: Exception) {
                Napier.e("Error importing images from Pile Detail", e)
                _state.update {
                    it.copy(errorMessage = UiText.StringResource(R.string.error_importing_images))
                }
            } finally {
                _state.update { it.copy(isLoadingNewDocument = false) }
            }
        }
    }

    /**
     * Resizes every selected document with the Document Resizer, saving the
     * result over the original files or as duplicate documents depending on
     * the option chosen in the two-option prompt.
     */
    private fun resizeSelectedDocuments(mode: DocumentResizeMode) {
        viewModelScope.launch {
            val documents = selectedDocuments()
            if (documents.isEmpty()) return@launch

            _state.update { it.copy(isSelectionWorking = true) }

            val result = resizeDocumentsUseCase(documents, mode)

            _state.update {
                it.copy(
                    isSelectionWorking = false,
                    selectedDocumentIds = emptySet(),
                    errorMessage = when {
                        result.resizedCount == 0 ->
                            UiText.StringResource(R.string.error_resizing_documents)

                        mode == DocumentResizeMode.SAVE_AS_DUPLICATE -> UiText.StringResource(
                            R.string.documents_resized_duplicates,
                            result.resizedCount
                        )

                        else -> UiText.StringResource(
                            R.string.documents_resized,
                            result.resizedCount
                        )
                    }
                )
            }
        }
    }
}
