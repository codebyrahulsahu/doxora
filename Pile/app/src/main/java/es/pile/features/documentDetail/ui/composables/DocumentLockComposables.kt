package es.pile.features.documentDetail.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonDefaults.smallContainerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.pile.R
import es.pile.core.ui.composables.DOCUMENT_PIN_LENGTH
import es.pile.core.ui.composables.PinDots
import es.pile.core.ui.composables.PinPad
import kotlinx.coroutines.launch

/**
 * Full screen shown instead of the document content while a PIN protected document
 * has not been unlocked yet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentLockContent(
    modifier: Modifier = Modifier,
    onUnlock: suspend (pin: String) -> Boolean,
    popBackStack: () -> Unit
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var isError by rememberSaveable { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { },
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.locked_document_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (isError) {
                    stringResource(R.string.incorrect_pin)
                } else {
                    stringResource(R.string.locked_document_body)
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(Modifier.height(24.dp))

            PinDots(
                pinLength = pin.length,
                totalDigits = DOCUMENT_PIN_LENGTH,
                isError = isError
            )

            Spacer(Modifier.height(32.dp))

            PinPad(
                enabled = !isVerifying,
                onDigit = { digit ->
                    if (pin.length >= DOCUMENT_PIN_LENGTH) return@PinPad

                    val newPin = pin + digit
                    pin = newPin

                    if (newPin.length < DOCUMENT_PIN_LENGTH) return@PinPad

                    isVerifying = true
                    scope.launch {
                        val unlocked = onUnlock(newPin)
                        isVerifying = false

                        if (!unlocked) {
                            isError = true
                            pin = ""
                        }
                    }
                },
                onBackspace = {
                    isError = false
                    pin = pin.dropLast(1)
                }
            )
        }
    }
}

/**
 * Two step dialog used to protect a document with a new PIN.
 */
@Composable
fun SetDocumentPinDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onConfirm: (pin: String) -> Unit
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var isError by rememberSaveable { mutableStateOf(false) }

    val isConfirmationStep = pin.length == DOCUMENT_PIN_LENGTH

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Filled.Lock, contentDescription = null) },
        title = {
            Text(
                if (isConfirmationStep) {
                    stringResource(R.string.confirm_pin)
                } else {
                    stringResource(R.string.lock_document)
                }
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isError) {
                        stringResource(R.string.pins_do_not_match)
                    } else {
                        stringResource(R.string.set_pin_body, DOCUMENT_PIN_LENGTH)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(Modifier.height(16.dp))

                PinDots(
                    pinLength = if (isConfirmationStep) confirmation.length else pin.length,
                    totalDigits = DOCUMENT_PIN_LENGTH,
                    isError = isError
                )

                Spacer(Modifier.height(16.dp))

                PinPad(
                    onDigit = { digit ->
                        isError = false

                        if (!isConfirmationStep) {
                            if (pin.length < DOCUMENT_PIN_LENGTH) pin += digit
                        } else if (confirmation.length < DOCUMENT_PIN_LENGTH) {
                            val newConfirmation = confirmation + digit
                            confirmation = newConfirmation

                            if (newConfirmation.length == DOCUMENT_PIN_LENGTH) {
                                if (newConfirmation == pin) {
                                    onConfirm(newConfirmation)
                                } else {
                                    isError = true
                                    pin = ""
                                    confirmation = ""
                                }
                            }
                        }
                    },
                    onBackspace = {
                        isError = false

                        if (isConfirmationStep) {
                            confirmation = confirmation.dropLast(1)
                        } else {
                            pin = pin.dropLast(1)
                        }
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Dialog asking for the current PIN before removing the protection of a document.
 */
@Composable
fun RemoveDocumentPinDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onConfirm: suspend (pin: String) -> Boolean
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var isError by rememberSaveable { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Filled.Lock, contentDescription = null) },
        title = { Text(stringResource(R.string.remove_document_lock)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isError) {
                        stringResource(R.string.incorrect_pin)
                    } else {
                        stringResource(R.string.remove_document_lock_body)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(Modifier.height(16.dp))

                PinDots(
                    pinLength = pin.length,
                    totalDigits = DOCUMENT_PIN_LENGTH,
                    isError = isError
                )

                Spacer(Modifier.height(16.dp))

                PinPad(
                    enabled = !isVerifying,
                    onDigit = { digit ->
                        if (pin.length >= DOCUMENT_PIN_LENGTH) return@PinPad

                        val newPin = pin + digit
                        pin = newPin

                        if (newPin.length < DOCUMENT_PIN_LENGTH) return@PinPad

                        isVerifying = true
                        scope.launch {
                            val removed = onConfirm(newPin)
                            isVerifying = false

                            if (!removed) {
                                isError = true
                                pin = ""
                            }
                        }
                    },
                    onBackspace = {
                        isError = false
                        pin = pin.dropLast(1)
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}
