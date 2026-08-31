package es.pile.features.settings.ui.overview

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pile.R
import es.pile.core.domain.models.AppTheme
import es.pile.core.domain.repositories.BackupSummary
import es.pile.core.domain.repositories.LocalBackupRepository
import es.pile.core.domain.repositories.SettingsRepository
import es.pile.core.ui.util.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsOverviewViewModel(
    private val settingsRepository: SettingsRepository,
    private val localBackupRepository: LocalBackupRepository
) : ViewModel() {

    /** Transient state of the local backup / restore operations. */
    private val backupState = MutableStateFlow(BackupUiState())

    val state: StateFlow<SettingsOverviewState> = combine(
        settingsRepository.userSettings,
        backupState
    ) { userSettings, backup ->
        SettingsOverviewState(
            isLoading = false,
            theme = userSettings.theme,
            isMaterialColor = userSettings.isMaterialColor,
            isLocalAiEnabled = userSettings.isLocalAiEnabled,
            selectedModel = userSettings.selectedModel,
            imageResolution = userSettings.imageResolution,
            profileName = userSettings.profileName,
            profileEmail = userSettings.profileEmail,
            isWorkingOnBackup = backup.isWorking,
            backupMessage = backup.message
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

            is SettingsOverviewEvent.OnBackupExported -> exportBackup(event.uri)

            is SettingsOverviewEvent.OnBackupRestored -> restoreBackup(event.uri)

            SettingsOverviewEvent.OnBackupMessageDismissed -> updateBackupState(message = null)
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

    private fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            updateBackupState(isWorking = true, message = null)

            val result = localBackupRepository.createBackup(uri)

            if (result.isSuccess) {
                val summary = result.getOrDefault(BackupSummary())

                updateBackupState(
                    isWorking = false,
                    message = UiText.StringResource(
                        R.string.backup_exported,
                        summary.documents
                    )
                )
            } else {
                updateBackupState(
                    isWorking = false,
                    message = UiText.StringResource(R.string.error_exporting_backup)
                )
            }
        }
    }

    private fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            updateBackupState(isWorking = true, message = null)

            val result = localBackupRepository.restoreBackup(uri)

            if (result.isSuccess) {
                val summary = result.getOrDefault(BackupSummary())

                updateBackupState(
                    isWorking = false,
                    message = UiText.StringResource(
                        R.string.backup_restored,
                        summary.documents
                    )
                )
            } else {
                updateBackupState(
                    isWorking = false,
                    message = UiText.StringResource(R.string.error_restoring_backup)
                )
            }
        }
    }

    private fun updateBackupState(isWorking: Boolean = false, message: UiText? = null) {
        backupState.update { it.copy(isWorking = isWorking, message = message) }
    }

    private data class BackupUiState(
        val isWorking: Boolean = false,
        val message: UiText? = null
    )
}
