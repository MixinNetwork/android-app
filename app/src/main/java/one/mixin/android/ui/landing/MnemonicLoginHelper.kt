package one.mixin.android.ui.landing

data class PreparedLoginMnemonic(
    val completedWords: List<String>,
    val pendingImportWords: List<String>?,
)

fun prepareMnemonicForLogin(
    words: List<String>,
    checksum: (List<String>) -> List<String>,
): PreparedLoginMnemonic =
    PreparedLoginMnemonic(
        completedWords = completeMnemonicForLogin(words, checksum),
        pendingImportWords = pendingImportMnemonicForLogin(words),
    )

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

fun pendingImportMnemonicForLogin(words: List<String>): List<String>? =
    when (words.size) {
        12, 24 -> words
        13, 25 -> null
        else -> throw IllegalArgumentException("Unsupported mnemonic word count: ${words.size}")
    }
