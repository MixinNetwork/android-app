package one.mixin.android.util

import cafe.cryptography.curve25519.CompressedEdwardsY

fun verifyPubkey(bytes: ByteArray): Boolean {
    return try {
        return !CompressedEdwardsY(bytes).decompress().isSmallOrder
    } catch (e: Exception) {
        false
    }
}
