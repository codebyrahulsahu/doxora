package es.pile.core.domain.repositories

import kotlinx.coroutines.flow.Flow

/**
 * Stores the text that has been recognized (OCR) for a document.
 *
 * Everything is kept in the local database, the recognized text never leaves the device.
 */
interface DocumentTextRepository {

    /**
     * Observable stream with the recognized text of a document, or null when the
     * document has not been processed yet.
     */
    fun getDocumentText(documentId: String): Flow<String?>

    /**
     * Persists (or replaces) the recognized text of a document.
     */
    suspend fun saveDocumentText(documentId: String, text: String)

    /**
     * Removes the recognized text of a document.
     */
    suspend fun deleteDocumentText(documentId: String)

    /**
     * One shot read of every stored text, mapped by document id.
     * Used by the local backup exporter.
     */
    suspend fun getAllDocumentTexts(): Map<String, String>

    /**
     * Restores a batch of texts coming from a local backup file.
     */
    suspend fun restoreDocumentTexts(texts: Map<String, String>)
}
