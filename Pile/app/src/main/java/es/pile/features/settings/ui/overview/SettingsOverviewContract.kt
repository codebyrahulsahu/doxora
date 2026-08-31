package es.pile.features.settings.ui.overview

import es.pile.core.domain.models.AppTheme
import es.pile.core.domain.models.ImageResolution

data class SettingsOverviewState(
    val isLoading: Boolean = true,
    val theme: AppTheme = AppTheme.SYSTEM,
    val isMaterialColor: Boolean = true,
    val isLocalAiEnabled: Boolean = false,
    val selectedModel: String? = null,
    val imageResolution: ImageResolution = ImageResolution.LOW,
    val profileName: String? = null,
    val profileEmail: String? = null,
    val isCloudBackupEnabled: Boolean = false,
    val isAppLockEnabled: Boolean = false,
)

sealed interface SettingsOverviewEvent {
    data object OnBackClicked : SettingsOverviewEvent
    data object OnResolutionClicked : SettingsOverviewEvent
    data class OnThemeChanged(val newTheme: AppTheme) : SettingsOverviewEvent
    data object OnMaterialColorToggled : SettingsOverviewEvent
    data object OnLocalAiToggled : SettingsOverviewEvent
    data class OnProfileUpdated(val name: String, val email: String) : SettingsOverviewEvent
    data object OnCloudBackupToggled : SettingsOverviewEvent
    data object OnAppLockToggled : SettingsOverviewEvent
}
