package es.pile.features.settings.ui.resizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pile.core.domain.repositories.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsDocumentResizerViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val state: StateFlow<SettingsDocumentResizerState> = settingsRepository.userSettings
        .map { userSettings ->
            SettingsDocumentResizerState(
                isLoading = false,
                isEnabled = userSettings.isDocumentResizerEnabled,
                targetSizeKb = userSettings.documentResizerTargetSizeKb
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsDocumentResizerState()
        )

    fun handleEvent(event: SettingsDocumentResizerEvent) {
        when (event) {
            SettingsDocumentResizerEvent.OnBackClicked -> {}
            SettingsDocumentResizerEvent.OnEnabledToggled -> updateEnabled()
            is SettingsDocumentResizerEvent.OnTargetSizeChanged -> {
                updateTargetSize(event.targetSizeKb)
            }
        }
    }

    private fun updateEnabled() {
        viewModelScope.launch {
            val enabled = state.value.isEnabled
            settingsRepository.updateDocumentResizerEnabled(!enabled)
        }
    }

    private fun updateTargetSize(targetSizeKb: Int) {
        viewModelScope.launch {
            settingsRepository.updateDocumentResizerTargetSizeKb(targetSizeKb.coerceIn(MIN_SIZE_KB, MAX_SIZE_KB))
        }
    }

    private companion object {
        /** Smallest target size (in KB) accepted by the Document Resizer. */
        const val MIN_SIZE_KB = 16

        /** Largest target size (in KB) accepted by the Document Resizer (100 MB). */
        const val MAX_SIZE_KB = 1024 * 100
    }
}
