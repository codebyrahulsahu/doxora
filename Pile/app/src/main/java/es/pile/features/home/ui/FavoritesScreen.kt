package es.pile.features.home.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonDefaults.smallContainerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pile.R
import es.pile.core.domain.models.DocumentCoverItem
import es.pile.core.domain.models.DocumentSortOrder
import es.pile.core.ui.composables.DocumentSortMenu
import es.pile.core.ui.composables.LoadingWrapper
import es.pile.core.ui.composables.itemDocumentsVerticalList
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    popBackStack: () -> Unit,
    navigateToDocumentDetail: (String) -> Unit,
    viewModel: FavoritesViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bitmapCache by viewModel.bitmapCache.collectAsStateWithLifecycle()

    FavoritesContent(
        modifier = modifier,
        state = state,
        bitmapCache = bitmapCache,
        onEvent = viewModel::handleEvent,
        popBackStack = popBackStack,
        navigateToDocumentDetail = navigateToDocumentDetail
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoritesContent(
    modifier: Modifier = Modifier,
    state: FavoritesState,
    bitmapCache: Map<String, Bitmap>,
    onEvent: (FavoritesEvent) -> Unit,
    popBackStack: () -> Unit,
    navigateToDocumentDetail: (String) -> Unit
) {
    val sortedDocuments = sortDocuments(state.documents, state.sortOrder)

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                title = { Text(stringResource(R.string.favorites)) },
                navigationIcon = {
                    FilledIconButton(
                        modifier = Modifier
                            .padding(start = 14.dp, end = 4.dp)
                            .size(smallContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)),
                        onClick = popBackStack
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24px),
                            contentDescription = stringResource(R.string.return_)
                        )
                    }
                },
                actions = {
                    DocumentSortMenu(
                        sortOrder = state.sortOrder,
                        onSortOrderChange = { onEvent(FavoritesEvent.OnSortOrderChanged(it)) }
                    )
                }
            )
        }
    ) { padding ->
        LoadingWrapper(state.isLoading) {
            if (sortedDocuments.isEmpty()) {
                FavoritesEmptyState(modifier = Modifier.padding(padding))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    itemDocumentsVerticalList(
                        documents = sortedDocuments,
                        documentSizes = state.documentSizes,
                        bitmapCache = bitmapCache,
                        onLoadBitmap = { onEvent(FavoritesEvent.OnImageDisplayed(it)) },
                        lockedDocumentIds = state.lockedDocumentIds,
                        onDocumentClick = navigateToDocumentDetail,
                        favoriteDocumentIds = state.favoriteDocumentIds,
                        onFavoriteToggle = { onEvent(FavoritesEvent.OnFavoriteToggled(it)) }
                    )

                    item { Box(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

private fun sortDocuments(
    documents: List<DocumentCoverItem>,
    sortOrder: DocumentSortOrder
): List<DocumentCoverItem> = documents.sortedWith(
    when (sortOrder) {
        DocumentSortOrder.NEWEST ->
            compareByDescending<DocumentCoverItem> { it.document.creationDateTime }

        DocumentSortOrder.OLDEST ->
            compareBy<DocumentCoverItem> { it.document.creationDateTime }
    }
)

@Composable
private fun FavoritesEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.user_ic_bookmark_star_24px),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
            )
            Text(
                text = stringResource(R.string.no_favorites),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
        }
    }
}
