package es.pile.core.ui.composables

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tanishranjan.cropkit.CropDefaults
import com.tanishranjan.cropkit.CropShape
import com.tanishranjan.cropkit.GridLinesType
import com.tanishranjan.cropkit.GridLinesVisibility
import com.tanishranjan.cropkit.ImageCropper
import com.tanishranjan.cropkit.rememberCropController
import es.pile.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Accent color of the crop frame, handles and gridlines. */
private val CROP_ACCENT_COLOR = Color(0xFF2979FF)

/**
 * Full screen cropper used to adjust a picture before it becomes an avatar
 * (for example the profile picture of a hub).
 *
 * The crop frame is locked to a square with a circular guide, so what the user
 * sees inside the circle is exactly what the round avatar will show. The
 * picture can also be rotated in 90º steps before cropping.
 *
 * @param bitmap Picture to adjust, already decoded and EXIF rotated.
 * @param title Title shown in the toolbar.
 * @param onDismiss Called when the user cancels: nothing is saved.
 * @param onConfirm Called with the cropped bitmap when the user confirms.
 */
@Composable
fun PictureCropperDialog(
    bitmap: Bitmap,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Bitmap) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isCropping by remember { mutableStateOf(false) }

    val cropColors = remember {
        CropDefaults.cropColors(
            overlay = Color.Black.copy(alpha = 0.7f),
            overlayActive = Color.Black.copy(alpha = 0.4f),
            gridlines = CROP_ACCENT_COLOR.copy(alpha = 0.8f),
            cropRectangle = CROP_ACCENT_COLOR,
            handle = CROP_ACCENT_COLOR
        )
    }

    val cropOptions = remember {
        CropDefaults.cropOptions(
            // Square crop with a circular guide: hub avatars are round.
            cropShape = CropShape.AspectRatio(1f),
            contentScale = ContentScale.Fit,
            gridLinesVisibility = GridLinesVisibility.ALWAYS,
            gridLinesType = GridLinesType.CIRCLE
        )
    }

    val cropController = rememberCropController(
        bitmap = bitmap,
        cropOptions = cropOptions,
        cropColors = cropColors
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cancel)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )

                IconButton(onClick = { cropController.rotateAntiClockwise() }) {
                    Icon(
                        painter = painterResource(R.drawable.rotate_24px),
                        contentDescription = stringResource(R.string.rotate_left)
                    )
                }

                IconButton(onClick = { cropController.rotateClockwise() }) {
                    Icon(
                        painter = painterResource(R.drawable.rotate_24px),
                        contentDescription = stringResource(R.string.rotate_right),
                        // The same icon mirrored: rotation to the other side.
                        modifier = Modifier.scale(scaleX = -1f, scaleY = 1f)
                    )
                }
            }

            ImageCropper(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                cropController = cropController
            )

            Text(
                text = stringResource(R.string.crop_picture_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }

                Button(
                    enabled = !isCropping,
                    onClick = {
                        if (isCropping) return@Button

                        isCropping = true
                        scope.launch {
                            val cropped = withContext(Dispatchers.Default) {
                                runCatching { cropController.crop() }.getOrNull()
                            }

                            isCropping = false
                            if (cropped != null) onConfirm(cropped) else onDismiss()
                        }
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(stringResource(R.string.crop_picture_action))
                }
            }
        }
    }
}
