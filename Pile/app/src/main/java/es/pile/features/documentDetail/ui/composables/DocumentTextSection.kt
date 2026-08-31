package es.pile.features.documentDetail.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import es.pile.R

/**
 * Shows the text that has been recognized (OCR) from the document.
 *
 * The recognized text lives inside a [SelectionContainer] so it can be selected and
 * copied, and it can also be edited and saved back, which makes it easy to fix the
 * small mistakes every recognizer eventually makes.
 */
@Composable
fun DocumentTextSection(
    modifier: Modifier = Modifier,
    recognizedText: String?,
    isRecognizing: Boolean,
    onRecognizeText: () -> Unit,
    onTextChanged: (String) -> Unit,
    onDeleteText: () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionTitleBar(
            title = stringResource(R.string.recognized_text),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            when {
                isRecognizing -> RecognizingTextContent()

                recognizedText.isNullOrBlank() -> EmptyRecognizedTextContent(
                    onRecognizeText = onRecognizeText
                )

                else -> RecognizedTextContent(
                    text = recognizedText,
                    onTextChanged = onTextChanged,
                    onRecognizeText = onRecognizeText,
                    onDeleteText = onDeleteText
                )
            }
        }
    }
}

@Composable
private fun RecognizingTextContent(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.5.dp
        )
        Text(
            text = stringResource(R.string.recognizing_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyRecognizedTextContent(
    modifier: Modifier = Modifier,
    onRecognizeText: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.recognized_text_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        Button(onClick = onRecognizeText) {
            Text(stringResource(R.string.recognize_text))
        }
    }
}

@Composable
private fun RecognizedTextContent(
    modifier: Modifier = Modifier,
    text: String,
    onTextChanged: (String) -> Unit,
    onRecognizeText: () -> Unit,
    onDeleteText: () -> Unit
) {
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var editedText by rememberSaveable(key = text) { mutableStateOf(text) }

    val clipboardManager = LocalClipboardManager.current

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (isEditing) {
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .height(220.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            } else {
                SelectionContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditing) {
                TextButton(onClick = {
                    onTextChanged(editedText)
                    isEditing = false
                }) {
                    Text(stringResource(R.string.save))
                }

                TextButton(onClick = {
                    editedText = text
                    isEditing = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            } else {
                TextButton(onClick = { isEditing = true }) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.edit))
                }

                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(text))
                }) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.copy))
                }
            }

            Spacer(Modifier.weight(1f))

            if (!isEditing) {
                TextButton(onClick = onRecognizeText) {
                    Text(stringResource(R.string.recognize_again))
                }

                TextButton(onClick = onDeleteText) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete_text),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
