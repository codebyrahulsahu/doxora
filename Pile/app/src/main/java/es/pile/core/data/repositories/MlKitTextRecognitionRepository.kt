package es.pile.core.data.repositories

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.TextRecognizerOptions
import es.pile.core.domain.repositories.TextRecognitionRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * OCR implementation backed by ML Kit Text Recognition.
 *
 * Processing runs entirely on the device, the images are never uploaded anywhere.
 */
class MlKitTextRecognitionRepository(
    private val ioDispatcher: CoroutineDispatcher
) : TextRecognitionRepository {

    override suspend fun recognizeText(bitmap: Bitmap): Result<String> = withContext(ioDispatcher) {
        val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        try {
            val image = InputImage.fromBitmap(bitmap, 0)

            val text = Tasks.await(recognizer.process(image))

            Result.success(text.text.trim())
        } catch (e: Exception) {
            Napier.e("Error recognizing text", e)
            Result.failure(e)
        } finally {
            runCatching { recognizer.close() }
        }
    }
}
