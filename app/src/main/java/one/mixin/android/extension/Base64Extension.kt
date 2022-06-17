package one.mixin.android.extension

import one.mixin.android.crypto.Base64

fun String.base64Encode() = toByteArray().base64Encode()

fun ByteArray.base64Encode(): String = Base64.encodeBytes(this)

fun ByteArray.base64RawEncode(): String = android.util.Base64.encodeToString(this, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING)

fun String.decodeBase64(): ByteArray {
    return android.util.Base64.decode(this, android.util.Base64.DEFAULT)
}
fun String.base64RawUrlDecode(): ByteArray {
    return android.util.Base64.decode(this, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING)
}

@JvmOverloads
fun ByteArray.toIntString(
    separator: CharSequence = " ",
    prefix: CharSequence = "[",
    postfix: CharSequence = "]"
) =
    this.joinToString(separator, prefix, postfix) {
        String.format("%d", it)
    }
