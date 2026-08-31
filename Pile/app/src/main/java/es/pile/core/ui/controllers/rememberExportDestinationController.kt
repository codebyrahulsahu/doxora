package es.pile.core.ui.controllers

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import es.pile.core.domain.models.DocumentExportFormat
import es.pile.core.domain.repositories.AppPreferencesRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Actions exposed by [rememberExportDestinationController].
 *
 * @property requestExport Starts the export of the given format. The folder
 * where the files are written is only asked the first time; afterwards the
 * remembered folder is reused.
 */
data class ExportActions(
    val requestExport: (DocumentExportFormat) -> Unit
)

/**
 * Headless controller shared by every screen that can export documents.
 *
 * The user always picks the format first (PDF, JPG or PNG) and only then, and
 * only once, is the storage location requested: the chosen folder is persisted
 * with a durable read/write permission and reused for all the later exports.
 *
 * @param onExport Invoked with the chosen format and the destination folder
 * (null when no folder could be granted, in which case the caller falls back to
 * the public Downloads directory).
 */
@Composable
fun rememberExportDestinationController(
    onExport: (DocumentExportFormat, Uri?) -> Unit
): ExportActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Koin is not available while rendering a @Preview.
    val appPreferencesRepository: AppPreferencesRepository? =
        if (LocalInspectionMode.current) null else koinInject<AppPreferencesRepository>()

    // Format chosen by the user while the storage permission is being granted.
    val pendingFormat = remember { mutableStateOf<DocumentExportFormat?>(null) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { folderUri ->
        val format = pendingFormat.value ?: return@rememberLauncherForActivityResult
        pendingFormat.value = null

        if (folderUri == null) return@rememberLauncherForActivityResult

        // Keep the permission across process restarts so it is never asked again.
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                folderUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }.onFailure { Napier.e("Could not persist the export folder permission", it) }

        scope.launch {
            appPreferencesRepository?.updateExportFolderUri(folderUri.toString())
        }

        onExport(format, folderUri)
    }

    return remember(onExport) {
        ExportActions(
            requestExport = { format ->
                scope.launch {
                    val storedUri = appPreferencesRepository
                        ?.appPreferences
                        ?.first()
                        ?.exportFolderUri
                        ?.let(Uri::parse)

                    val hasPermission = storedUri != null && context.contentResolver
                        .persistedUriPermissions
                        .any { it.uri == storedUri && it.isWritePermission }

                    if (storedUri != null && hasPermission) {
                        onExport(format, storedUri)
                    } else {
                        pendingFormat.value = format
                        folderPickerLauncher.launch(storedUri)
                    }
                }
            }
        )
    }
}
