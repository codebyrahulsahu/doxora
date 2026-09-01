package es.pile.core.domain.models

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Helpers for the custom target file size typed by the user in the Document
 * Resizer prompt.
 *
 * The prompt accepts a numeric value in [Unit.KB] or [Unit.MB]; everything is
 * converted to kilobytes before it is passed to the compression pipeline.
 */
object DocumentResizeTargetSize {
    /** Smallest target size accepted by the Document Resizer, in KB. */
    const val MIN_KB = 16

    /**
     * Number of kilobytes in one megabyte. Uses the same 1024-based unit as
     * the stored-file math (`targetSizeKb * 1024` bytes).
     */
    const val KB_PER_MB = 1024

    /** Unit shown next to the target size input field. */
    enum class Unit { KB, MB }

    /**
     * Parses [input] as a positive size in [unit] and returns the equivalent
     * size in kilobytes, or null when the text is empty or not a number.
     *
     * Both `.` and `,` are accepted as the decimal separator.
     */
    fun parse(input: String, unit: Unit): Int? {
        val normalized = input.trim().replace(',', '.')
        if (normalized.isEmpty()) return null

        val value = normalized.toDoubleOrNull() ?: return null
        if (value <= 0.0 || value.isNaN() || value.isInfinite()) return null

        val sizeKb = when (unit) {
            Unit.KB -> value.roundToInt()
            Unit.MB -> (value * KB_PER_MB).roundToInt()
        }
        return sizeKb.takeIf { it > 0 }
    }

    /** True when [sizeKb] is large enough for the Document Resizer. */
    fun isValid(sizeKb: Int): Boolean = sizeKb >= MIN_KB

    /**
     * Unit that best matches a stored size in kilobytes when pre-filling the
     * input field: MB when the value is at least 1 MB, KB otherwise.
     */
    fun preferredUnit(sizeKb: Int): Unit =
        if (sizeKb >= KB_PER_MB) Unit.MB else Unit.KB

    /** Formats [sizeKb] for the input field in the given [unit]. */
    fun displayValue(sizeKb: Int, unit: Unit): String = when (unit) {
        Unit.KB -> sizeKb.toString()
        Unit.MB -> formatMegabytes(sizeKb.toDouble() / KB_PER_MB)
    }

    /**
     * Converts the text currently shown in the input when the user toggles
     * between KB and MB. Invalid text is returned unchanged.
     */
    fun convertDisplay(currentText: String, from: Unit, to: Unit): String {
        if (from == to) return currentText
        val sizeKb = parse(currentText, from) ?: return currentText
        return displayValue(sizeKb, to)
    }

    private fun formatMegabytes(mb: Double): String {
        val thousandths = (mb * 1000.0).roundToInt()
        val whole = thousandths / 1000
        var frac = thousandths % 1000
        if (frac == 0 || abs(mb - whole) < 0.0005) return whole.toString()
        if (frac < 0) frac = -frac
        val fracStr = frac.toString().padStart(3, '0').trimEnd('0')
        return "$whole.$fracStr"
    }
}
