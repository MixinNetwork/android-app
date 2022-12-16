@file:Suppress("unused")

package one.mixin.android.crypto.mnemonic

import one.mixin.android.crypto.mnemonic.kdf.PBKDF2
import one.mixin.android.extension.sha256
import one.mixin.android.extension.toBitArray
import one.mixin.android.extension.toByteArray
import timber.log.Timber
import java.security.SecureRandom
import kotlin.collections.ArrayList

@JvmInline
value class MnemonicWords(val words: List<String>) {
    constructor(phrase: String) : this(phrase.split(" "))
    constructor(phrase: Array<String>) : this(phrase.toList())

    override fun toString() = words.joinToString(" ")
}

fun dirtyPhraseToMnemonicWords(string: String) = MnemonicWords(
    string.trim().lowercase().split(" ")
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toList()
)

/**
 * Generates a seed buffer from a mnemonic phrase according to the BIP39 spec.
 * The mnemonic phrase is given as a list of words and the seed can be salted using a password
 */
fun MnemonicWords.toSeed(password: String = ""): ByteArray {
    val pass = words.joinToString(" ")
    val salt = "mnemonic$password"

    return PBKDF2().derive(pass.toCharArray(), salt.toByteArray())
}

/**
 * Converts a phrase (list of words) into a [ByteArray] entropy buffer according to the BIP39 spec
 */
fun mnemonicToEntropy(phrase: String, wordList: List<String>) =
    MnemonicWords(phrase).mnemonicToEntropy(wordList)

/**
 * Converts a list of words into a [ByteArray] entropy buffer according to the BIP39 spec
 */
fun MnemonicWords.mnemonicToEntropy(wordList: List<String>): ByteArray {
    require(words.size % 3 <= 0) { "Word list size must be multiple of three words." }

    require(words.isNotEmpty()) { "Word list is empty." }

    val numTotalBits = words.size * 11
    val bitArray = BooleanArray(numTotalBits)

    for ((phraseIndex, word) in words.withIndex()) {
        val dictIndex = wordList.binarySearch(word)
        require(dictIndex >= 0) { "word($word) not in known word list" }

        // Set the next 11 bits to the value of the index.
        for (bit in 0..10)
            bitArray[phraseIndex * 11 + bit] = dictIndex and (1 shl (10 - bit)) != 0
    }

    val numChecksumBits = numTotalBits / 33
    val numEntropyBits = numTotalBits - numChecksumBits

    val entropy = bitArray.toByteArray(numEntropyBits / 8)

    // Take the digest of the entropy.
    val hash = entropy.sha256()
    val hashBits = hash.toBitArray()

    // Check all the checksum bits.
    for (i in 0 until numChecksumBits)
        require(bitArray[numEntropyBits + i] == hashBits[i]) { "mnemonic checksum does not match" }

    return entropy
}

/**
 * Converts an entropy buffer to a list of words according to the BIP39 spec
 */
fun entropyToMnemonic(entropy: ByteArray, wordList: List<String>): String {
    if (entropy.size % 4 > 0) {
        throw RuntimeException("Entropy not multiple of 32 bits.")
    }

    if (entropy.isEmpty()) {
        throw RuntimeException("Entropy is empty.")
    }

    val hash = entropy.sha256()
    val hashBits = hash.toBitArray()

    val entropyBits = entropy.toBitArray()
    val checksumLengthBits = entropyBits.size / 32

    val concatBits = BooleanArray(entropyBits.size + checksumLengthBits)
    entropyBits.copyInto(concatBits)
    hashBits.copyInto(concatBits, destinationOffset = entropyBits.size, endIndex = checksumLengthBits)

    val words = ArrayList<String>().toMutableList()
    val numWords = concatBits.size / 11
    for (i in 0 until numWords) {
        var index = 0
        for (j in 0..10) {
            index = index shl 1
            if (concatBits[i * 11 + j]) {
                index = index or 0x01
            }
        }
        words.add(wordList[index])
    }

    return words.joinToString(" ")
}

/**
 * Generates a mnemonic phrase, given a desired [strength]
 * The [strength] represents the number of entropy bits this phrase encodes and needs to be a multiple of 32
 */
fun generateMnemonic(strength: Int = 128, wordList: List<String>): String {
    require(strength % 32 == 0) { "The entropy strength needs to be a multiple of 32" }

    val entropyBuffer = ByteArray(strength / 8)
    SecureRandom().nextBytes(entropyBuffer)

    return entropyToMnemonic(entropyBuffer, wordList)
}

/**
 * Checks if MnemonicWords is a valid encoding according to the BIP39 spec
 */
fun MnemonicWords.validate(wordList: List<String>) = try {
    mnemonicToEntropy(wordList)
    true
} catch (e: IllegalArgumentException) {
    Timber.d(e)
    false
}
