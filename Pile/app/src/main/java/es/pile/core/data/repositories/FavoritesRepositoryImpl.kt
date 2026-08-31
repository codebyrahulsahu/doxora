package es.pile.core.data.repositories

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import es.pile.DatabaseQueries
import es.pile.core.domain.repositories.FavoritesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant

class FavoritesRepositoryImpl(
    private val databaseQueries: DatabaseQueries,
    private val ioDispatcher: CoroutineDispatcher
) : FavoritesRepository {
    override val favoriteDocumentIds: Flow<List<String>> =
        databaseQueries.selectFavoriteDocumentIds().asFlow().mapToList(ioDispatcher).map { it }

    override suspend fun setFavorite(documentId: String, favorite: Boolean): Unit = withContext(ioDispatcher) {
        if (favorite) databaseQueries.addFavoriteDocument(documentId, Instant.now().toString())
        else databaseQueries.removeFavoriteDocument(documentId)
    }
}
