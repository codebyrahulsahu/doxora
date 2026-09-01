package es.pile.features.settings.ui.resizer

import java.util.Locale

/**
 * UI state of the Document Resizer settings screen.
 *
 * @property isLoading Whether the current settings are still being loaded.
 * @property isEnabled Whether the Document Resizer toggle is on.
 * @property targetSizeKb The configured custom target file size in kilobytes.
 */
data class SettingsDocumentResizerState(
    val isLoading: Boolean = true,
    val isEnabled: Boolean = false,
    val targetSizeKb: Int = 512
)

sealed interface SettingsDocumentResizerEvent {
    data object OnBackClicked : SettingsDocumentResizerEvent
    data object OnEnabledToggled : SettingsDocumentResizerEvent
    data class OnTargetSizeChanged(val targetSizeKb: Int) : SettingsDocumentResizerEvent
}

/**
 * Formats a size in KB as a human friendly string ("512 KB", "1.5 MB", "1 MB").
 */
fun formatDocumentResizerTargetSize(sizeKb: Int): String {
    if (sizeKb < 1024) return "$sizeKb KB"

    return if (sizeKb % 1024 == 0) {
        "${sizeKb / 1024} MB"
    } else {
        String.format(Locale.US, "%.1f MB", sizeKb / 1024f)
    }
}
