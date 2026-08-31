package es.pile.features.settings.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.pile.R
import es.pile.core.ui.composables.PictureAvatar
import java.io.File

private val DEFAULT_AVATAR_SIZE = 56.dp

/**
 * Circular avatar showing the user profile picture when there is one, or a person
 * placeholder otherwise.
 *
 * The image is decoded off the main thread.
 *
 * @param profilePictureFile The stored picture file, or null to show the placeholder.
 * @param onClick Called when the avatar is tapped. Null to disable the click.
 */
@Composable
fun ProfilePictureAvatar(
    profilePictureFile: File?,
    modifier: Modifier = Modifier,
    size: Dp = DEFAULT_AVATAR_SIZE,
    onClick: (() -> Unit)? = null,
) {
    PictureAvatar(
        pictureFile = profilePictureFile,
        contentDescription = stringResource(R.string.profile_picture),
        modifier = modifier,
        size = size,
        placeholderIcon = Icons.Filled.Person,
        onClick = onClick
    )
}

/**
 * Small round "edit" badge shown at the bottom end of the profile avatar.
 */
@Composable
fun ProfilePictureEditBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = stringResource(R.string.change_profile_picture),
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(12.dp)
        )
    }
}
