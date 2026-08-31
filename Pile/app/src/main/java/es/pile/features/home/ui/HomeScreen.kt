package es.pile.features.home.ui

import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pile.R
import es.pile.core.domain.models.DocumentCoverItem
import es.pile.core.domain.models.DocumentSortOrder
import es.pile.core.domain.models.DocumentViewMode
import es.pile.core.ui.composables.AlertDraftDocumentWarning
import es.pile.core.ui.composables.AlertNewPile
import es.pile.core.ui.composables.DocumentViewToggle
import es.pile.core.ui.composables.LoadingAlert
import es.pile.core.ui.composables.LoadingWrapper
import es.pile.core.ui.composables.DocumentSortMenu
import es.pile.core.ui.composables.PileCardsRow
import es.pile.core.ui.composables.SwipeBox
import es.pile.core.ui.composables.itemDocumentsIconGrid
import es.pile.core.ui.composables.itemDocumentsVerticalList
import es.pile.core.ui.controllers.ImportActions
import es.pile.core.ui.controllers.rememberDocumentImportController
import es.pile.features.home.ui.compostables.HomeScreenSectionTitle
import es.pile.features.search.ui.SearchContent
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navigateToPileDetail: (pileId: String) -> Unit,
    navigateToDocumentDetail: (documentId: String) -> Unit,
    navigateToEditDocument: (documentId: String) -> Unit,
    navigateToAddDocument: (documentId: String) -> Unit,
    navigateToSettings: () -> Unit,
    navigateToFavorites: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bitmapCache by viewModel.bitmapCache.collectAsStateWithLifecycle()

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

    var isNavigating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { document ->
            isNavigating = true
            if (document.isIncomingPdf) navigateToAddDocument(document.id)
            else navigateToEditDocument(document.id)
        }
    }

    val listState = rememberLazyListState()

    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var isNewPileAlertExpanded by rememberSaveable { mutableStateOf(false) }

    val importActions = rememberDocumentImportController(
        cameraUri = state.cameraUri,
        onUriConsumed = { viewModel.handleEvent(HomeEvent.OnCameraUriConsumed) },
        onPdfSelected = { viewModel.handleEvent(HomeEvent.OnPdfImported(it)) },
        onImagesSelected = { viewModel.handleEvent(HomeEvent.OnImagesImported(it)) },
        onCameraClick = { viewModel.handleEvent(HomeEvent.OnCameraClick) }
    )

    var isSearchBarExpanded by rememberSaveable { mutableStateOf(false) }
    var showDeleteSelectionAlert by rememberSaveable { mutableStateOf(false) }

    // While documents are selected, tapping one toggles its selection instead of
    // navigating to the document detail.
    val onDocumentClicked: (String) -> Unit = { documentId ->
        if (state.isSelectionMode) {
            viewModel.handleEvent(HomeEvent.OnDocumentSelectionToggled(documentId))
        } else {
            navigateToDocumentDetail(documentId)
        }
    }

    val onDocumentLongPressed: (String) -> Unit = { documentId ->
        isSearchBarExpanded = false
        viewModel.handleEvent(HomeEvent.OnDocumentLongPressed(documentId))
    }

    BackHandler(enabled = state.isSelectionMode) {
        viewModel.handleEvent(HomeEvent.OnSelectionCleared)
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarStrings = Pair(
        stringResource(R.string.document_unsaved_changes_deleted),
        stringResource(R.string.undo)
    )

    val context = LocalContext.current

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { uiText ->
            snackbarHostState.showSnackbar(
                message = uiText.asString(context),
                duration = SnackbarDuration.Short
            )
            viewModel.handleEvent(HomeEvent.OnErrorDismissed)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.displayCutout,
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            AnimatedVisibility(
                !isSearchBarExpanded && !state.isSelectionMode,
                enter = fadeIn(), exit = fadeOut()
            ) {
                FabMenuWithController(
                    modifier = Modifier.padding(WindowInsets.navigationBars.asPaddingValues()),
                    fabMenuExpanded = fabMenuExpanded,
                    updateFabMenuExpanded = { fabMenuExpanded = it },
                    importActions = importActions
                )
            }
        },
        topBar = {
            if (state.isSelectionMode) {
                SelectionTopBar(
                    selectedCount = state.selectedDocumentIds.size,
                    enabled = !state.isSelectionWorking,
                    onClose = { viewModel.handleEvent(HomeEvent.OnSelectionCleared) },
                    onExport = { viewModel.handleEvent(HomeEvent.OnExportSelectedClicked) },
                    onShare = { viewModel.handleEvent(HomeEvent.OnShareSelectedClicked) },
                    onDelete = { showDeleteSelectionAlert = true }
                )
            } else {
                val horizontalPaddingAnimated by animateDpAsState(
                    targetValue = if (isSearchBarExpanded) 0.dp else 16.dp,
                )
                val bottomPaddingAnimated by animateDpAsState(
                    targetValue = if (isSearchBarExpanded) 0.dp else 8.dp,
                )
                val displayCutoutStartPaddingAnimated by animateDpAsState(
                    targetValue = if (isSearchBarExpanded) 0.dp else WindowInsets.displayCutout.asPaddingValues()
                        .calculateStartPadding(LocalLayoutDirection.current)
                )
                val displayCutoutEndPaddingAnimated by animateDpAsState(
                    targetValue = if (isSearchBarExpanded) 0.dp else WindowInsets.displayCutout.asPaddingValues()
                        .calculateEndPadding(LocalLayoutDirection.current)
                )

                SearchContent(
                    modifier = Modifier
                        .padding(horizontal = horizontalPaddingAnimated)
                        .padding(bottom = bottomPaddingAnimated)
                        .padding(start = displayCutoutStartPaddingAnimated)
                        .padding(end = displayCutoutEndPaddingAnimated),
                    expanded = isSearchBarExpanded,
                    onExpandedChange = { isSearchBarExpanded = it },
                    onSettingsClick = navigateToSettings,
                    navigateToDocumentDetail = navigateToDocumentDetail
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { innerPadding ->
        val backgroundDocuments = MaterialTheme.colorScheme.surface

        val layoutDirection = LocalLayoutDirection.current


        LoadingWrapper(state.isInitialLoading) {
            Box(
                Modifier
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        start = innerPadding.calculateStartPadding(layoutDirection),
                        end = innerPadding.calculateEndPadding(layoutDirection)
                    )
                    .fillMaxSize()
                    .background(backgroundDocuments)
                    .pointerInteropFilter {
                        when (it.action) {
                            MotionEvent.ACTION_DOWN -> {
                                fabMenuExpanded = false
                            }
                        }
                        false
                    }
            ) {
                BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    val availableWidth = maxWidth

                    LazyColumn(
                        Modifier
                            .padding(bottom = innerPadding.calculateBottomPadding())
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .pointerInteropFilter {
                                when (it.action) {
                                    MotionEvent.ACTION_DOWN -> {
                                        fabMenuExpanded = false
                                    }
                                }
                                false
                            },
                        state = listState
                    ) {
                    item { Spacer(Modifier.height(8.dp)) }

                    item {
                        val tempDocument = state.temporaryDocument
                        AnimatedVisibility(
                            visible = tempDocument != null,
                            enter = fadeIn(tween(100)) + expandVertically(),
                            exit = fadeOut(tween(100)) + shrinkVertically()
                        ) {
                            UnsavedDocumentCard(
                                onNavigateUnsavedDocument = {
                                    if (tempDocument == null) return@UnsavedDocumentCard

                                    if (tempDocument.isIncomingPdf)
                                        navigateToAddDocument(tempDocument.id)
                                    else
                                        navigateToEditDocument(tempDocument.id)
                                },
                                onDismiss = {
                                    viewModel.handleEvent(HomeEvent.OnRemoveDraftDocument)

                                    scope.launch {
                                        val result = snackbarHostState
                                            .showSnackbar(
                                                message = snackbarStrings.first,
                                                actionLabel = snackbarStrings.second,
                                                duration = SnackbarDuration.Long
                                            )
                                        when (result) {
                                            SnackbarResult.ActionPerformed -> { // restore
                                                viewModel.handleEvent(HomeEvent.OnRestoreDraftDocument)
                                            }

                                            SnackbarResult.Dismissed -> {
                                                viewModel.handleEvent(HomeEvent.OnPurgeDraftDocument)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }

                    item {
                        HomeScreenSectionTitle(
                            title = stringResource(R.string.your_piles),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }

                    item {
                        PileCardsRow(
                            piles = state.pileModels,
                            pileDocumentCounts = state.pileDocumentCounts,
                            onPileClick = navigateToPileDetail,
                            onNewPileClick = { isNewPileAlertExpanded = true }
                        )
                    }

                    item { Spacer(Modifier.height(30.dp)) }

                    item {
                        Column(
                            Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 24.dp,
                                        topEnd = 24.dp
                                    )
                                )
                                .background(backgroundDocuments)
                                .padding(top = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.all_documents),
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.weight(1f)
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = navigateToFavorites) {
                                        Icon(
                                            imageVector = if (state.favoriteDocumentIds.isEmpty()) {
                                                Icons.Outlined.StarBorder
                                            } else {
                                                Icons.Filled.Star
                                            },
                                            contentDescription = stringResource(R.string.favorites),
                                            tint = if (state.favoriteDocumentIds.isEmpty()) {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            }
                                        )
                                    }

                                    DocumentViewToggle(
                                        viewMode = state.viewMode,
                                        onViewModeChange = {
                                            viewModel.handleEvent(
                                                HomeEvent.OnViewModeChanged(it)
                                            )
                                        }
                                    )

                                    Spacer(Modifier.width(8.dp))

                                    DocumentSortMenu(
                                        sortOrder = state.sortOrder,
                                        onSortOrderChange = {
                                            viewModel.handleEvent(
                                                HomeEvent.OnSortOrderChanged(it)
                                            )
                                        }
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                    val showEmptyDocuments = state.documentCoverItems.isEmpty()

                    if (showEmptyDocuments) {
                        item {
                            HomeEmptyState(
                                icon = painterResource(R.drawable.ic_clip),
                                text = stringResource(R.string.no_documents_home),
                                modifier = Modifier.background(backgroundDocuments)
                            )
                        }
                    } else {
                        when (state.viewMode) {
                            DocumentViewMode.LIST -> itemDocumentsVerticalList(
                                backgroundColor = backgroundDocuments,
                                documents = sortedDocuments,
                                documentSizes = state.documentSizes,
                                bitmapCache = bitmapCache,
                                onLoadBitmap = {
                                    viewModel.handleEvent(HomeEvent.OnImageDisplayed(it))
                                },
                                lockedDocumentIds = state.lockedDocumentIds,
                                selectedDocumentIds = state.selectedDocumentIds,
                                onDocumentClick = onDocumentClicked,
                                onDocumentLongPress = onDocumentLongPressed,
                                favoriteDocumentIds = state.favoriteDocumentIds,
                                onFavoriteToggle = {
                                    viewModel.handleEvent(HomeEvent.OnFavoriteToggled(it))
                                }
                            )

                            DocumentViewMode.GRID -> itemDocumentsIconGrid(
                                availableWidth = availableWidth,
                                backgroundColor = backgroundDocuments,
                                documents = sortedDocuments,
                                bitmapCache = bitmapCache,
                                onLoadBitmap = {
                                    viewModel.handleEvent(HomeEvent.OnImageDisplayed(it))
                                },
                                lockedDocumentIds = state.lockedDocumentIds,
                                selectedDocumentIds = state.selectedDocumentIds,
                                onDocumentClick = onDocumentClicked,
                                onDocumentLongPress = onDocumentLongPressed,
                                favoriteDocumentIds = state.favoriteDocumentIds,
                                onFavoriteToggle = {
                                    viewModel.handleEvent(HomeEvent.OnFavoriteToggled(it))
                                }
                            )
                        }
                    }

                    item {
                        Box(
                            Modifier
                                .height(100.dp)
                                .fillMaxWidth()
                                .background(backgroundDocuments)
                        )
                    }
                    }
                }
            }
        }
    }

    if (state.isSelectionWorking) {
        LoadingAlert(stringResource(R.string.preparing_documents))
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
                    viewModel.handleEvent(HomeEvent.OnDeleteSelectedClicked)
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

    if (isNewPileAlertExpanded) {
        AlertNewPile(
            onDismiss = { isNewPileAlertExpanded = false },
            onConfirm = { pileName, pileIconId, pileColorNumber ->
                isNewPileAlertExpanded = false
                viewModel.handleEvent(HomeEvent.OnCreatePile(pileName, pileIconId, pileColorNumber))
            }
        )
    }

    if (state.showDraftWarning) {
        val tempDocument = state.temporaryDocument
        AlertDraftDocumentWarning(
            onDismiss = { viewModel.handleEvent(HomeEvent.OnDismissDraftWarning) },
            onDiscardAndContinue = {
                viewModel.handleEvent(HomeEvent.OnConfirmImport)
            },
            onNavigateToDraft = {
                viewModel.handleEvent(HomeEvent.OnDismissDraftWarning)
                if (tempDocument != null) {
                    if (tempDocument.isIncomingPdf)
                        navigateToAddDocument(tempDocument.id)
                    else
                        navigateToEditDocument(tempDocument.id)
                }
            }
        )
    }

    if (state.isLoadingNewDocument || isNavigating) {
        LoadingAlert(stringResource(R.string.loading_new_document))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FabMenuWithController(
    modifier: Modifier = Modifier,
    fabMenuExpanded: Boolean,
    updateFabMenuExpanded: (Boolean) -> Unit = {},
    importActions: ImportActions
) {
    val items = listOf(
        Triple(
            painterResource(R.drawable.ic_clip),
            stringResource(R.string.import_pdf_file),
            importActions.launchPdfPicker
        ),
        Triple(
            rememberVectorPainter(Icons.Filled.Photo),
            stringResource(R.string.import_from_gallery),
            importActions.launchGallery
        ),
        Triple(
            rememberVectorPainter(Icons.Filled.CameraAlt),
            stringResource(R.string.take_a_photo),
            importActions.launchCamera
        )
    )

    BackHandler(fabMenuExpanded) { updateFabMenuExpanded(false) }

    val expandedString = stringResource(R.string.expanded)
    val collapsedString = stringResource(R.string.collapsed)
    val toggleMenuString = stringResource(R.string.toggle_menu)
    val closeMenuString = stringResource(R.string.close_menu)

    FloatingActionButtonMenu(
        modifier = modifier,
        expanded = fabMenuExpanded,
        button = {
            ToggleFloatingActionButton(
                modifier = Modifier
                    .semantics {
                        traversalIndex = -1f
                        stateDescription =
                            if (fabMenuExpanded) expandedString else collapsedString
                        contentDescription = toggleMenuString
                    },
                checked = fabMenuExpanded,
                onCheckedChange = { updateFabMenuExpanded(it) }
            ) {
                val imageVector by remember {
                    derivedStateOf {
                        if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add
                    }
                }
                Icon(
                    painter = rememberVectorPainter(imageVector),
                    contentDescription = null,
                    modifier = Modifier.animateIcon({ checkedProgress })
                )
            }
        }
    ) {
        items.forEachIndexed { i, (icon, label, action) ->
            FloatingActionButtonMenuItem(
                modifier =
                    Modifier.semantics {
                        isTraversalGroup = true
                        if (i == items.size - 1) {
                            customActions = listOf(
                                CustomAccessibilityAction(
                                    label = closeMenuString,
                                    action = {
                                        action()
                                        updateFabMenuExpanded(false)
                                        true
                                    }
                                )
                            )
                        }
                    },
                onClick = {
                    action()
                    updateFabMenuExpanded(false)
                },
                icon = { Icon(icon, contentDescription = null) },
                text = { Text(text = label) }
            )
        }
    }
}

@Composable
private fun HomeEmptyState(
    icon: Painter,
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UnsavedDocumentCard(
    modifier: Modifier = Modifier,
    onNavigateUnsavedDocument: () -> Unit,
    onDismiss: () -> Unit = {},
) {
    SwipeBox(
        onDelete = onDismiss,
        modifier = Modifier,
        contentPaddingValues = PaddingValues(horizontal = 16.dp)
    ) {
        Card(
            modifier = modifier,
            onClick = { onNavigateUnsavedDocument() },
            shape = RoundedCornerShape(16.dp),
            colors = CardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    stringResource(R.string.user_document_unsaved_changes),
                    Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                )
            }
        }
    }
}

/**
 * Top bar shown while the multi selection mode is active: it replaces the search
 * bar and reveals the actions (Export, Share and Delete) for every selected
 * document.
 */
@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    enabled: Boolean,
    onClose: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 8.dp)
                .padding(top = 4.dp, bottom = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close_menu)
                    )
                }

                Text(
                    text = stringResource(R.string.documents_selected, selectedCount),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SelectionActionButton(
                    icon = Icons.Filled.FileDownload,
                    label = stringResource(R.string.export),
                    onClick = onExport,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                )

                SelectionActionButton(
                    icon = Icons.Filled.Share,
                    label = stringResource(R.string.share),
                    onClick = onShare,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                )

                SelectionActionButton(
                    icon = Icons.Filled.Delete,
                    label = stringResource(R.string.delete),
                    onClick = onDelete,
                    enabled = enabled,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SelectionActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val contentAlpha = if (enabled) 1f else 0.38f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint.copy(alpha = contentAlpha),
                modifier = Modifier.size(18.dp)
            )

            Spacer(Modifier.width(6.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )
        }
    }
}
