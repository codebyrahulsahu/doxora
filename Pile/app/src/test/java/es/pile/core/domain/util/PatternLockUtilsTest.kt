package es.pile.core.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PatternLockUtilsTest {

    @Test
    fun `dotInBetween returns the dot crossed on the same row`() {
        // 0 1 2
        assertEquals(1, PatternLockUtils.dotInBetween(0, 2))
        assertEquals(1, PatternLockUtils.dotInBetween(2, 0))

        // 3 4 5
        assertEquals(4, PatternLockUtils.dotInBetween(3, 5))
    }

    @Test
    fun `dotInBetween returns the dot crossed on the same column`() {
        assertEquals(3, PatternLockUtils.dotInBetween(0, 6))
        assertEquals(4, PatternLockUtils.dotInBetween(1, 7))
    }

    @Test
    fun `dotInBetween returns the center when crossing diagonally`() {
        // 0 -> 8 crosses the center dot 4
        assertEquals(4, PatternLockUtils.dotInBetween(0, 8))
        assertEquals(4, PatternLockUtils.dotInBetween(2, 6))
    }

    @Test
    fun `dotInBetween returns null for adjacent dots`() {
        assertNull(PatternLockUtils.dotInBetween(0, 1))
        assertNull(PatternLockUtils.dotInBetween(0, 3))
        assertNull(PatternLockUtils.dotInBetween(0, 4))
        assertNull(PatternLockUtils.dotInBetween(4, 8))
    }

    @Test
    fun `dotInBetween returns null for the same dot`() {
        assertNull(PatternLockUtils.dotInBetween(4, 4))
    }

    @Test
    fun `connect adds the crossed dot automatically`() {
        assertEquals(listOf(0, 1, 2), PatternLockUtils.connect(listOf(0), 2))
        assertEquals(listOf(0, 4, 8), PatternLockUtils.connect(listOf(0), 8))
    }

    @Test
    fun `connect ignores an already selected dot`() {
        assertNull(PatternLockUtils.connect(listOf(0, 4), 0))
    }

    @Test
    fun `connect starts a new pattern with the first dot`() {
        assertEquals(listOf(3), PatternLockUtils.connect(emptyList(), 3))
    }

    @Test
    fun `connect adds only dots not selected yet`() {
        // 0 -> 8 -> 2 crosses 5 (not selected yet); 4 was selected earlier
        val result = PatternLockUtils.connect(listOf(0, 4, 8), 2)
        assertEquals(listOf(0, 4, 8, 5, 2), result)
    }

    @Test
    fun `isValidLength requires the minimum amount of dots`() {
        assertFalse(PatternLockUtils.isValidLength(listOf(0, 1, 2)))
        assertTrue(PatternLockUtils.isValidLength(listOf(0, 1, 2, 3)))
        assertTrue(PatternLockUtils.isValidLength(List(9) { it }))
    }

    @Test
    fun `encode joins the dots in order`() {
        assertEquals("03678", PatternLockUtils.encode(listOf(0, 3, 6, 7, 8)))
        assertEquals("4", PatternLockUtils.encode(listOf(4)))
    }

    @Test
    fun `matches compares dots and order`() {
        assertTrue(PatternLockUtils.matches(listOf(0, 4, 8), listOf(0, 4, 8)))
        assertFalse(PatternLockUtils.matches(listOf(0, 4, 8), listOf(8, 4, 0)))
        assertFalse(PatternLockUtils.matches(listOf(0, 4), listOf(0, 4, 8)))
    }
}
