package es.pile.features.settings.ui.overview

import android.net.Uri
import es.pile.core.domain.models.AppTheme
import es.pile.core.domain.models.ImageResolution
import es.pile.core.ui.util.UiText
import java.io.File

data class SettingsOverviewState(
    val isLoading: Boolean = true,
    val theme: AppTheme = AppTheme.SYSTEM,
    val isMaterialColor: Boolean = true,
    val isLocalAiEnabled: Boolean = false,
    val selectedModel: String? = null,
    val imageResolution: ImageResolution = ImageResolution.LOW,
    val profileName: String? = null,
    val profileEmail: String? = null,
    val profilePictureFile: File? = null,
    val isWorkingOnProfilePicture: Boolean = false,
    val profilePictureMessage: UiText? = null,
    val isWorkingOnBackup: Boolean = false,
    val backupMessage: UiText? = null,
    /**
     * Tree URI (as a string) of the folder where exported documents are saved,
     * or null when no folder has been chosen yet (it is then asked on the
     * first export).
     */
    val exportFolderUri: String? = null
)

sealed interface SettingsOverviewEvent {
    data object OnBackClicked : SettingsOverviewEvent
    data object OnResolutionClicked : SettingsOverviewEvent
    data class OnThemeChanged(val newTheme: AppTheme) : SettingsOverviewEvent
    data object OnMaterialColorToggled : SettingsOverviewEvent
    data object OnLocalAiToggled : SettingsOverviewEvent
    data class OnProfileUpdated(val name: String, val email: String) : SettingsOverviewEvent

    // Profile picture
    data class OnProfilePicturePicked(val uri: Uri) : SettingsOverviewEvent
    data object OnProfilePictureRemoved : SettingsOverviewEvent
    data object OnProfilePictureMessageDismissed : SettingsOverviewEvent

    // Export folder
    /** The user picked a new folder where the exported documents are saved. */
    data class OnExportFolderPicked(val uri: Uri) : SettingsOverviewEvent

    /** Forget the saved export folder: it will be asked again on the next export. */
    data object OnExportFolderReset : SettingsOverviewEvent

    // Local backup & restore
    data class OnBackupExported(val uri: Uri) : SettingsOverviewEvent
    data class OnBackupRestored(val uri: Uri) : SettingsOverviewEvent

    data object OnBackupMessageDismissed : SettingsOverviewEvent
}
