package es.pile.core.domain.repositories

import es.pile.core.domain.models.AppPreferences
import kotlinx.coroutines.flow.Flow

interface AppPreferencesRepository {
    val appPreferences: Flow<AppPreferences>
    suspend fun updateOnboardingCompleted(completed: Boolean)

    /**
     * Stores the folder the user chose to save the exported files in, so the
     * storage location (and its permission) is only requested once.
     *
     * @param uri String representation of the tree URI, or null to forget it.
     */
    suspend fun updateExportFolderUri(uri: String?)
}
