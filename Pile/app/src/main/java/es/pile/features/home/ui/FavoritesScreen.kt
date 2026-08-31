package es.pile.features.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pile.core.domain.models.DocumentCoverItem
import es.pile.core.domain.repositories.DocumentModelRepository
import es.pile.core.domain.repositories.FavoritesRepository
import es.pile.core.ui.composables.itemDocumentsVerticalList
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class FavoritesViewModel(
    documentRepository: DocumentModelRepository,
    favoritesRepository: FavoritesRepository
) : ViewModel() {
    val documents = combine(documentRepository.documentModels, favoritesRepository.favoriteDocumentIds) { docs, ids ->
        val favorites = ids.toSet()
        docs.filter { it.id in favorites }.map { DocumentCoverItem(it, "favorite_${it.id}") }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    popBackStack: () -> Unit,
    navigateToDocumentDetail: (String) -> Unit,
    viewModel: FavoritesViewModel = koinViewModel()
) {
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Favorites") },
            navigationIcon = { IconButton(onClick = popBackStack) { Icon(Icons.Default.ArrowBack, "Back") }, },
            actions = { Icon(Icons.Default.Star, "Favorites", tint = MaterialTheme.colorScheme.primary) }
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (documents.isEmpty()) item { Text("No favorite documents yet", modifier = Modifier.padding(24.dp)) }
            itemDocumentsVerticalList(documents = documents, documentSizes = emptyMap(), onDocumentClick = navigateToDocumentDetail)
        }
    }
}
