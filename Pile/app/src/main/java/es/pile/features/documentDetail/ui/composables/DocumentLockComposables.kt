package es.pile.features.documentDetail.ui.composables

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Pattern
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.pile.R
import es.pile.core.domain.models.DocumentLockType
import es.pile.core.domain.util.PatternLockUtils
import es.pile.core.ui.composables.DOCUMENT_PIN_LENGTH
import es.pile.core.ui.composables.PatternLock
import es.pile.core.ui.composables.PinDots
import es.pile.core.ui.composables.PinPad
import kotlinx.coroutines.launch

/** Icon and label describing each kind of document lock. */
private fun DocumentLockType.presentation(): Pair<ImageVector, Int> = when (this) {
    DocumentLockType.PIN -> Icons.Filled.Password to R.string.pin_lock
    DocumentLockType.PATTERN -> Icons.Filled.Pattern to R.string.pattern_lock
}

/**
 * Full screen shown instead of the document content while a protected document
 * has not been unlocked yet. Asks for a PIN or for a pattern depending on
 * [lockType].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentLockContent(
    modifier: Modifier = Modifier,
    lockType: DocumentLockType,
    onUnlock: suspend (secret: String) -> Boolean,
    popBackStack: () -> Unit
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var isError by rememberSaveable { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var patternResetKey by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()

    fun onVerificationFinished(unlocked: Boolean) {
        isVerifying = false

        if (!unlocked) {
            isError = true
            pin = ""
            patternResetKey++
        }
    }

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
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
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
                text = when {
                    isError && lockType == DocumentLockType.PATTERN ->
                        stringResource(R.string.incorrect_pattern)

                    isError -> stringResource(R.string.incorrect_pin)

                    lockType == DocumentLockType.PATTERN ->
                        stringResource(R.string.locked_document_pattern_body)

                    else -> stringResource(R.string.locked_document_body)
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

            if (lockType == DocumentLockType.PATTERN) {
                PatternLock(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(vertical = 12.dp),
                    enabled = !isVerifying,
                    isError = isError,
                    resetKey = patternResetKey,
                    onPatternEntered = { pattern ->
                        isVerifying = true
                        scope.launch {
                            val unlocked = onUnlock(PatternLockUtils.encode(pattern))
                            onVerificationFinished(unlocked)
                        }
                    }
                )
            } else {
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
                            onVerificationFinished(unlocked)
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
}

/**
 * Dialog shown before protecting a document, so the user can choose between a
 * PIN and a draw pattern.
 */
@Composable
fun ChooseDocumentLockTypeDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onLockTypeChosen: (type: DocumentLockType) -> Unit
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Filled.Lock, contentDescription = null) },
        title = { Text(stringResource(R.string.choose_lock_type)) },
        text = {
            Text(
                text = stringResource(R.string.choose_lock_type_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DocumentLockType.entries.forEach { type ->
                    val (icon, label) = type.presentation()

                    TextButton(
                        onClick = { onLockTypeChosen(type) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = icon, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(label))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
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
 * Two step dialog used to protect a document with a new draw pattern.
 */
@Composable
fun SetDocumentPatternDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onConfirm: (pattern: String) -> Unit
) {
    var pattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isError by remember { mutableStateOf(false) }
    var resetKey by remember { mutableIntStateOf(0) }

    fun resetPattern() {
        isError = false
        pattern = emptyList()
        resetKey++
    }

    val isConfirmationStep = PatternLockUtils.isValidLength(pattern)

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Filled.Lock, contentDescription = null) },
        title = {
            Text(
                if (isConfirmationStep) {
                    stringResource(R.string.confirm_pattern)
                } else {
                    stringResource(R.string.lock_with_pattern)
                }
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isError) {
                        stringResource(R.string.patterns_do_not_match)
                    } else if (isConfirmationStep) {
                        stringResource(R.string.confirm_pattern_body)
                    } else {
                        stringResource(
                            R.string.set_pattern_body,
                            PatternLockUtils.MIN_PATTERN_LENGTH
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                PatternLock(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(vertical = 12.dp),
                    isError = isError,
                    resetKey = resetKey,
                    onPatternEntered = { entered ->
                        if (!isConfirmationStep) {
                            pattern = entered
                            resetKey++
                        } else if (PatternLockUtils.matches(entered, pattern)) {
                            onConfirm(PatternLockUtils.encode(entered))
                        } else {
                            isError = true
                            pattern = emptyList()
                            resetKey++
                        }
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = {
                resetPattern()
                onDismiss()
            }) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Dialog asking for the current PIN or pattern before removing the protection
 * of a document.
 */
@Composable
fun RemoveDocumentLockDialog(
    modifier: Modifier = Modifier,
    lockType: DocumentLockType,
    onDismiss: () -> Unit,
    onConfirm: suspend (secret: String) -> Boolean
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var isError by rememberSaveable { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var patternResetKey by remember { mutableIntStateOf(0) }

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
                        if (lockType == DocumentLockType.PATTERN) {
                            stringResource(R.string.incorrect_pattern)
                        } else {
                            stringResource(R.string.incorrect_pin)
                        }
                    } else if (lockType == DocumentLockType.PATTERN) {
                        stringResource(R.string.remove_document_pattern_body)
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

                if (lockType == DocumentLockType.PATTERN) {
                    PatternLock(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .padding(vertical = 8.dp),
                        enabled = !isVerifying,
                        isError = isError,
                        resetKey = patternResetKey,
                        onPatternEntered = { pattern ->
                            isVerifying = true
                            scope.launch {
                                val removed = onConfirm(PatternLockUtils.encode(pattern))
                                isVerifying = false

                                if (!removed) {
                                    isError = true
                                    patternResetKey++
                                }
                            }
                        }
                    )
                } else {
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
