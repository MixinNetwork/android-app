package one.mixin.android.extension

import one.mixin.android.crypto.Base64
import one.mixin.android.crypto.Base64RFC4648

fun String.base64Encode() = toByteArray().base64Encode()

fun ByteArray.base64Encode(): String = Base64.encodeBytes(this)

fun String.decodeBase64(): ByteArray = Base64.decode(this)

fun ByteArray.base64RawURLEncode(): String = Base64RFC4648.getUrlEncoder().withoutPadding().encodeToString(this)

fun String.base64RawURLDecode(): ByteArray = Base64RFC4648.getUrlDecoder().decode(this)

@JvmOverloads
fun ByteArray.toIntString(
    separator: CharSequence = " ",
    prefix: CharSequence = "[",
    postfix: CharSequence = "]",
) =
    this.joinToString(separator, prefix, postfix) {
        String.format("%d", it)
    }
