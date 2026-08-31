package es.pile.core.data.repositories

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DocumentLockCodecTest {

    @Test
    fun `encode adds the pin prefix`() {
        assertEquals("pin:abc123", DocumentLockCodec.encode("abc123"))
    }

    @Test
    fun `isPattern detects legacy pattern locks so they can be cleared`() {
        assertTrue(DocumentLockCodec.isPattern("pattern:5d41402abc4b2a76b9719d911017c592"))
        assertFalse(DocumentLockCodec.isPattern("pin:5d41402abc4b2a76b9719d911017c592"))
        assertFalse(DocumentLockCodec.isPattern("5d41402abc4b2a76b9719d911017c592"))
        assertFalse(DocumentLockCodec.isPattern(null))
    }
}
