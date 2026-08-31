package es.pile.features.recycleBin.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonDefaults.smallContainerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pile.R
import es.pile.core.domain.models.TRASH_RETENTION_DAYS
import es.pile.core.ui.composables.DocumentCover
import es.pile.core.ui.composables.LoadingWrapper
import es.pile.core.ui.composables.formatFileSize
import es.pile.features.recycleBin.domain.models.TrashedDocument
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    modifier: Modifier = Modifier,
    popBackStack: () -> Unit,
    viewModel: RecycleBinViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bitmapCache by viewModel.bitmapCache.collectAsStateWithLifecycle()

    RecycleBinContent(
        modifier = modifier,
        state = state,
        bitmapCache = bitmapCache,
        onEvent = viewModel::handleEvent,
        popBackStack = popBackStack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecycleBinContent(
    modifier: Modifier = Modifier,
    state: RecycleBinState,
    bitmapCache: Map<String, Bitmap>,
    onEvent: (RecycleBinEvent) -> Unit,
    popBackStack: () -> Unit
) {
    var documentPendingDeletion by rememberSaveable { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { uiText ->
            snackbarHostState.showSnackbar(message = uiText.asString(context))
            onEvent(RecycleBinEvent.OnMessageDismissed)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                title = { Text(stringResource(R.string.recycle_bin)) },
                navigationIcon = {
                    FilledIconButton(
                        modifier = Modifier
                            .padding(start = 14.dp, end = 4.dp)
                            .size(smallContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)),
                        onClick = popBackStack
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24px),
                            contentDescription = stringResource(R.string.return_)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LoadingWrapper(state.isLoading) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 40.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    RetentionInfoCard()
                }

                if (state.trashedDocuments.isEmpty()) {
                    item {
                        RecycleBinEmptyState()
                    }
                } else {
                    items(
                        count = state.trashedDocuments.size,
                        key = { index -> state.trashedDocuments[index].document.id }
                    ) { index ->
                        val trashed = state.trashedDocuments[index]
                        val coverBitmap = bitmapCache[trashed.coverImageCacheKey]

                        if (coverBitmap == null) {
                            LaunchedEffect(trashed.coverImageCacheKey) {
                                onEvent(RecycleBinEvent.OnImageDisplayed(trashed.document))
                            }
                        }

                        TrashedDocumentRow(
                            trashedDocument = trashed,
                            sizeBytes = state.documentSizes[trashed.document.id],
                            coverBitmap = coverBitmap,
                            isLocked = trashed.document.id in state.lockedDocumentIds,
                            onRestore = { onEvent(RecycleBinEvent.OnRestore(trashed.document.id)) },
                            onDeleteForever = {
                                documentPendingDeletion = trashed.document.id
                            }
                        )
                    }
                }
            }
        }
    }

    documentPendingDeletion?.let { documentId ->
        AlertDeleteForever(
            onDismiss = { documentPendingDeletion = null },
            onConfirm = {
                documentPendingDeletion = null
                onEvent(RecycleBinEvent.OnDeleteForever(documentId))
            }
        )
    }
}

@Composable
private fun RetentionInfoCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_recycle_bin_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(28.dp)
            )

            Spacer(Modifier.width(14.dp))

            Text(
                text = stringResource(
                    R.string.recycle_bin_explanation,
                    TRASH_RETENTION_DAYS
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun TrashedDocumentRow(
    trashedDocument: TrashedDocument,
    sizeBytes: Long?,
    coverBitmap: Bitmap?,
    isLocked: Boolean,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DocumentCover(
                isPdf = trashedDocument.document.isIncomingPdf,
                coverBitmap = coverBitmap
            )

            Spacer(Modifier.width(14.dp))

            val title = if (trashedDocument.document.title.isBlank()) {
                stringResource(R.string.untitled_document)
            } else {
                trashedDocument.document.title
            }

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (isLocked) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = stringResource(R.string.document_locked),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = trashedDescription(trashedDocument),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = sizeBytes?.let(::formatFileSize)
                        ?: stringResource(R.string.size_unknown),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            IconButton(onClick = onRestore) {
                Icon(
                    imageVector = Icons.Filled.Restore,
                    contentDescription = stringResource(R.string.restore_document),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onDeleteForever) {
                Icon(
                    imageVector = Icons.Filled.DeleteForever,
                    contentDescription = stringResource(R.string.delete_forever),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun trashedDescription(trashedDocument: TrashedDocument): String {
    val deletedDate = trashedDocument.trashedAt.toLocalDate()
    val daysAgo = ChronoUnit.DAYS.between(deletedDate, LocalDate.now()).toInt()

    val deletedPart = if (daysAgo <= 0) {
        stringResource(R.string.deleted_today)
    } else {
        stringResource(R.string.deleted_days_ago, daysAgo)
    }

    val expiryDate = trashedDocument.trashedAt.toLocalDate()
        .plusDays(TRASH_RETENTION_DAYS.toLong())
    val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate).toInt()

    val expiryPart = if (daysLeft <= 0) {
        stringResource(R.string.deleted_forever_now)
    } else {
        stringResource(R.string.permanently_deleted_in_days, daysLeft)
    }

    return "$deletedPart · $expiryPart"
}

@Composable
private fun RecycleBinEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.ic_recycle_bin_24px),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
            )
            Text(
                text = stringResource(R.string.recycle_bin_empty),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun AlertDeleteForever(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        icon = {
            Icon(
                painter = painterResource(R.drawable.warning_24px),
                contentDescription = null
            )
        },
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_forever_alert_title)) },
        text = { Text(stringResource(R.string.delete_forever_alert_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.delete_forever),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
