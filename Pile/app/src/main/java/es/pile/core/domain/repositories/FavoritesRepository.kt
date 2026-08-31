package es.pile.core.domain.repositories

import kotlinx.coroutines.flow.Flow

/** Stores favorites locally so they survive app restarts and never leave the device. */
interface FavoritesRepository {
    val favoriteDocumentIds: Flow<List<String>>
    suspend fun setFavorite(documentId: String, favorite: Boolean)
}
