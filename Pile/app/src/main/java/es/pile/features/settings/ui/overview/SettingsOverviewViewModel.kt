package es.pile.features.settings.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pile.core.domain.models.AppTheme
import es.pile.core.domain.repositories.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsOverviewViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val state: StateFlow<SettingsOverviewState> = settingsRepository.userSettings
        .map { userSettings ->
            SettingsOverviewState(
                isLoading = false,
                theme = userSettings.theme,
                isMaterialColor = userSettings.isMaterialColor,
                isLocalAiEnabled = userSettings.isLocalAiEnabled,
                selectedModel = userSettings.selectedModel,
                imageResolution = userSettings.imageResolution,
                profileName = userSettings.profileName,
                profileEmail = userSettings.profileEmail,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsOverviewState()
        )

    fun handleEvent(event: SettingsOverviewEvent) {
        when (event) {
            SettingsOverviewEvent.OnBackClicked -> {}
            SettingsOverviewEvent.OnResolutionClicked -> {}

            is SettingsOverviewEvent.OnThemeChanged -> updateTheme(event.newTheme)

            SettingsOverviewEvent.OnMaterialColorToggled -> updateMaterialColor()

            SettingsOverviewEvent.OnLocalAiToggled -> updateLocalAi()

            is SettingsOverviewEvent.OnProfileUpdated -> updateProfile(event.name, event.email)

        }
    }

    private fun updateTheme(newTheme: AppTheme) {
        viewModelScope.launch {
            settingsRepository.updateTheme(newTheme)
        }
    }

    private fun updateMaterialColor() {
        viewModelScope.launch {
            settingsRepository.updateMaterialColor(!state.value.isMaterialColor)
        }
    }

    private fun updateLocalAi() {
        viewModelScope.launch {
            settingsRepository.updateLocalAi(!state.value.isLocalAiEnabled)
        }
    }

    private fun updateProfile(name: String, email: String) {
        viewModelScope.launch {
            settingsRepository.updateProfileName(name)
            settingsRepository.updateProfileEmail(email)
        }
    }

}
