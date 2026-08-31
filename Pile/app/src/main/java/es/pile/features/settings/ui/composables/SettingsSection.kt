package es.pile.features.settings.ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.pile.R
import es.pile.core.ui.theme.PileTheme

@Preview
@Composable
private fun SettingsSectionPrev() {
    PileTheme {
        Surface() {
            SettingsSection(
                title = "Appearance",
                modifier = Modifier.padding(16.dp)
            ) {
                SettingsItem(
                    itemPosition = ItemPosition.SINGLE,
                    title = "Use system theme color",
                    onAction = {}
                )
            }
        }
    }
}

@Preview
@Composable
private fun CollapsibleSettingsSectionPrev() {
    PileTheme {
        Surface() {
            SettingsSection(
                title = "About & Support",
                collapsible = true,
                modifier = Modifier.padding(16.dp)
            ) {
                SettingsItem(
                    itemPosition = ItemPosition.SINGLE,
                    title = "About Doxora",
                    onAction = {}
                )
            }
        }
    }
}

/**
 * A titled group of settings items.
 *
 * When [collapsible] is true the content stays hidden and is only revealed
 * after the section header is tapped (it collapses again on the next tap).
 *
 * @param title Header of the section.
 * @param collapsible Whether the content can be hidden/revealed by tapping the header.
 * @param initiallyExpanded Initial state of a collapsible section.
 */
@Composable
fun SettingsSection(
    modifier: Modifier = Modifier,
    title: String,
    collapsible: Boolean = false,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit = {}
) {
    var isExpanded by rememberSaveable(collapsible) {
        mutableStateOf(if (collapsible) initiallyExpanded else true)
    }

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "sectionChevronRotation"
    )

    val expandedDescription = stringResource(R.string.expanded)
    val collapsedDescription = stringResource(R.string.collapsed)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .then(
                    if (collapsible) {
                        Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { isExpanded = !isExpanded }
                            .semantics {
                                stateDescription = if (isExpanded) {
                                    expandedDescription
                                } else {
                                    collapsedDescription
                                }
                            }
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )

            if (collapsible) {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronRotation)
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                content()
            }
        }
    }
}
