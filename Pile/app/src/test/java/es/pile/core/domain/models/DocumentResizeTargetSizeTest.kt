package es.pile.core.domain.models

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DocumentResizeTargetSizeTest {

    @Test
    fun `parse reads a whole number of kilobytes`() {
        assertEquals(512, DocumentResizeTargetSize.parse("512", DocumentResizeTargetSize.Unit.KB))
        assertEquals(16, DocumentResizeTargetSize.parse("16", DocumentResizeTargetSize.Unit.KB))
    }

    @Test
    fun `parse converts megabytes to kilobytes`() {
        assertEquals(1024, DocumentResizeTargetSize.parse("1", DocumentResizeTargetSize.Unit.MB))
        assertEquals(1536, DocumentResizeTargetSize.parse("1.5", DocumentResizeTargetSize.Unit.MB))
        assertEquals(2048, DocumentResizeTargetSize.parse("2", DocumentResizeTargetSize.Unit.MB))
    }

    @Test
    fun `parse accepts a comma as decimal separator`() {
        assertEquals(1536, DocumentResizeTargetSize.parse("1,5", DocumentResizeTargetSize.Unit.MB))
        assertEquals(512, DocumentResizeTargetSize.parse("512,0", DocumentResizeTargetSize.Unit.KB))
    }

    @Test
    fun `parse rejects empty blank and non numeric input`() {
        assertNull(DocumentResizeTargetSize.parse("", DocumentResizeTargetSize.Unit.KB))
        assertNull(DocumentResizeTargetSize.parse("   ", DocumentResizeTargetSize.Unit.MB))
        assertNull(DocumentResizeTargetSize.parse("abc", DocumentResizeTargetSize.Unit.KB))
        assertNull(DocumentResizeTargetSize.parse("-2", DocumentResizeTargetSize.Unit.KB))
        assertNull(DocumentResizeTargetSize.parse("0", DocumentResizeTargetSize.Unit.MB))
    }

    @Test
    fun `isValid enforces the minimum target size`() {
        assertTrue(DocumentResizeTargetSize.isValid(16))
        assertTrue(DocumentResizeTargetSize.isValid(512))
        assertFalse(DocumentResizeTargetSize.isValid(15))
        assertFalse(DocumentResizeTargetSize.isValid(0))
    }

    @Test
    fun `preferredUnit uses MB from one megabyte upwards`() {
        assertEquals(DocumentResizeTargetSize.Unit.KB, DocumentResizeTargetSize.preferredUnit(512))
        assertEquals(DocumentResizeTargetSize.Unit.KB, DocumentResizeTargetSize.preferredUnit(1023))
        assertEquals(DocumentResizeTargetSize.Unit.MB, DocumentResizeTargetSize.preferredUnit(1024))
        assertEquals(DocumentResizeTargetSize.Unit.MB, DocumentResizeTargetSize.preferredUnit(1536))
    }

    @Test
    fun `displayValue formats KB and MB without trailing zeros`() {
        assertEquals("512", DocumentResizeTargetSize.displayValue(512, DocumentResizeTargetSize.Unit.KB))
        assertEquals("1", DocumentResizeTargetSize.displayValue(1024, DocumentResizeTargetSize.Unit.MB))
        assertEquals("1.5", DocumentResizeTargetSize.displayValue(1536, DocumentResizeTargetSize.Unit.MB))
        assertEquals("2", DocumentResizeTargetSize.displayValue(2048, DocumentResizeTargetSize.Unit.MB))
    }

    @Test
    fun `convertDisplay switches the shown number between units`() {
        assertEquals(
            "0.5",
            DocumentResizeTargetSize.convertDisplay("512", DocumentResizeTargetSize.Unit.KB, DocumentResizeTargetSize.Unit.MB)
        )
        assertEquals(
            "2048",
            DocumentResizeTargetSize.convertDisplay("2", DocumentResizeTargetSize.Unit.MB, DocumentResizeTargetSize.Unit.KB)
        )
        assertEquals(
            "abc",
            DocumentResizeTargetSize.convertDisplay("abc", DocumentResizeTargetSize.Unit.KB, DocumentResizeTargetSize.Unit.MB)
        )
    }
}
