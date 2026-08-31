package es.pile.features.settings.ui.overview

import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.S
import android.os.Build.VERSION_CODES.TIRAMISU
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pile.R
import es.pile.core.domain.models.AppTheme
import es.pile.core.domain.models.ImageResolution
import es.pile.core.domain.models.ImageResolution.LOW
import es.pile.core.domain.models.ImageResolution.ORIGINAL
import es.pile.core.ui.composables.LoadingAlert
import es.pile.core.ui.composables.LoadingWrapper
import es.pile.core.ui.theme.PileTheme
import es.pile.features.settings.ui.composables.ItemPosition
import es.pile.features.settings.ui.composables.SettingsItem
import es.pile.features.settings.ui.composables.SettingsRadioButton
import es.pile.features.settings.ui.composables.SettingsSection
import es.pile.features.settings.ui.composables.SettingsTopBar
import es.pile.features.settings.ui.composables.SUPPORT_EMAIL
import es.pile.features.settings.ui.composables.SUPPORT_GITHUB
import es.pile.features.settings.ui.composables.SUPPORT_INSTAGRAM
import es.pile.features.settings.ui.composables.SupportContactsCard
import org.koin.androidx.compose.koinViewModel

/** MIME type used when the user picks where the local backup file is written. */
private const val BACKUP_MIME_TYPE = "application/zip"

@Composable
fun SettingsOverviewScreen(
    viewModel: SettingsOverviewViewModel = koinViewModel(),
    popBackStack: () -> Unit,
    navigateToSettingsResolution: () -> Unit,
    navigateToFavorites: () -> Unit = {},
    navigateToRecycleBin: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestoreWarning by rememberSaveable { mutableStateOf(false) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE)
    ) { uri ->
        if (uri != null) viewModel.handleEvent(SettingsOverviewEvent.OnBackupExported(uri))
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        pendingRestoreUri = uri
        showRestoreWarning = true
    }

    val context = LocalContext.current

    SettingsOverviewContent(
        state = state,
        navigateToFavorites = navigateToFavorites,
        navigateToRecycleBin = navigateToRecycleBin,
        onExportBackup = {
            exportBackupLauncher.launch("pile-backup-${System.currentTimeMillis()}.zip")
        },
        onImportBackup = {
            importBackupLauncher.launch(
                arrayOf(BACKUP_MIME_TYPE, "application/octet-stream", "*/*")
            )
        },
        onOpenUrl = { url ->
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        },
        onSendEmail = { address ->
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$address")
                    }
                )
            }
        },
        onEvent = { event ->
            when (event) {
                is SettingsOverviewEvent.OnBackClicked -> popBackStack()
                is SettingsOverviewEvent.OnResolutionClicked -> navigateToSettingsResolution()
                else -> viewModel.handleEvent(event)
            }
        }
    )

    if (showRestoreWarning) {
        val uri = pendingRestoreUri

        AlertDialog(
            onDismissRequest = {
                showRestoreWarning = false
                pendingRestoreUri = null
            },
            title = { Text(stringResource(R.string.restore_backup)) },
            text = { Text(stringResource(R.string.restore_backup_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreWarning = false
                    pendingRestoreUri = null

                    if (uri != null) {
                        viewModel.handleEvent(SettingsOverviewEvent.OnBackupRestored(uri))
                    }
                }) {
                    Text(stringResource(R.string.restore_backup))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreWarning = false
                    pendingRestoreUri = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsOverviewPreview() {
    PileTheme {
        SettingsOverviewContent(
            state = SettingsOverviewState(
                isLoading = false,
                isLocalAiEnabled = true,
            ),
            onEvent = {}
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsOverviewContent(
    state: SettingsOverviewState,
    onEvent: (SettingsOverviewEvent) -> Unit,
    navigateToFavorites: () -> Unit = {},
    navigateToRecycleBin: () -> Unit = {},
    onExportBackup: () -> Unit = {},
    onImportBackup: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
    onSendEmail: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showAppThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showEditProfileDialog by rememberSaveable { mutableStateOf(false) }
    var supportDialog by remember { mutableStateOf(SupportDialog.NONE) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.backupMessage) {
        state.backupMessage?.let { uiText ->
            snackbarHostState.showSnackbar(message = uiText.asString(context))
            onEvent(SettingsOverviewEvent.OnBackupMessageDismissed)
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.displayCutout,
        topBar = {
            SettingsTopBar(
                title = stringResource(R.string.settings),
                popBackStack = { onEvent(SettingsOverviewEvent.OnBackClicked) },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LoadingWrapper(state.isLoading) {
            Column(
                modifier = modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                UserAccountSection(
                    profileName = state.profileName,
                    profileEmail = state.profileEmail,
                    onEditProfile = { showEditProfileDialog = true }
                )

                AppearanceSection(
                    theme = state.theme,
                    isMaterialColor = state.isMaterialColor,
                    onAppThemeChange = { showAppThemeDialog = true },
                    onMaterialColorToggle = { onEvent(SettingsOverviewEvent.OnMaterialColorToggled) }
                )

                ResolutionSection(
                    imageResolution = state.imageResolution,
                    onResolutionChange = { onEvent(SettingsOverviewEvent.OnResolutionClicked) }
                )

                LibrarySection(
                    onFavoritesClick = navigateToFavorites,
                    onRecycleBinClick = navigateToRecycleBin
                )

                LocalBackupSection(
                    enabled = !state.isWorkingOnBackup,
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup
                )

                AboutSupportSection(
                    onOpenUrl = onOpenUrl,
                    onSendEmail = onSendEmail,
                    onAbout = { supportDialog = SupportDialog.ABOUT },
                    onHelp = { supportDialog = SupportDialog.HELP },
                    onPrivacy = { supportDialog = SupportDialog.PRIVACY }
                )
            }
        }
    }

    if (state.isWorkingOnBackup) {
        LoadingAlert(title = stringResource(R.string.working_on_backup))
    }

    if (showAppThemeDialog) {
        AppThemeDialog(
            currentTheme = state.theme,
            onDismiss = {
                showAppThemeDialog = false
            },
            onConfirm = {
                onEvent(SettingsOverviewEvent.OnThemeChanged(it))
                showAppThemeDialog = false
            }
        )
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            initialName = state.profileName.orEmpty(),
            initialEmail = state.profileEmail.orEmpty(),
            onDismiss = { showEditProfileDialog = false },
            onConfirm = { name, email ->
                onEvent(SettingsOverviewEvent.OnProfileUpdated(name, email))
                showEditProfileDialog = false
            }
        )
    }

    when (supportDialog) {
        SupportDialog.NONE -> {}
        SupportDialog.ABOUT -> AboutPileDialog(onDismiss = { supportDialog = SupportDialog.NONE })
        SupportDialog.HELP -> HelpSupportDialog(onDismiss = { supportDialog = SupportDialog.NONE })
        SupportDialog.PRIVACY -> PrivacyPolicyDialog(onDismiss = { supportDialog = SupportDialog.NONE })
    }
}

@Composable
private fun UserAccountSection(
    modifier: Modifier = Modifier,
    profileName: String?,
    profileEmail: String?,
    onEditProfile: () -> Unit
) {
    SettingsSection(modifier = modifier, title = stringResource(R.string.user_account)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.person_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        text = profileName?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.profile_name_default),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = profileEmail?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.profile_email_default),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(onClick = onEditProfile) {
                    Text(stringResource(R.string.edit_profile))
                }
            }
        }
    }
}

@Composable
private fun AppearanceSection(
    modifier: Modifier = Modifier,
    theme: AppTheme,
    isMaterialColor: Boolean,
    onAppThemeChange: () -> Unit,
    onMaterialColorToggle: () -> Unit
) {
    val isMaterialColorCompatible = SDK_INT >= S

    val subtitle = when (theme) {
        AppTheme.SYSTEM -> stringResource(R.string.system_default)
        AppTheme.DARK -> stringResource(R.string.dark)
        AppTheme.LIGHT -> stringResource(R.string.light)
    }

    SettingsSection(modifier = modifier, title = stringResource(R.string.appearance)) {
        SettingsItem(
            itemPosition = if (isMaterialColorCompatible) ItemPosition.TOP else ItemPosition.SINGLE,
            title = stringResource(R.string.theme),
            subtitle = subtitle,
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.dark_mode_24px),
                    contentDescription = null
                )
            },
            onAction = onAppThemeChange
        )

        if (isMaterialColorCompatible) {
            SettingsItem(
                itemPosition = ItemPosition.BOTTOM,
                title = stringResource(R.string.use_system_theme_color),
                checked = isMaterialColor,
                onAction = onMaterialColorToggle
            )
        }
    }
}

@Composable
private fun ResolutionSection(
    modifier: Modifier = Modifier,
    imageResolution: ImageResolution,
    onResolutionChange: () -> Unit,
) {
    val subtitle = when (imageResolution) {
        ORIGINAL -> stringResource(R.string.original_quality_tittle)
        LOW -> stringResource(R.string.storage_saver_tittle)
    }

    SettingsSection(modifier = modifier, title = stringResource(R.string.resolution)) {
        SettingsItem(
            itemPosition = ItemPosition.SINGLE,
            title = stringResource(R.string.document_resolution),
            subtitle = subtitle,
            onAction = onResolutionChange
        )
    }
}

@Composable
private fun LibrarySection(
    modifier: Modifier = Modifier,
    onFavoritesClick: () -> Unit,
    onRecycleBinClick: () -> Unit
) {
    SettingsSection(modifier = modifier, title = stringResource(R.string.library)) {
        SettingsItem(
            itemPosition = ItemPosition.TOP,
            title = stringResource(R.string.favorites),
            subtitle = stringResource(R.string.favorites_subtitle),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            onAction = onFavoritesClick
        )
        SettingsItem(
            itemPosition = ItemPosition.BOTTOM,
            title = stringResource(R.string.recycle_bin),
            subtitle = stringResource(R.string.recycle_bin_subtitle),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_recycle_bin_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            onAction = onRecycleBinClick
        )
    }
}

/**
 * Everything here stays on the device: the backup is a single file written exactly
 * where the user picks it, and it can only be read back from this same screen.
 */
@Composable
private fun LocalBackupSection(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    SettingsSection(modifier = modifier, title = stringResource(R.string.local_backup_restore)) {
        SettingsItem(
            enabled = enabled,
            itemPosition = ItemPosition.TOP,
            title = stringResource(R.string.export_backup),
            subtitle = stringResource(R.string.export_backup_body),
            leadingIcon = {
                Icon(painterResource(R.drawable.save_24px), contentDescription = null)
            },
            onAction = onExportBackup
        )
        SettingsItem(
            enabled = enabled,
            itemPosition = ItemPosition.BOTTOM,
            title = stringResource(R.string.restore_backup),
            subtitle = stringResource(R.string.restore_backup_body),
            leadingIcon = {
                Icon(painterResource(R.drawable.download_24px), contentDescription = null)
            },
            onAction = onImportBackup
        )
    }
}

@Composable
private fun AboutSupportSection(
    modifier: Modifier = Modifier,
    onOpenUrl: (String) -> Unit,
    onSendEmail: (String) -> Unit,
    onAbout: () -> Unit,
    onHelp: () -> Unit,
    onPrivacy: () -> Unit
) {
    SettingsSection(modifier = modifier, title = stringResource(R.string.about_support)) {
        SupportContactsCard(
            onOpenUrl = onOpenUrl,
            onSendEmail = onSendEmail
        )

        SettingsItem(
            itemPosition = ItemPosition.TOP,
            title = stringResource(R.string.about_pile),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.info_24px),
                    contentDescription = null
                )
            },
            onAction = onAbout
        )

        SettingsItem(
            itemPosition = ItemPosition.MIDDLE,
            title = stringResource(R.string.help_support),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.mail_24px),
                    contentDescription = null
                )
            },
            onAction = onHelp
        )

        SettingsItem(
            itemPosition = ItemPosition.BOTTOM,
            title = stringResource(R.string.privacy_policy),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_privacy_shield),
                    contentDescription = null
                )
            },
            onAction = onPrivacy
        )
    }
}

@Composable
private fun EditProfileDialog(
    initialName: String,
    initialEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, email: String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var email by rememberSaveable { mutableStateOf(initialEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_profile)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.profile_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.profile_email)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim(), email.trim()) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun AboutPileDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            if (SDK_INT >= TIRAMISU) {
                context.packageManager
                    .getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                    .versionName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }
        }.getOrNull().orEmpty()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_pile)) },
        text = {
            Column {
                Text(stringResource(R.string.about_pile_body))

                Text(
                    text = stringResource(R.string.app_version, version),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

@Composable
private fun HelpSupportDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.help_support)) },
        text = {
            Column {
                Text(stringResource(R.string.help_support_body))
                Text(
                    text = stringResource(
                        R.string.support_details,
                        SUPPORT_INSTAGRAM,
                        SUPPORT_GITHUB,
                        SUPPORT_EMAIL
                    ),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

@Composable
private fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.privacy_policy)) },
        text = { Text(stringResource(R.string.privacy_policy_body)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

@Composable
fun AppThemeDialog(
    modifier: Modifier = Modifier,
    currentTheme: AppTheme,
    onDismiss: () -> Unit,
    onConfirm: (AppTheme) -> Unit
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_theme)) },
        text = {
            Column {
                SettingsRadioButton(
                    title = stringResource(R.string.system_default),
                    selected = currentTheme == AppTheme.SYSTEM,
                    onClick = { onConfirm(AppTheme.SYSTEM) }
                )

                SettingsRadioButton(
                    title = stringResource(R.string.light),
                    selected = currentTheme == AppTheme.LIGHT,
                    onClick = { onConfirm(AppTheme.LIGHT) }
                )

                SettingsRadioButton(
                    title = stringResource(R.string.dark),
                    selected = currentTheme == AppTheme.DARK,
                    onClick = { onConfirm(AppTheme.DARK) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private enum class SupportDialog {
    NONE,
    ABOUT,
    HELP,
    PRIVACY
}
