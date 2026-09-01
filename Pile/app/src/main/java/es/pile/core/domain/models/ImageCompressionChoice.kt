package es.pile.core.domain.models

/**
 * Compression choice made by the user when importing new images.
 *
 * When the app asks whether the imported images should be compressed, the
 * answer is captured in this model and passed down to the save pipeline,
 * overriding the silent settings-based behaviour for that import.
 *
 * @property compress Whether the imported images should be compressed.
 * @property targetSizeKb Maximum file size per image in kilobytes, used when
 * [compress] is true.
 */
data class ImageCompressionChoice(
    val compress: Boolean,
    val targetSizeKb: Int = DEFAULT_TARGET_SIZE_KB
) {
    companion object {
        /** Default target size offered by the compression prompt, in KB. */
        const val DEFAULT_TARGET_SIZE_KB = 512

        /** Preset target sizes offered by the compression prompt, in KB. */
        val PRESET_SIZES_KB = listOf(512, 1024, 2048)

        /** Convenience choice that keeps the images exactly as imported. */
        fun original() = ImageCompressionChoice(compress = false)
    }
}
