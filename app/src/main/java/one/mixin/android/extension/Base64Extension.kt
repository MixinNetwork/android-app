package one.mixin.android.extension

import one.mixin.android.crypto.Base64

fun String.base64Encode() = toByteArray().base64Encode()

fun ByteArray.base64Encode(): String = Base64.encodeBytes(this)
