package es.pile.core.domain.models

/**
 * Kind of secret used to protect a document.
 */
enum class DocumentLockType {
    /** Numeric 4 digit PIN typed on a keypad. */
    PIN,

    /** Pattern drawn over a 3x3 grid of dots. */
    PATTERN
}
