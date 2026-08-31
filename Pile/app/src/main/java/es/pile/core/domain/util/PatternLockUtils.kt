package es.pile.core.domain.util

/**
 * Pure helpers shared by the pattern lock UI.
 *
 * A pattern is stored as the indices of the 3x3 dots joined together,
 * row by row:
 *
 * ```
 * 0 1 2
 * 3 4 5
 * 6 7 8
 * ```
 *
 * so the L shaped pattern top-left -> bottom-left -> bottom-right is "03678".
 */
object PatternLockUtils {

    /** Number of dots of the pattern grid (3x3). */
    const val GRID_SIZE = 3

    /** Total number of dots of the pattern grid. */
    const val DOT_COUNT = GRID_SIZE * GRID_SIZE

    /** Minimum number of connected dots for a pattern to be valid. */
    const val MIN_PATTERN_LENGTH = 4

    /**
     * Dot that lies between [last] and [current] and would be crossed by a straight
     * movement, if any (like the Android system pattern lock).
     *
     * @return The index of the crossed dot, or null when there is none.
     */
    fun dotInBetween(last: Int, current: Int): Int? {
        val lastRow = last / GRID_SIZE
        val lastCol = last % GRID_SIZE
        val currentRow = current / GRID_SIZE
        val currentCol = current % GRID_SIZE

        val rowDiff = currentRow - lastRow
        val colDiff = currentCol - lastCol

        val crossesADot = rowDiff % 2 == 0 && colDiff % 2 == 0 && (rowDiff != 0 || colDiff != 0)
        if (!crossesADot) return null

        return (lastRow + rowDiff / 2) * GRID_SIZE + (lastCol + colDiff / 2)
    }

    /**
     * Builds the pattern reached when connecting [current] after [selected],
     * adding every dot crossed on the way (that is not selected yet).
     *
     * @param selected Dots already connected, in order.
     * @param current Dot being connected now.
     * @return The new list of connected dots, or null when [current] was already selected.
     */
    fun connect(selected: List<Int>, current: Int): List<Int>? {
        if (current in selected) return null

        val last = selected.lastOrNull() ?: return listOf(current)

        val inBetween = dotInBetween(last, current)

        return if (inBetween == null || inBetween in selected) {
            selected + current
        } else {
            selected + inBetween + current
        }
    }

    /** True when [pattern] has enough connected dots to be a valid pattern. */
    fun isValidLength(pattern: List<Int>): Boolean = pattern.size >= MIN_PATTERN_LENGTH

    /** Encodes a pattern as the string persisted (hashed) by the lock repository. */
    fun encode(selected: List<Int>): String = selected.joinToString(separator = "")

    /** True when both lists contain exactly the same dots in the same order. */
    fun matches(first: List<Int>, second: List<Int>): Boolean = first == second
}
