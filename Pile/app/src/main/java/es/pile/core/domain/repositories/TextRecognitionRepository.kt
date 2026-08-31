package es.pile.core.domain.repositories

import android.graphics.Bitmap

/**
 * Runs optical character recognition (OCR) on a single image.
 */
interface TextRecognitionRepository {

    /**
     * Extracts the text contained in [bitmap].
     *
     * @return A [Result] with the recognized text (empty when nothing was found).
     */
    suspend fun recognizeText(bitmap: Bitmap): Result<String>
}
