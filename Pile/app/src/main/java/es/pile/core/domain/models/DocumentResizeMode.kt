package es.pile.core.domain.models

/**
 * How the result of the Document Resizer should be stored when one or more
 * already imported documents are resized from the selection top bar.
 *
 * The resizer prompt offers exactly these two options.
 */
enum class DocumentResizeMode {
    /** The resized pages replace the original files of the document in the app. */
    SAVE_AS_ORIGINAL,

    /** The resized pages are stored as a new duplicate document in the app. */
    SAVE_AS_DUPLICATE
}
