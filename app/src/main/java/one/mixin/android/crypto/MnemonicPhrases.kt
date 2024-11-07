package one.mixin.android.crypto

import org.bitcoinj.crypto.MnemonicCode
import java.util.zip.CRC32

fun isMnemonicValid(words: List<String>): Boolean {
    return runCatching {
        MnemonicCode.toSeed(words, "")
    }.getOrNull() != null
}

fun toMnemonic(seed: ByteArray): String = MnemonicCode.INSTANCE.toMnemonic(seed).joinToString(" ")

fun toSeed(words: List<String>, passphrase: String): ByteArray = MnemonicCode.toSeed(words, passphrase)

fun mnemonicChecksumIndex(words: List<String>, prefixLen: Int = 3): Int {
    val trimmedWords = StringBuilder()
    for (word in words) {
        if (word.length < prefixLen) {
            trimmedWords.append(word)
        } else {
            trimmedWords.append(word.substring(0, prefixLen))
        }
    }

    val crc32 = CRC32()
    crc32.update(trimmedWords.toString().toByteArray())
    val checksum = crc32.value

    return (checksum % words.size).toInt()
}
