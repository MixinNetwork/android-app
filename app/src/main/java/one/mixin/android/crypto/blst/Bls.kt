@file:Suppress("unused")

package one.mixin.android.crypto.blst

import one.mixin.android.extension.hexStringToByteArray

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
    pubStrings.mapTo(pubs) { P1_Affine(it.hexStringToByteArray()) }
    val sigs = mutableListOf<P2_Affine>()
    sigStrings.mapTo(sigs) { P2_Affine(it.hexStringToByteArray()) }
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
