package one.mixin.android.tip.bip44

import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Bip32ECKeyPair.HARDENED_BIT

object Bip44Path {
    /**
     * Generate Bitcoin derivation path with variable index
     * Bitcoin path: m/44'/0'/0'/0/{index}
     */
    fun bitcoin(index: Int = 0): IntArray =
        intArrayOf(
            44 or HARDENED_BIT,
            0 or HARDENED_BIT,
            0 or HARDENED_BIT,
            0,
            index,
        )

    /**
     * Generate Ethereum derivation path with variable index
     * Ethereum path: m/44'/60'/0'/0/{index}
     */
    fun ethereum(index: Int = 0): IntArray =
        intArrayOf(
            44 or HARDENED_BIT,
            60 or HARDENED_BIT,
            0 or HARDENED_BIT,
            0,
            index,
        )

    /**
     * Generate Solana derivation path with variable index
     * Solana path: m/44'/501'/{index}'/0'
     */
    fun solana(index: Int = 0): IntArray =
        intArrayOf(
            44 or HARDENED_BIT,
            501 or HARDENED_BIT,
            index or HARDENED_BIT,
            0 or HARDENED_BIT,
        )

    fun ethereumPathString(index: Int = 0): String {
        return "m/44'/60'/0'/0/$index"
    }

    fun solanaPathString(index: Int = 0): String {
        return "m/44'/501'/${index}'/0'"
    }
}

fun generateBip44Key(
    master: Bip32ECKeyPair,
    path: IntArray,
): Bip32ECKeyPair =
    Bip32ECKeyPair.deriveKeyPair(master, path)
