package es.pile.features.pileDetail.ui

import android.graphics.Bitmap
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pile.DocumentModel
import es.pile.PileModel
import es.pile.R
import es.pile.core.domain.models.DocumentCoverItem
import es.pile.core.domain.models.DocumentSortOrder
import es.pile.core.ui.composables.AlertDraftDocumentWarning
import es.pile.core.ui.composables.CompressImagesDialog
import es.pile.core.ui.composables.AlertEditPile
import es.pile.core.ui.composables.DocumentSelectionTopBar
import es.pile.core.ui.composables.DocumentSortMenu
import es.pile.core.ui.composables.LoadingAlert
import es.pile.core.ui.composables.LoadingWrapper
import es.pile.core.ui.composables.PictureAvatar
import es.pile.core.ui.composables.itemDocumentsVerticalList
import es.pile.core.ui.controllers.rememberDocumentImportController
import es.pile.core.ui.controllers.rememberExportDestinationController
import es.pile.core.ui.theme.ExtendedTheme
import es.pile.core.ui.theme.PileTheme
import es.pile.features.home.ui.FabMenuAction
import es.pile.features.home.ui.FabMenuWithController
import es.pile.features.settings.ui.composables.ItemPosition
import es.pile.features.settings.ui.composables.SettingsItem
import java.io.File
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PileDetailScreen(
    modifier: Modifier = Modifier,
    pileId: String,
    navigateToDocumentDetail: (documentId: String) -> Unit,
    navigateToSearchScreen: (pileId: String) -> Unit,
    navigateToEditDocument: (documentId: String) -> Unit,
    navigateToAddDocument: (documentId: String) -> Unit,
    popBackStack: () -> Unit,
    popToHome: () -> Unit,
    viewModel: PileDetailViewModel = koinViewModel { parametersOf(pileId) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bitmapCache by viewModel.bitmapCache.collectAsStateWithLifecycle()

    LaunchedEffect(state.isLoading) {
        if (!state.isLoading && state.pile == null) {
            popBackStack()
        }
    }

    var isNavigating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collectLatest { document ->
            isNavigating = true
            if (document.isIncomingPdf) navigateToAddDocument(document.id)
            else navigateToEditDocument(document.id)
        }
    }

    PileDetailContent(
        modifier = modifier,
        state = state,
        bitmapCache = bitmapCache,
        isNavigating = isNavigating,
        onEvent = { viewModel.handleEvent(it) },
        popBackStack = popBackStack,
        popToHome = popToHome,
        navigateToDocumentDetail = navigateToDocumentDetail,
        navigateToSearchScreen = { navigateToSearchScreen(pileId) },
        navigateToAddDocument = navigateToAddDocument,
        navigateToEditDocument = navigateToEditDocument
    )
}

@Preview
@Composable
private fun PileDetailPrev() {
    PileTheme {
        PileDetailContent(
            state = PileDetailState(
                pile = PileModel(
                    id = "1",
                    name = "Sample Pile",
                    iconId = "Bank",
                    colorNumber = 1L,
                    position = 0
                ),
                isLoading = false,
                documentCoverItems = listOf(
                    DocumentCoverItem(
                        document = DocumentModel(
                            id = "doc1",
                            title = "Document 1",
                            imageIds = emptyList(),
                            creationDateTime = LocalDateTime.now(),
                            modificationDateTime = LocalDateTime.now(),
                            documentStatus = 1,
                            documentPileIds = listOf("1"),
                            documentDetails = emptyList(),
                            documentNote = "",
                            documentOrganizationIds = emptyList(),
                            isIncomingPdf = false
                        ),
                        coverImageCacheKey = "key1"
                    )
                )
            ),
            bitmapCache = emptyMap(),
            onEvent = {},
            popBackStack = {},
            popToHome = {},
            navigateToDocumentDetail = {},
            navigateToSearchScreen = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PileDetailContent(
    modifier: Modifier = Modifier,
    state: PileDetailState,
    bitmapCache: Map<String, Bitmap>,
    isNavigating: Boolean = false,
    onEvent: (PileDetailEvent) -> Unit,
    popBackStack: () -> Unit,
    popToHome: () -> Unit,
    navigateToDocumentDetail: (documentId: String) -> Unit,
    navigateToSearchScreen: () -> Unit,
    navigateToAddDocument: (documentId: String) -> Unit = {},
    navigateToEditDocument: (documentId: String) -> Unit = {},
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it.asString(context),
                duration = SnackbarDuration.Short
            )
            onEvent(PileDetailEvent.OnErrorDismissed)
        }
    }

    val sortedDocuments = remember(state.documentCoverItems, state.sortOrder) {
        state.documentCoverItems.sortedWith(
            when (state.sortOrder) {
                DocumentSortOrder.NEWEST ->
                    compareByDescending<DocumentCoverItem> { it.document.creationDateTime }
                DocumentSortOrder.OLDEST ->
                    compareBy<DocumentCoverItem> { it.document.creationDateTime }
            }
        )
    }

    var isUpdatePileExpanded by rememberSaveable { mutableStateOf(false) }
    var isDeletePileExpanded by rememberSaveable { mutableStateOf(false) }
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showHubPictureOptions by rememberSaveable { mutableStateOf(false) }
    var showDeleteSelectionAlert by rememberSaveable { mutableStateOf(false) }

    val importActions = rememberDocumentImportController(
        cameraUri = state.cameraUri,
        onUriConsumed = { onEvent(PileDetailEvent.OnCameraUriConsumed) },
        onPdfSelected = { onEvent(PileDetailEvent.OnPdfImported(it)) },
        onImagesSelected = { onEvent(PileDetailEvent.OnImagesImported(it)) },
        onCameraClick = { onEvent(PileDetailEvent.OnCameraClick) },
        onScannerError = { onEvent(PileDetailEvent.OnScannerUnavailable) }
    )

    // Export: the format is picked first and the folder is only requested once.
    val exportActions = rememberExportDestinationController { format, folderUri ->
        onEvent(PileDetailEvent.OnExportSelectedClicked(format, folderUri))
    }

    val hubPictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onEvent(PileDetailEvent.OnHubPicturePicked(it)) }
    }

    val launchHubPicturePicker = {
        hubPictureLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // Tapping a document toggles its selection while the selection mode is on.
    val onDocumentClicked: (String) -> Unit = { documentId ->
        if (state.isSelectionMode) {
            onEvent(PileDetailEvent.OnDocumentSelectionToggled(documentId))
        } else {
            navigateToDocumentDetail(documentId)
        }
    }

    BackHandler(enabled = state.isSelectionMode) {
        onEvent(PileDetailEvent.OnSelectionCleared)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.displayCutout,
        modifier = modifier,
        topBar = {
            if (state.isSelectionMode) {
                DocumentSelectionTopBar(
                    selectedCount = state.selectedDocumentIds.size,
                    enabled = !state.isSelectionWorking,
                    onClose = { onEvent(PileDetailEvent.OnSelectionCleared) },
                    onExportFormatSelected = { format -> exportActions.requestExport(format) },
                    onShare = { onEvent(PileDetailEvent.OnShareSelectedClicked) },
                    onDelete = { showDeleteSelectionAlert = true }
                )
            } else {
                state.pile?.let { pileModel ->
                    TopAppBar(
                        pileModel = pileModel,
                        documentCount = state.documentCoverItems.size,
                        hubPictureFile = state.hubPictureFile,
                        popBackStack = popBackStack,
                        onSearchClick = navigateToSearchScreen,
                        onPictureClick = { showHubPictureOptions = true },
                        onEditClick = { isUpdatePileExpanded = true },
                        onDeleteClick = { isDeletePileExpanded = true }
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            AnimatedVisibility(
                visible = !state.isLoading && !state.isSelectionMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                // The "+" button keeps every import action and adds the option to
                // upload the profile picture of the person this hub belongs to.
                val hubPictureAction = FabMenuAction(
                    icon = rememberVectorPainter(Icons.Filled.PhotoLibrary),
                    label = stringResource(R.string.upload_hub_picture),
                    action = { showHubPictureOptions = true }
                )

                FabMenuWithController(
                    modifier = Modifier.padding(WindowInsets.navigationBars.asPaddingValues()),
                    fabMenuExpanded = fabMenuExpanded,
                    updateFabMenuExpanded = { fabMenuExpanded = it },
                    importActions = importActions,
                    extraActions = listOf(hubPictureAction)
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LoadingWrapper(
                isLoading = state.isLoading,
                modifier = Modifier.pointerInteropFilter {
                    when (it.action) {
                        MotionEvent.ACTION_DOWN -> {
                            fabMenuExpanded = false
                        }
                    }
                    false
                }
            ) {
                if (state.documentCoverItems.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(32.dp)
                                .padding(top = 100.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.user_ic_category_24px),
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.no_documents_in_pile),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.all_documents),
                                    style = MaterialTheme.typography.titleLarge
                                )

                                DocumentSortMenu(
                                    sortOrder = state.sortOrder,
                                    onSortOrderChange = {
                                        onEvent(PileDetailEvent.OnSortOrderChanged(it))
                                    }
                                )
                            }
                        }

                        itemDocumentsVerticalList(
                            documents = sortedDocuments,
                            documentSizes = state.documentSizes,
                            bitmapCache = bitmapCache,
                            onLoadBitmap = { onEvent(PileDetailEvent.OnImageDisplayed(it)) },
                            lockedDocumentIds = state.lockedDocumentIds,
                            selectedDocumentIds = state.selectedDocumentIds,
                            onDocumentClick = onDocumentClicked,
                            onDocumentLongPress = { documentId ->
                                onEvent(PileDetailEvent.OnDocumentLongPressed(documentId))
                            },
                            favoriteDocumentIds = state.favoriteDocumentIds,
                            onFavoriteToggle = { onEvent(PileDetailEvent.OnFavoriteToggled(it)) }
                        )

                        item {
                            Spacer(Modifier.height(100.dp))
                        }
                    }
                }
            }

            if (state.isLoadingNewDocument || isNavigating) {
                LoadingAlert(title = stringResource(R.string.loading_new_document))
            }
        }

        if (state.isSelectionWorking) {
            LoadingAlert(title = stringResource(R.string.preparing_documents))
        }

        if (state.isWorkingOnHubPicture) {
            LoadingAlert(title = stringResource(R.string.saving_hub_picture))
        }

        if (showDeleteSelectionAlert) {
            AlertDialog(
                onDismissRequest = { showDeleteSelectionAlert = false },
                title = {
                    Text(
                        stringResource(
                            R.string.delete_documents_alert_title,
                            state.selectedDocumentIds.size
                        )
                    )
                },
                text = { Text(stringResource(R.string.delete_documents_alert_body)) },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteSelectionAlert = false
                        onEvent(PileDetailEvent.OnDeleteSelectedClicked)
                    }) {
                        Text(
                            stringResource(R.string.delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteSelectionAlert = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (showHubPictureOptions) {
            HubPictureOptionsDialog(
                hasPicture = state.hubPictureFile != null,
                onDismiss = { showHubPictureOptions = false },
                onChooseFromGallery = {
                    showHubPictureOptions = false
                    launchHubPicturePicker()
                },
                onRemove = {
                    showHubPictureOptions = false
                    onEvent(PileDetailEvent.OnHubPictureRemoved)
                }
            )
        }

        if (isUpdatePileExpanded) {
            state.pile?.let { pile ->
                AlertEditPile(
                    pileModel = pile,
                    onDismiss = { isUpdatePileExpanded = false },
                    onConfirm = { pileName, pileIconId, colorNumber ->
                        isUpdatePileExpanded = false
                        onEvent(
                            PileDetailEvent.OnPileChange(
                                pileName,
                                pileIconId,
                                colorNumber
                            )
                        )
                    }
                )
            }
        }

        if (isDeletePileExpanded) {
            AlertDeletePile(
                onDismiss = { isDeletePileExpanded = false },
                onConfirm = {
                    isDeletePileExpanded = false
                    onEvent(PileDetailEvent.OnDeletePile)
                    popToHome()
                }
            )
        }

        if (state.showDraftWarning) {
            val tempDocument = state.temporaryDocument
            AlertDraftDocumentWarning(
                onDismiss = { onEvent(PileDetailEvent.OnDismissDraftWarning) },
                onDiscardAndContinue = {
                    onEvent(PileDetailEvent.OnConfirmImport)
                },
                onNavigateToDraft = {
                    onEvent(PileDetailEvent.OnDismissDraftWarning)
                    if (tempDocument != null) {
                        if (tempDocument.isIncomingPdf) navigateToAddDocument(tempDocument.id)
                        else navigateToEditDocument(tempDocument.id)
                    }
                }
            )
        }

        // Ask whether the picked images should be compressed before importing them.
        state.pendingImageImport?.let { pending ->
            CompressImagesDialog(
                imageCount = pending.uris.size,
                defaultChoice = pending.defaultChoice,
                onDismiss = { onEvent(PileDetailEvent.OnImageCompressionDismissed) },
                onConfirm = { choice ->
                    onEvent(PileDetailEvent.OnImageCompressionConfirmed(choice))
                }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
private fun TopAppBar(
    modifier: Modifier = Modifier,
    pileModel: PileModel,
    documentCount: Int,
    hubPictureFile: File? = null,
    popBackStack: () -> Unit,
    onSearchClick: () -> Unit,
    onPictureClick: () -> Unit = {},
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val colorIndex = pileModel.colorNumber?.toInt()
    val colorFamily = ExtendedTheme.colors.customColorList.getOrNull(colorIndex ?: -1)

    val containerColor = colorFamily?.colorContainer
        ?: MaterialTheme.colorScheme.primaryContainer
    val contentColor = colorFamily?.onColorContainer
        ?: MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .statusBarsPadding()
            .displayCutoutPadding()
            .padding(bottom = 14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = popBackStack) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back_24px),
                    contentDescription = stringResource(R.string.return_),
                    tint = contentColor
                )
            }

            PictureAvatar(
                pictureFile = hubPictureFile,
                contentDescription = stringResource(R.string.hub_picture),
                size = 40.dp,
                containerColor = contentColor.copy(alpha = 0.14f),
                contentColor = contentColor,
                onClick = onPictureClick,
                modifier = Modifier.padding(end = 10.dp)
            )

            Column(Modifier.weight(1f)) {
                Text(
                    text = pileModel.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.pile_document_count, documentCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }

            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.search),
                    tint = contentColor
                )
            }
            IconButton(onClick = onEditClick) {
                Icon(
                    painter = painterResource(R.drawable.edit_24px),
                    contentDescription = stringResource(R.string.edit_pile),
                    tint = contentColor
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    painter = painterResource(R.drawable.delete_24px),
                    contentDescription = stringResource(R.string.delete_pile),
                    tint = contentColor
                )
            }
        }
    }
}

@Composable
private fun AlertDeletePile(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        icon = {
            Icon(
                painter = painterResource(R.drawable.warning_24px),
                contentDescription = null
            )
        },
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_pile_alert_title)) },

        text = {
            Text(stringResource(R.string.delete_pile_alert_body))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Options shown when uploading or removing the profile picture of a hub.
 */
@Composable
private fun HubPictureOptionsDialog(
    hasPicture: Boolean,
    onDismiss: () -> Unit,
    onChooseFromGallery: () -> Unit,
    onRemove: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.hub_picture)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.hub_picture_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                SettingsItem(
                    itemPosition = if (hasPicture) ItemPosition.TOP else ItemPosition.SINGLE,
                    title = stringResource(R.string.choose_from_gallery),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onAction = onChooseFromGallery
                )

                if (hasPicture) {
                    SettingsItem(
                        itemPosition = ItemPosition.BOTTOM,
                        title = stringResource(R.string.remove_hub_picture),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onAction = onRemove
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
