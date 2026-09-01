package es.pile.features.home.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pile.DocumentModel
import es.pile.R
import es.pile.core.domain.models.DocumentCoverItem
import es.pile.core.domain.models.DocumentExportFormat
import es.pile.core.domain.models.DocumentViewMode
import es.pile.core.domain.models.ImageCompressionChoice
import es.pile.core.domain.models.PendingImageImport
import es.pile.core.domain.repositories.BitmapCacheRepository
import es.pile.core.domain.repositories.DocumentLockRepository
import es.pile.core.domain.repositories.FileRepository
import es.pile.core.domain.repositories.FavoritesRepository
import es.pile.core.domain.repositories.PileModelRepository
import es.pile.core.domain.repositories.SettingsRepository
import es.pile.core.domain.useCases.CreatePileUseCase
import es.pile.core.domain.useCases.GetDocumentSizesUseCase
import es.pile.core.domain.useCases.RequestCoverThumbnailUseCase
import es.pile.core.ui.util.UiText
import es.pile.features.documentDetail.domain.helper.DocumentOpener
import es.pile.features.documentDetail.domain.useCases.MoveDocumentToTrashUseCase
import es.pile.features.documentDetail.domain.useCases.export.ExportDocumentImagesUseCase
import es.pile.features.documentDetail.domain.useCases.export.ExportDocumentUseCase
import es.pile.features.documentDetail.domain.useCases.export.GetPdfUriUseCase
import es.pile.features.home.domain.models.TemporaryDocumentBackup
import es.pile.features.home.domain.schedulers.CleanupScheduler
import es.pile.features.home.domain.useCases.CreateDocumentUseCase
import es.pile.features.home.domain.useCases.GetHomeDataUseCase
import es.pile.features.home.domain.useCases.ManageTemporaryDocumentUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val createDocumentUseCase: CreateDocumentUseCase,
    private val manageTemporaryDocumentUseCase: ManageTemporaryDocumentUseCase,
    private val getHomeDataUseCase: GetHomeDataUseCase,
    private val createPileUseCase: CreatePileUseCase,
    private val requestCoverThumbnailUseCase: RequestCoverThumbnailUseCase,
    private val getDocumentSizesUseCase: GetDocumentSizesUseCase,
    private val cleanupScheduler: CleanupScheduler,
    private val bitmapCacheRepository: BitmapCacheRepository,
    private val fileRepository: FileRepository,
    private val favoritesRepository: FavoritesRepository,
    private val documentLockRepository: DocumentLockRepository,
    private val settingsRepository: SettingsRepository,
    private val pileModelRepository: PileModelRepository,
    private val moveDocumentToTrashUseCase: MoveDocumentToTrashUseCase,
    private val getPdfUriUseCase: GetPdfUriUseCase,
    private val exportDocumentUseCase: ExportDocumentUseCase,
    private val exportDocumentImagesUseCase: ExportDocumentImagesUseCase,
    private val documentOpener: DocumentOpener
) : ViewModel() {
    private var _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    val bitmapCache = bitmapCacheRepository.bitmapCache

    private val _navigationEvent = Channel<DocumentModel>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private var pendingImportAction: (() -> Unit)? = null

    private var backupUnsavedDocument: TemporaryDocumentBackup? = null

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
                val hubPictureFiles = settings.hubPicturePaths
                    .mapValues { (_, path) -> fileRepository.getProfilePictureFile(path) }
                    .filterValues { file -> file.exists() }

                _state.update {
                    it.copy(
                        viewMode = settings.documentViewMode,
                        hubPictureFiles = hubPictureFiles
                    )
                }
            }
        }
        viewModelScope.launch {
            getHomeDataUseCase().collect { homeData ->
                val documentCoverItems = homeData.documents.map { documentModel ->
                    DocumentCoverItem(
                        document = documentModel,
                        coverImageCacheKey = bitmapCacheRepository.getCoverKey(documentModel)
                    )
                }

                _state.update {
                    it.copy(
                        pileModels = homeData.piles,
                        documentCoverItems = documentCoverItems,
                        temporaryDocument = homeData.temporaryDocument,
                        coloredPileIds = homeData.coloredPileIds,
                        pileDocumentCounts = homeData.pileDocumentCounts,
                        isInitialLoading = false
                    )
                }

                val documentSizes = getDocumentSizesUseCase(homeData.documents)
                _state.update { it.copy(documentSizes = documentSizes) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        purgeDraftDocument()
    }

    fun handleEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OnFavoriteToggled -> viewModelScope.launch {
                favoritesRepository.setFavorite(
                    event.documentId,
                    event.documentId !in state.value.favoriteDocumentIds
                )
            }
            is HomeEvent.OnImageDisplayed -> requestCoverThumbnail(event.document)

            is HomeEvent.OnRemoveDraftDocument -> removeDraftDocument()
            HomeEvent.OnRestoreDraftDocument -> restoreDraftDocument()
            HomeEvent.OnPurgeDraftDocument -> purgeDraftDocument()

            is HomeEvent.OnCreatePile -> addPile(event.pileName, event.iconId, event.color)

            is HomeEvent.OnPilesReordered -> reorderPiles(event.orderedPileIds)

            is HomeEvent.OnPdfImported -> {
                if (state.value.temporaryDocument != null) {
                    pendingImportAction = { importPDFIntent(event.uri) }
                    _state.update { it.copy(showDraftWarning = true) }
                } else {
                    importPDFIntent(event.uri)
                }
            }

            is HomeEvent.OnImagesImported -> requestImageImport(event.uris)

            is HomeEvent.OnImageCompressionConfirmed -> confirmImageImport(event.choice)

            HomeEvent.OnImageCompressionDismissed ->
                _state.update { it.copy(pendingImageImport = null) }

            HomeEvent.OnCameraClick -> {
                if (state.value.temporaryDocument != null) {
                    pendingImportAction = { createCameraUri() }
                    _state.update { it.copy(showDraftWarning = true) }
                } else {
                    createCameraUri()
                }
            }

            HomeEvent.OnCameraUriConsumed -> dismissCameraUri()

            HomeEvent.OnScannerUnavailable -> _state.update {
                it.copy(errorMessage = UiText.StringResource(R.string.scanner_unavailable))
            }

            HomeEvent.OnConfirmImport -> {
                _state.update { it.copy(showDraftWarning = false) }
                pendingImportAction?.invoke()
                pendingImportAction = null
            }

            HomeEvent.OnDismissDraftWarning -> {
                _state.update { it.copy(showDraftWarning = false) }
                pendingImportAction = null
            }

            is HomeEvent.OnSortOrderChanged -> {
                _state.update { it.copy(sortOrder = event.sortOrder) }
            }

            is HomeEvent.OnViewModeChanged -> viewModelScope.launch {
                settingsRepository.updateDocumentViewMode(event.viewMode)
            }

            // Multi selection actions
            is HomeEvent.OnDocumentLongPressed -> {
                val alreadySelected = event.documentId in state.value.selectedDocumentIds

                _state.update {
                    it.copy(
                        selectedDocumentIds = if (alreadySelected) {
                            it.selectedDocumentIds - event.documentId
                        } else {
                            it.selectedDocumentIds + event.documentId
                        }
                    )
                }
            }

            is HomeEvent.OnDocumentSelectionToggled -> {
                _state.update {
                    it.copy(
                        selectedDocumentIds = if (event.documentId in it.selectedDocumentIds) {
                            it.selectedDocumentIds - event.documentId
                        } else {
                            it.selectedDocumentIds + event.documentId
                        }
                    )
                }
            }

            HomeEvent.OnSelectionCleared -> clearSelection()

            is HomeEvent.OnExportSelectedClicked -> exportSelectedDocuments(
                event.format,
                event.destinationFolderUri
            )

            HomeEvent.OnShareSelectedClicked -> shareSelectedDocuments()

            HomeEvent.OnDeleteSelectedClicked -> deleteSelectedDocuments()

            HomeEvent.OnSelectedDocumentsDeleted -> clearSelection()

            HomeEvent.OnErrorDismissed -> _state.update { it.copy(errorMessage = null) }
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
     * Exports every selected document with the [format] chosen by the user.
     *
     * @param destinationFolderUri Folder the user granted access to the first
     * time something was exported, or null to use the Downloads directory.
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

                Napier.d { "Exported ${exportedCount}/${documents.size} documents" }
            }

            val destinationIsFolder = destinationFolderUri != null

            _state.update {
                it.copy(
                    isSelectionWorking = false,
                    selectedDocumentIds = emptySet(),
                    errorMessage = when {
                        exportedCount == 0 ->
                            UiText.StringResource(R.string.error_exporting_documents)

                        destinationIsFolder -> UiText.StringResource(
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

    /**
     * Opens the system share sheet with a PDF file of every selected document.
     */
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

    /**
     * Moves every selected document into the Recycle Bin.
     */
    private fun deleteSelectedDocuments() {
        viewModelScope.launch {
            val documents = selectedDocuments()
            if (documents.isEmpty()) return@launch

            documents.forEach { document ->
                moveDocumentToTrashUseCase(document)
            }

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

    private fun requestCoverThumbnail(document: DocumentModel) {
        viewModelScope.launch {
            requestCoverThumbnailUseCase(document)
        }
    }

    private fun addPile(pileName: String, iconId: String, color: Long) {
        viewModelScope.launch {
            createPileUseCase(pileName, iconId, color)
        }
    }

    /**
     * Persists the manual order of the hubs after the user long pressed and
     * dragged them on the home screen.
     */
    private fun reorderPiles(orderedPileIds: List<String>) {
        viewModelScope.launch {
            pileModelRepository.updatePilePositions(orderedPileIds)
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

    private fun importPDFIntent(uri: Uri) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoadingNewDocument = true) }
                val newDoc = createDocumentUseCase.createFromPdf(uri)
                _navigationEvent.send(newDoc)
            } catch (e: Exception) {
                Napier.e("Error importing PDF", e)
                _state.update {
                    it.copy(errorMessage = UiText.StringResource(R.string.error_importing_pdf))
                }
            } finally {
                _state.update { it.copy(isLoadingNewDocument = false) }
            }
        }
    }

    /**
     * First step of an image import: instead of importing right away, the user is
     * asked whether the images should be compressed (and to which size). The
     * pre-selected answer follows the Document Resizer settings.
     */
    private fun requestImageImport(uriList: List<Uri>) {
        viewModelScope.launch {
            val settings = settingsRepository.userSettings.first()

            _state.update {
                it.copy(
                    pendingImageImport = PendingImageImport(
                        uris = uriList,
                        defaultChoice = ImageCompressionChoice(
                            compress = settings.isDocumentResizerEnabled,
                            targetSizeKb = settings.documentResizerTargetSizeKb
                        )
                    )
                )
            }
        }
    }

    /** Second step: the compression prompt was answered, the import continues. */
    private fun confirmImageImport(choice: ImageCompressionChoice) {
        val pending = state.value.pendingImageImport ?: return
        _state.update { it.copy(pendingImageImport = null) }
        rememberResizerChoice(choice)

        if (state.value.temporaryDocument != null) {
            pendingImportAction = { importImagesIntent(pending.uris, choice) }
            _state.update { it.copy(showDraftWarning = true) }
        } else {
            importImagesIntent(pending.uris, choice)
        }
    }

    /**
     * The ON/OFF switch of the Document Resizer prompt is also remembered as the
     * new default, so the answer given here pre-selects the next import and stays
     * in sync with Settings -> Document Resizer.
     */
    private fun rememberResizerChoice(choice: ImageCompressionChoice) {
        viewModelScope.launch {
            settingsRepository.updateDocumentResizerEnabled(choice.compress)

            if (choice.compress) {
                settingsRepository.updateDocumentResizerTargetSizeKb(choice.targetSizeKb)
            }
        }
    }

    private fun importImagesIntent(uriList: List<Uri>, compression: ImageCompressionChoice? = null) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoadingNewDocument = true) }
                val newDoc = createDocumentUseCase.createFromImages(uriList, compression = compression)
                _navigationEvent.send(newDoc)
            } catch (e: Exception) {
                Napier.e("Error importing images", e)
                _state.update {
                    it.copy(errorMessage = UiText.StringResource(R.string.error_importing_images))
                }
            } finally {
                _state.update { it.copy(isLoadingNewDocument = false) }
            }
        }
    }

    private fun removeDraftDocument() {
        viewModelScope.launch {
            backupUnsavedDocument = manageTemporaryDocumentUseCase.deleteForUndo()
        }
    }

    private fun restoreDraftDocument() {
        val backup = backupUnsavedDocument ?: return

        viewModelScope.launch {
            try {
                backupUnsavedDocument = null
                manageTemporaryDocumentUseCase.restoreBackup(backup)
            } catch (e: Exception) {
                Napier.e { "Error restoring backup. Message: ${e.message}" }
                _state.update {
                    it.copy(
                        errorMessage = UiText.StringResource(R.string.error_restoring_draft_document)
                    )
                }
            }
        }
    }

    private fun purgeDraftDocument() {
        val backup = backupUnsavedDocument ?: return
        val documentId = backup.document.id

        backupUnsavedDocument = null

        cleanupScheduler.scheduleDocumentDeletion(documentId)
    }
}
