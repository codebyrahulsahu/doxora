package es.pile.core.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class AppPreferences(
    val isOnboardingCompleted: Boolean = false,
    /**
     * Tree URI (as a string) of the folder the user picked the first time a
     * document was exported.
     *
     * The permission over that folder is persisted, so the storage location is
     * only asked once: every later export reuses it silently.
     */
    val exportFolderUri: String? = null
)
