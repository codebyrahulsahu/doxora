package es.pile.core.domain.repositories

import es.pile.core.domain.models.AppTheme
import es.pile.core.domain.models.DocumentViewMode
import es.pile.core.domain.models.ImageResolution
import es.pile.core.domain.models.UserSettings
import kotlinx.coroutines.flow.Flow

/**
 * Repository responsible for managing and persisting user preferences.
 *
 * It provides a continuous stream of [UserSettings] and exposes methods to
 * perform atomic updates on specific preferences such as theme, image quality,
 * and AI configurations.
 */
interface SettingsRepository {
    /**
     * A [Flow] that emits the current [UserSettings] whenever they change.
     */
    val userSettings: Flow<UserSettings>

    /**
     * Updates the user settings with the provided [UserSettings] object.
     *
     * @param userSettings The new [UserSettings]
     */
    suspend fun updateUserSettings(userSettings: UserSettings)

    /**
     * Updates the application's visual theme.
     *
     * @param theme The new [AppTheme] to be applied (System, Light, or Dark).
     */
    suspend fun updateTheme(theme: AppTheme)

    /**
     * Updates the configuration for Material You color.
     *
     * @param enable Whether Material You color should be enabled or disabled.
     */
    suspend fun updateMaterialColor(enable: Boolean)

    /**
     * Updates the configuration for local AI features.
     *
     * @param enable Whether local AI should be enabled or disabled.
     */
    suspend fun updateLocalAi(enable: Boolean)

    /**
     * Updates the selected language model for local AI.
     *
     * @param model The new language model to be used.
     */
    suspend fun updateSelectedModel(model: String?)

    /**
     * Updates the image quality setting.
     *
     * @param resolution The new [ImageResolution] to be applied.
     */
    suspend fun updateImageResolution(resolution: ImageResolution)

    /**
     * Enables or disables the Document Resizer.
     *
     * When enabled, new document images are compressed to the configured
     * [updateDocumentResizerTargetSizeKb] target size keeping the best
     * possible quality.
     *
     * @param enable Whether the Document Resizer should be enabled.
     */
    suspend fun updateDocumentResizerEnabled(enable: Boolean)

    /**
     * Updates the target file size used by the Document Resizer.
     *
     * @param sizeKb The new target size in kilobytes.
     */
    suspend fun updateDocumentResizerTargetSizeKb(sizeKb: Int)

    /**
     * Updates the user profile display name.
     *
     * @param name The new profile name.
     */
    suspend fun updateProfileName(name: String)

    /**
     * Updates the user profile email.
     *
     * @param email The new profile email.
     */
    suspend fun updateProfileEmail(email: String)

    /**
     * Updates the layout used to display the documents in the main screen.
     *
     * @param viewMode The new [DocumentViewMode] (list or icon grid).
     */
    suspend fun updateDocumentViewMode(viewMode: DocumentViewMode)

    /**
     * Updates the profile picture.
     *
     * @param path File name of the stored picture inside the app's internal
     * storage, or null to remove the current picture.
     */
    suspend fun updateProfilePicturePath(path: String?)

    /**
     * Updates the profile picture uploaded for a hub.
     *
     * @param hubId Id of the hub the picture belongs to.
     * @param path File name of the stored picture inside the app's internal
     * storage, or null to remove the current hub picture.
     */
    suspend fun updateHubPicturePath(hubId: String, path: String?)
}