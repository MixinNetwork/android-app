package one.mixin.android.extension

import okhttp3.HttpUrl
import okhttp3.RequestBody
import okio.Buffer

fun RequestBody.bodyToString(): String {
    val buffer = Buffer()
    this.writeTo(buffer)
    return buffer.readUtf8()
}

fun HttpUrl.cutOut(): String {
    return toString().removePrefix(scheme() + "://" + host())
}
