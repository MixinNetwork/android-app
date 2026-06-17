package one.mixin.android.crypto

import org.bitcoinj.crypto.MnemonicCode
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Provides functionality to generate Solana private keys from BIP-39 mnemonic phrases.
 * This implementation minimizes external dependencies by using standard Java Cryptography Architecture (JCA)
 * for HMAC and PBKDF2 operations.
 */
object SolanaKeyGenerator {

    private const val HMAC_SHA512_ALGORITHM = "HmacSHA512"

    /**
     * Derives a 32-byte Ed25519 private key seed from a BIP-39 mnemonic phrase
     * according to Solana's standards (BIP-39 -> BIP-32/SLIP-0010).
     *
     * @param mnemonic The space-separated mnemonic phrase.
     * @param passphrase An optional passphrase for the mnemonic (defaults to empty).
     * @param index The account index for derivation (e.g., 0 for m/44'/501'/0'/0').
     * @return A 32-byte array representing the private key seed.
     */
    fun getPrivateKeyFromMnemonic(mnemonic: String, passphrase: String = "", index: Int = 0): ByteArray {
        // Use MnemonicCode.toSeed which applies NFKD normalization to both mnemonic and
        // passphrase per BIP39, ensuring correct seed derivation for non-ASCII inputs.
        val seed = MnemonicCode.toSeed(mnemonic.split(" "), passphrase)

        // 2. Seed to Master Key/Chain Code via SLIP-0010
        val masterKey = hmacSha512("ed25519 seed".toByteArray(StandardCharsets.UTF_8), seed)
        var currentKey = masterKey.copyOfRange(0, 32)
        var currentChainCode = masterKey.copyOfRange(32, 64)

        // 3. Derive path m/44'/501'/index'/0'
        val solanaDerivationPath = intArrayOf(44, 501, index, 0)
        // All levels are hardened as per SLIP-0010 for Ed25519
        for (pathIndex in solanaDerivationPath) {
            val hardenedIndex = pathIndex or 0x80000000.toInt() // Apply hardening
            val data = ByteBuffer.allocate(37)
                .put(0.toByte()) // 0x00 for private key derivation
                .put(currentKey)
                .putInt(hardenedIndex)
                .array()

            val hmacResult = hmacSha512(currentChainCode, data)
            currentKey = hmacResult.copyOfRange(0, 32)
            currentChainCode = hmacResult.copyOfRange(32, 64)
        }

        return currentKey
    }

    /**
     * Computes HMAC-SHA512.
     */
    private fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance(HMAC_SHA512_ALGORITHM)
        mac.init(SecretKeySpec(key, HMAC_SHA512_ALGORITHM))
        return mac.doFinal(data)
    }
}
