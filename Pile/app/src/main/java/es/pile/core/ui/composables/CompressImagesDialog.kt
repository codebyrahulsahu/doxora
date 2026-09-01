package es.pile.core.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.pile.R
import es.pile.core.domain.models.ImageCompressionChoice
import es.pile.core.ui.theme.PileTheme
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Document Resizer prompt shown right after images are picked, no matter where
 * they come from (gallery, device folders, camera or scanner).
 *
 * The dialog is driven by a single ON/OFF switch: when the resizer is on, the
 * maximum size per image can be chosen (presets or a custom KB/MB value); when
 * it is off the images are imported exactly as they are.
 *
 * The switch starts on the value stored in the Document Resizer settings, and
 * the answer given here wins for this import (and is remembered as the new
 * default by the caller).
 *
 * @param imageCount How many images are about to be imported.
 * @param defaultChoice Pre-selected compression choice (from the settings).
 * @param onDismiss Called when the prompt is cancelled: nothing is imported.
 * @param onConfirm Called with the final choice; the import continues with it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompressImagesDialog(
    imageCount: Int,
    defaultChoice: ImageCompressionChoice,
    onDismiss: () -> Unit,
    onConfirm: (ImageCompressionChoice) -> Unit
) {
    var compress by rememberSaveable { mutableStateOf(defaultChoice.compress) }

    val defaultIsPreset = defaultChoice.targetSizeKb in ImageCompressionChoice.PRESET_SIZES_KB
    // 0 means "custom size" (the text field below is used instead of a preset).
    var selectedPresetKb by rememberSaveable {
        mutableStateOf(if (defaultIsPreset) defaultChoice.targetSizeKb else 0)
    }
    var customUnitIsMb by rememberSaveable { mutableStateOf(defaultChoice.targetSizeKb >= 1024) }
    var customText by rememberSaveable {
        mutableStateOf(
            if (defaultIsPreset) "" else formatSizeForInput(defaultChoice.targetSizeKb, customUnitIsMb)
        )
    }

    val customSizeKb = parseSizeToKb(customText, customUnitIsMb)
    val resolvedTargetKb = if (selectedPresetKb > 0) selectedPresetKb else customSizeKb
    val confirmEnabled = !compress || resolvedTargetKb != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.document_resizer_switch_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.document_resizer_prompt_body, imageCount),
                    style = MaterialTheme.typography.bodyMedium
                )

                DocumentResizerSwitchRow(
                    checked = compress,
                    onCheckedChange = { compress = it }
                )

                if (compress) {
                    Text(
                        text = stringResource(R.string.document_resizer_max_size),
                        style = MaterialTheme.typography.titleSmall
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ImageCompressionChoice.PRESET_SIZES_KB.forEach { presetKb ->
                            FilterChip(
                                selected = selectedPresetKb == presetKb,
                                onClick = { selectedPresetKb = presetKb },
                                label = { Text(formatSizeLabel(presetKb)) }
                            )
                        }
                        FilterChip(
                            selected = selectedPresetKb == 0,
                            onClick = { selectedPresetKb = 0 },
                            label = { Text(stringResource(R.string.compression_custom)) }
                        )
                    }

                    if (selectedPresetKb == 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customText,
                                onValueChange = { customText = it },
                                label = {
                                    Text(
                                        stringResource(
                                            if (customUnitIsMb) R.string.size_in_mb
                                            else R.string.size_in_kb
                                        )
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                isError = customText.isNotBlank() && customSizeKb == null,
                                modifier = Modifier.weight(1f)
                            )

                            FilterChip(
                                selected = !customUnitIsMb,
                                onClick = { customUnitIsMb = false },
                                label = { Text(stringResource(R.string.kb)) }
                            )
                            FilterChip(
                                selected = customUnitIsMb,
                                onClick = { customUnitIsMb = true },
                                label = { Text(stringResource(R.string.mb)) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = confirmEnabled,
                onClick = {
                    onConfirm(
                        if (compress) {
                            ImageCompressionChoice(
                                compress = true,
                                targetSizeKb = resolvedTargetKb
                                    ?: ImageCompressionChoice.DEFAULT_TARGET_SIZE_KB
                            )
                        } else {
                            ImageCompressionChoice.original()
                        }
                    )
                }
            ) {
                Text(stringResource(R.string.import_images_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/** ON/OFF switch that enables or disables the Document Resizer for this import. */
@Composable
private fun DocumentResizerSwitchRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val title = stringResource(R.string.document_resizer_switch_title)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(
                    if (checked) R.string.document_resizer_switch_on
                    else R.string.document_resizer_switch_off
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { contentDescription = title }
        )
    }
}

/** Formats a size in KB as a short label ("512 KB", "1 MB", "1.5 MB"). */
private fun formatSizeLabel(sizeKb: Int): String {
    if (sizeKb < 1024) return "$sizeKb KB"

    return if (sizeKb % 1024 == 0) {
        "${sizeKb / 1024} MB"
    } else {
        String.format(Locale.US, "%.1f MB", sizeKb / 1024f)
    }
}

/** Formats a size in KB as plain text for the custom size field. */
private fun formatSizeForInput(sizeKb: Int, asMb: Boolean): String {
    if (!asMb) return sizeKb.toString()

    val mb = sizeKb / 1024f
    return if (mb % 1f == 0f) mb.toInt().toString() else String.format(Locale.US, "%.2f", mb)
}

/** Parses the typed custom size into KB, or null when the value is not valid. */
private fun parseSizeToKb(text: String, isMb: Boolean): Int? {
    val value = text.trim().toFloatOrNull() ?: return null
    if (value <= 0f) return null

    val kb = if (isMb) value * 1024f else value
    return kb.roundToInt().coerceAtLeast(1)
}

@Preview
@Composable
private fun CompressImagesDialogPreview() {
    PileTheme {
        CompressImagesDialog(
            imageCount = 3,
            defaultChoice = ImageCompressionChoice(compress = true, targetSizeKb = 512),
            onDismiss = {},
            onConfirm = {}
        )
    }
}
