package one.mixin.android.tip.bip44

import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Bip32ECKeyPair.HARDENED_BIT

object Bip44Path {
    // m/44'/0'/0'/0/0
    val Bitcoin =
        intArrayOf(
            44 or HARDENED_BIT,
            0 or HARDENED_BIT,
            0 or HARDENED_BIT,
            0,
            0,
        )

    // m/44'/60'/0'/0/0
    val Ethereum =
        intArrayOf(
            44 or HARDENED_BIT,
            60 or HARDENED_BIT,
            0 or HARDENED_BIT,
            0,
            0,
        )

    // m/44'/501'/0'/0'
    val Solana =
        intArrayOf(
            44 or HARDENED_BIT,
            501 or HARDENED_BIT,
            0 or HARDENED_BIT,
            0,
        )
}

fun generateBip44Key(
    master: Bip32ECKeyPair,
    path: IntArray,
): Bip32ECKeyPair =
    Bip32ECKeyPair.deriveKeyPair(master, path)
