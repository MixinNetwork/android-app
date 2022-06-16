@file:Suppress("unused")

package one.mixin.android.crypto.blst

import okhttp3.internal.and

const val blsDST = "BLS_SIG_BLS12381G2_XMD:SHA-256_SSWU_RO_NUL_"

fun sign(msg: ByteArray, IKM: ByteArray): P2 {
    val sk = SecretKey()
    sk.keygen(IKM)
    return sign(msg, sk)
}

fun sign(msg: ByteArray, sk: SecretKey): P2 {
    val sig = P2()
    return sig.hash_to(msg, blsDST)
        .sign_with(sk)
}

fun P2.verify(msg: ByteArray, pub: P1): Boolean {
    val afPub = pub.to_affine()
    val afSig = to_affine()
    val result = afSig.core_verify(afPub, true, msg, blsDST)
    return result == BLST_ERROR.BLST_SUCCESS
}

fun aggregateSignatures(vararg sigs: P2): P2 {
    val aggSig = P2()
    for (s in sigs) {
        aggSig.add(s)
    }
    return aggSig
}

@JvmName("aggregateVerifyHex")
fun aggregateVerify(msg: ByteArray, pubStrings: List<String>, sigStrings: List<String>): Boolean {
    val pubs = mutableListOf<P1_Affine>()
    pubStrings.mapTo(pubs) { P1_Affine(fromHexString(it)) }
    val sigs = mutableListOf<P2_Affine>()
    sigStrings.mapTo(sigs) { P2_Affine(fromHexString(it)) }
    return aggregateVerify(msg, pubs, sigs)
}

fun aggregateVerify(msg: ByteArray, pubs: List<P1_Affine>, sigs: List<P2_Affine>): Boolean {
    val aggSig = P2()
    for (s in sigs) {
        aggSig.aggregate(s)
    }

    val aggPk = P1()
    for (p in pubs) {
        aggPk.aggregate(p)
    }

    val afSig = aggSig.to_affine()
    val afPk = aggPk.to_affine()
    val result = afSig.core_verify(afPk, true, msg, blsDST)
    return result == BLST_ERROR.BLST_SUCCESS
}

private val hexArray = "0123456789abcdef".toCharArray()

fun toHexString(bytes: ByteArray): String {
    val hexChars = CharArray(bytes.size shl 1)
    var j = 0
    var k = 0
    while (j < bytes.size) {
        val v: Int = bytes[j] and 0xFF
        hexChars[k++] = hexArray[v ushr 4]
        hexChars[k++] = hexArray[v and 0x0F]
        j++
    }
    return String(hexChars)
}

private fun fromHexChar(c: Char): Int {
    if (c in '0'..'9') return c - '0' else if (c in 'a'..'f') return c - 'a' + 10 else if (c in 'A'..'F') return c - 'A' + 10
    throw IndexOutOfBoundsException("non-hex character")
}

fun fromHexString(str: String): ByteArray {
    val bytes = ByteArray(str.length ushr 1)
    var j = 0
    var k = 0
    while (j < bytes.size) {
        val hi = fromHexChar(str[k++])
        val lo = fromHexChar(str[k++])
        bytes[j] = (hi shl 4 or lo).toByte()
        j++
    }
    return bytes
}
