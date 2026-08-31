package es.pile.features.settings.ui.composables

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.pile.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    val placeholderIcon: ImageVector = Icons.Filled.Person

    val imageBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = profilePictureFile
    ) {
        value = profilePictureFile?.let { file ->
            withContext(Dispatchers.IO) {
                runCatching { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }
                    .getOrNull()
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = imageBitmap

        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = stringResource(R.string.profile_picture),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size)
            )
        } else {
            Icon(
                imageVector = placeholderIcon,
                contentDescription = stringResource(R.string.profile_picture),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size * 0.54f)
            )
        }
    }
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
