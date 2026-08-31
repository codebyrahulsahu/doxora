package es.pile.features.settings.ui.overview

import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.S
import android.os.Build.VERSION_CODES.TIRAMISU
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import es.pile.core.ui.composables.LoadingWrapper
import es.pile.core.ui.theme.PileTheme
import es.pile.features.settings.ui.composables.ItemPosition
import es.pile.features.settings.ui.composables.SettingsItem
import es.pile.features.settings.ui.composables.SettingsRadioButton
import es.pile.features.settings.ui.composables.SettingsSection
import es.pile.features.settings.ui.composables.SettingsTopBar
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsOverviewScreen(
    viewModel: SettingsOverviewViewModel = koinViewModel(),
    popBackStack: () -> Unit,
    navigateToSettingsResolution: () -> Unit,
    navigateToFavorites: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsOverviewContent(
        state = state,
        navigateToFavorites = navigateToFavorites,
        onEvent = { event ->
            when (event) {
                is SettingsOverviewEvent.OnBackClicked -> popBackStack()
                is SettingsOverviewEvent.OnResolutionClicked -> navigateToSettingsResolution()
                else -> viewModel.handleEvent(event)
            }
        }
    )
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
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showAppThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showEditProfileDialog by rememberSaveable { mutableStateOf(false) }
    var supportDialog by remember { mutableStateOf(SupportDialog.NONE) }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.displayCutout,
        topBar = {
            SettingsTopBar(
                title = stringResource(R.string.settings),
                popBackStack = { onEvent(SettingsOverviewEvent.OnBackClicked) },
                scrollBehavior = scrollBehavior
            )
        }
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

                SettingsSection(title = "Library") {
                    SettingsItem(
                        itemPosition = ItemPosition.SINGLE,
                        title = "Favorites",
                        subtitle = "Your starred documents",
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                        onAction = navigateToFavorites
                    )
                }

                LocalBackupSection()

                AboutSupportSection(
                    onAbout = { supportDialog = SupportDialog.ABOUT },
                    onHelp = { supportDialog = SupportDialog.HELP },
                    onPrivacy = { supportDialog = SupportDialog.PRIVACY }
                )
            }
        }
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

                IconButton(onClick = onEditProfile) {
                    Icon(
                        painter = painterResource(R.drawable.edit_24px),
                        contentDescription = stringResource(R.string.edit_profile),
                        tint = MaterialTheme.colorScheme.primary
                    )
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
private fun LocalBackupSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    SettingsSection(modifier = modifier, title = stringResource(R.string.local_backup_restore)) {
        SettingsItem(
            itemPosition = ItemPosition.TOP,
            title = stringResource(R.string.export_backup),
            subtitle = stringResource(R.string.local_backup_restore_body),
            leadingIcon = { Icon(painterResource(R.drawable.save_24px), contentDescription = null) },
            onAction = {
                context.startActivity(android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "application/json"
                    putExtra(android.content.Intent.EXTRA_TITLE, "pile-backup.json")
                })
            }
        )
        SettingsItem(
            itemPosition = ItemPosition.BOTTOM,
            title = stringResource(R.string.restore_backup),
            leadingIcon = { Icon(painterResource(R.drawable.download_24px), contentDescription = null) },
            onAction = {
                context.startActivity(android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "application/json"
                    addCategory(android.content.Intent.CATEGORY_OPENABLE)
                })
            }
        )
    }
}

@Composable
private fun AboutSupportSection(
    modifier: Modifier = Modifier,
    onAbout: () -> Unit,
    onHelp: () -> Unit,
    onPrivacy: () -> Unit
) {
    SettingsSection(modifier = modifier, title = stringResource(R.string.about_support)) {
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
                SupportQrCode(modifier = Modifier.padding(top = 16.dp))
                Text(stringResource(R.string.support_details), modifier = Modifier.padding(top = 12.dp))
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
private fun SupportQrCode(modifier: Modifier = Modifier) {
    // A local, scannable-looking support marker keeps the page usable offline.
    // The support destinations are printed beside it for accessibility and copy/paste.
    androidx.compose.foundation.Canvas(modifier.size(132.dp).clip(RoundedCornerShape(8.dp)).background(Color.White)) {
        val cells = 21
        val cell = size.minDimension / cells
        for (y in 0 until cells) for (x in 0 until cells) {
            val finder = (x < 7 && y < 7) || (x >= 14 && y < 7) || (x < 7 && y >= 14)
            val inside = ((x * 31 + y * 17 + x * y) % 5 == 0)
            if (finder || inside) drawRect(Color.Black, androidx.compose.ui.geometry.Offset(x * cell, y * cell), androidx.compose.ui.geometry.Size(cell, cell))
        }
    }
}

@Composable
private fun HelpSupportDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.help_support)) },
        text = { Text(stringResource(R.string.help_support_body)) },
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
