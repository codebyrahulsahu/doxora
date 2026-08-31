package es.pile.core.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import es.pile.core.domain.util.PatternLockUtils
import kotlin.math.hypot

/**
 * A 3x3 pattern lock pad, like the Android system one.
 *
 * The user draws a pattern by dragging a finger over the dots. Every dot crossed
 * on the way is connected automatically. When the finger is lifted and the
 * pattern has enough dots [onPatternEntered] is called with the connected dots
 * in order; otherwise the pad is cleared and the user can try again.
 *
 * @param enabled False to ignore every gesture (e.g. while verifying).
 * @param isError True to paint the pad with the error color.
 * @param resetKey When this value changes the pad is cleared.
 * @param onPatternEntered Called with the connected dots when a valid pattern is drawn.
 * @param onPatternProgress Called with the connected dots while drawing.
 */
@Composable
fun PatternLock(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    resetKey: Any? = null,
    onPatternEntered: (pattern: List<Int>) -> Unit,
    onPatternProgress: (pattern: List<Int>) -> Unit = {}
) {
    val selected = remember { mutableStateListOf<Int>() }
    var pointerPosition by remember { mutableStateOf(Offset.Zero) }
    var isDrawing by remember { mutableStateOf(false) }

    val selectedColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    val idleColor = if (isError) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    val lineColor = selectedColor.copy(alpha = if (enabled) 1f else 0.4f)

    fun addDotAt(position: Offset, canvasWidth: Float) {
        hitTest(position, canvasWidth)?.let { dot ->
            PatternLockUtils.connect(selected.toList(), dot)?.let { newSelection ->
                selected.clear()
                selected.addAll(newSelection)
                onPatternProgress(newSelection)
            }
        }
    }

    LaunchedEffect(resetKey) {
        selected.clear()
        isDrawing = false
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                detectDragGestures(
                    onDragStart = { start ->
                        selected.clear()
                        isDrawing = true
                        pointerPosition = start

                        addDotAt(start, size.width.toFloat())
                    },
                    onDrag = { change, _ ->
                        pointerPosition += change.positionChange()
                        change.consume()

                        addDotAt(pointerPosition, size.width.toFloat())
                    },
                    onDragEnd = {
                        isDrawing = false

                        if (PatternLockUtils.isValidLength(selected.toList())) {
                            onPatternEntered(selected.toList())
                        } else {
                            selected.clear()
                            onPatternProgress(emptyList())
                        }
                    },
                    onDragCancel = {
                        isDrawing = false
                        selected.clear()
                        onPatternProgress(emptyList())
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cell = this.size.width / PatternLockUtils.GRID_SIZE
            val dotRadius = cell * 0.09f

            fun centerOf(dot: Int): Offset = Offset(
                x = ((dot % PatternLockUtils.GRID_SIZE) + 0.5f) * cell,
                y = ((dot / PatternLockUtils.GRID_SIZE) + 0.5f) * cell
            )

            // Lines connecting the selected dots (and the finger while drawing)
            val points = selected.map(::centerOf).toMutableList()
            if (isDrawing && selected.isNotEmpty()) {
                points.add(
                    pointerPosition.coerceInCanvas(this.size.width, this.size.height)
                )
            }

            for (i in 0 until (points.size - 1).coerceAtLeast(0)) {
                drawLine(
                    color = lineColor,
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = dotRadius,
                    cap = StrokeCap.Round
                )
            }

            repeat(PatternLockUtils.DOT_COUNT) { dot ->
                val center = centerOf(dot)
                val isSelected = dot in selected

                if (isSelected) {
                    drawCircle(
                        color = selectedColor.copy(alpha = 0.2f),
                        radius = dotRadius * 2f,
                        center = center
                    )
                    drawCircle(color = selectedColor, radius = dotRadius, center = center)
                } else {
                    drawCircle(
                        color = idleColor,
                        radius = dotRadius,
                        center = center,
                        style = Stroke(width = dotRadius * 0.5f)
                    )
                }
            }
        }
    }
}

/** Returns the dot under [position], or null when the finger is not over any dot. */
private fun hitTest(position: Offset, canvasWidth: Float): Int? {
    val cell = canvasWidth / PatternLockUtils.GRID_SIZE
    if (cell <= 0f) return null

    val col = (position.x / cell).toInt().coerceIn(0, PatternLockUtils.GRID_SIZE - 1)
    val row = (position.y / cell).toInt().coerceIn(0, PatternLockUtils.GRID_SIZE - 1)

    val center = Offset((col + 0.5f) * cell, (row + 0.5f) * cell)
    val distance = hypot(position.x - center.x, position.y - center.y)

    return if (distance <= cell * 0.5f) {
        row * PatternLockUtils.GRID_SIZE + col
    } else {
        null
    }
}

private fun Offset.coerceInCanvas(canvasWidth: Float, canvasHeight: Float): Offset = Offset(
    x = x.coerceIn(0f, canvasWidth),
    y = y.coerceIn(0f, canvasHeight)
)
