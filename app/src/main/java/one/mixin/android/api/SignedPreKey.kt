package one.mixin.android.api

class SignedPreKey(
    keyId: Int,
    pubKey: String,
    val signature: String
) : OneTimePreKey(keyId, pubKey)
