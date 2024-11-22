package one.mixin.android.crypto

import blockchain.Blockchain
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation.createMasterPrivateKey
import org.bitcoinj.crypto.MnemonicCode
import java.util.zip.CRC32

fun isMnemonicValid(words: List<String>): Boolean {
    val nativeResult = runCatching {
        MnemonicCode.toSeed(words, "")
    }.getOrNull() != null
    require(Blockchain.isMnemonicValid(words.joinToString(" ")) == nativeResult)
    return nativeResult
}

fun toMnemonic(entropy: ByteArray): String {
    return MnemonicCode.INSTANCE.toMnemonic(entropy).joinToString(" ").also {
        require(Blockchain.newMnemonic(entropy) == it)
    }
}

fun toCompleteMnemonic(mnemonic: String): List<String> {
    val words = mnemonic.split(" ")
    val checksum = mnemonicChecksumWord(words)
    return words + checksum
}

fun toCompleteMnemonic(words: List<String>): List<String> {
    val checksum = mnemonicChecksumWord(words)
    return words + checksum
}

fun newMasterPrivateKeyFromMnemonic(mnemonic: String): DeterministicKey {
    val seed = toSeed(mnemonic.split(" ").let { list ->
        when (list.size) {
            25 -> {
                list.subList(0, 24)
            }
            13 -> {
                list.subList(0, 12)
            }
            else -> {
                list
            }
        }
    }, "")
    val masterKeyPrivateKey = createMasterPrivateKey(seed)
    require(Blockchain.mnemonicToMasterKey(mnemonic) == masterKeyPrivateKey.privateKeyAsHex)
    return masterKeyPrivateKey
}

fun toEntropy(words: List<String>): ByteArray = MnemonicCode.INSTANCE.toEntropy(words)

fun toSeed(words: List<String>, passphrase: String): ByteArray = MnemonicCode.toSeed(words, passphrase)

fun mnemonicChecksum(list: List<String>): Boolean {
    return when (list.size) {
        25 -> {
            mnemonicChecksumWord(list.subList(0, 24)) == list.last()
        }
        13 -> {
            list.distinct().size == list.size && mnemonicChecksumWord(list.subList(0, 12)) == list.last()
        }
        else -> {
            false
        }
    }
}

fun mnemonicChecksumWord(words: List<String>, prefixLen: Int = 3): String {
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

    val word = MnemonicCode.INSTANCE.wordList[(checksum % MnemonicCode.INSTANCE.wordList.size).toInt()]
    require(word == Blockchain.mnemonicChecksumWord(words.joinToString(" "), 3))
    return word
}