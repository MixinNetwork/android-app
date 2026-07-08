package one.mixin.android.ui.landing

fun completeMnemonicForLogin(
    words: List<String>,
    checksum: (List<String>) -> List<String>,
): List<String> {
    return when (words.size) {
        12, 24 -> checksum(words)
        13, 25 -> words
        else -> throw IllegalArgumentException("Unsupported mnemonic word count: ${words.size}")
    }
}
