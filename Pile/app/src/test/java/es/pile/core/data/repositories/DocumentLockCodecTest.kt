package es.pile.core.data.repositories

import es.pile.core.domain.models.DocumentLockType
import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentLockCodecTest {

    @Test
    fun `encode adds the prefix of the lock type`() {
        assertEquals("pin:abc123", DocumentLockCodec.encode(DocumentLockType.PIN, "abc123"))
        assertEquals("pattern:abc123", DocumentLockCodec.encode(DocumentLockType.PATTERN, "abc123"))
    }

    @Test
    fun `decodeType detects the lock type of the stored value`() {
        assertEquals(
            DocumentLockType.PIN,
            DocumentLockCodec.decodeType("pin:5d41402abc4b2a76b9719d911017c592")
        )
        assertEquals(
            DocumentLockType.PATTERN,
            DocumentLockCodec.decodeType("pattern:5d41402abc4b2a76b9719d911017c592")
        )
    }

    @Test
    fun `decodeType treats legacy values without prefix as PIN locks`() {
        assertEquals(
            DocumentLockType.PIN,
            DocumentLockCodec.decodeType("5d41402abc4b2a76b9719d911017c592")
        )
    }

    @Test
    fun `decodeType returns PIN for null or empty values`() {
        assertEquals(DocumentLockType.PIN, DocumentLockCodec.decodeType(null))
        assertEquals(DocumentLockType.PIN, DocumentLockCodec.decodeType(""))
    }

    @Test
    fun `encode and decode are symmetrical`() {
        val hash = "cafe babe".replace(" ", "")

        DocumentLockType.entries.forEach { type ->
            assertEquals(
                type,
                DocumentLockCodec.decodeType(DocumentLockCodec.encode(type, hash))
            )
        }
    }
}
