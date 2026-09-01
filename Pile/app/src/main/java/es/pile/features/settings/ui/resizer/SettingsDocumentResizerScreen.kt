package es.pile.features.settings.ui.resizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pile.R
import es.pile.core.ui.composables.LoadingWrapper
import es.pile.core.ui.theme.PileTheme
import es.pile.features.settings.ui.composables.ItemPosition
import es.pile.features.settings.ui.composables.SettingsItem
import es.pile.features.settings.ui.composables.SettingsTopBar
import kotlin.math.roundToInt
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun SettingsDocumentResizerScreen(
    viewModel: SettingsDocumentResizerViewModel = koinViewModel(),
    popBackStack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsDocumentResizerContent(
        state = state,
        onEvent = { event ->
            when (event) {
                is SettingsDocumentResizerEvent.OnBackClicked -> popBackStack()
                else -> viewModel.handleEvent(event)
            }
        }
    )
}

@Preview
@Composable
private fun SettingsDocumentResizerPrev() {
    PileTheme {
        SettingsDocumentResizerContent(
            state = SettingsDocumentResizerState(
                isLoading = false,
                isEnabled = true,
                targetSizeKb = 512
            ),
            onEvent = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDocumentResizerContent(
    modifier: Modifier = Modifier,
    state: SettingsDocumentResizerState,
    onEvent: (SettingsDocumentResizerEvent) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.displayCutout,
        topBar = {
            SettingsTopBar(
                title = stringResource(R.string.document_resizer),
                popBackStack = { onEvent(SettingsDocumentResizerEvent.OnBackClicked) },
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
                    .padding(top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SettingsItem(
                    itemPosition = ItemPosition.SINGLE,
                    title = stringResource(R.string.document_resizer_toggle_title),
                    subtitle = stringResource(R.string.document_resizer_toggle_body),
                    checked = state.isEnabled,
                    onAction = { onEvent(SettingsDocumentResizerEvent.OnEnabledToggled) }
                )

                DocumentResizerTargetSizeCard(
                    targetSizeKb = state.targetSizeKb,
                    onTargetSizeChanged = { sizeKb ->
                        onEvent(SettingsDocumentResizerEvent.OnTargetSizeChanged(sizeKb))
                    }
                )
            }
        }
    }
}

/** Units accepted by the custom target size field. */
private enum class SizeUnit { KB, MB }

@Composable
private fun DocumentResizerTargetSizeCard(
    targetSizeKb: Int,
    onTargetSizeChanged: (Int) -> Unit
) {
    var unit by rememberSaveable { mutableStateOf(SizeUnit.KB) }
    var text by rememberSaveable { mutableStateOf("") }
    var userEdited by rememberSaveable { mutableStateOf(false) }

    // Keep the field in sync with the stored value until the user starts typing.
    LaunchedEffect(targetSizeKb) {
        if (!userEdited) {
            text = formatSizeForUnit(targetSizeKb, unit)
        }
    }

    // Commit every valid value as soon as it is typed.
    LaunchedEffect(text, unit) {
        val parsed = parseToKb(text, unit)
        if (parsed != null && parsed != targetSizeKb) {
            onTargetSizeChanged(parsed)
        }
    }

    /**
     * Switches the [SizeUnit] keeping the entered value roughly the same:
     * e.g. 512 KB becomes 0.5 MB and vice versa.
     */
    val onUnitSelected: (SizeUnit) -> Unit = { newUnit ->
        if (newUnit != unit) {
            val currentKb = parseToKb(text, unit) ?: targetSizeKb
            unit = newUnit
            text = formatSizeForUnit(currentKb, newUnit)
            userEdited = true
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.document_resizer_target_size),
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { value ->
                        userEdited = true
                        text = value
                    },
                    label = {
                        Text(
                            stringResource(
                                if (unit == SizeUnit.KB) R.string.size_in_kb
                                else R.string.size_in_mb
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = unit == SizeUnit.KB,
                    onClick = { onUnitSelected(SizeUnit.KB) },
                    label = { Text(stringResource(R.string.kb)) }
                )

                FilterChip(
                    selected = unit == SizeUnit.MB,
                    onClick = { onUnitSelected(SizeUnit.MB) },
                    label = { Text(stringResource(R.string.mb)) }
                )
            }

            Text(
                text = stringResource(
                    R.string.document_resizer_target_size_body,
                    formatDocumentResizerTargetSize(targetSizeKb)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = stringResource(R.string.document_resizer_size_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Parses the typed text into a size in KB, or null when the value is not valid. */
private fun parseToKb(text: String, unit: SizeUnit): Int? {
    val value = text.trim().toFloatOrNull() ?: return null
    if (value <= 0f) return null

    val kb = when (unit) {
        SizeUnit.KB -> value
        SizeUnit.MB -> value * 1024f
    }
    return kb.roundToInt()
}

/** Formats a size in KB to plain text for the input field, using the given unit. */
private fun formatSizeForUnit(sizeKb: Int, unit: SizeUnit): String = when (unit) {
    SizeUnit.KB -> sizeKb.toString()
    SizeUnit.MB -> {
        val mb = sizeKb / 1024f
        if (mb % 1f == 0f) {
            mb.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", mb)
        }
    }
}
