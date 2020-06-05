package one.mixin.android.extension

import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer

fun RequestBody.bodyToString(): String {
    val buffer = Buffer()
    this.writeTo(buffer)
    return buffer.readUtf8()
}

fun HttpUrl.cutOut(): String {
    return toString().removePrefix(scheme + "://" + host)
}

fun Request.show(): String {
    return (
        "Request{method=" +
            this.method +
            ", url=" +
            this.url +
            ", tags=" +
            this.tag() +
            ", headers=" +
            this.headers +
            ", body=" +
            this.body +
            '}'.toString()
        )
}

fun Response.show(): String {
    return (
        "Response{protocol=" +
            this.protocol +
            ", code=" +
            this.code +
            ", message=" +
            this.message +
            ", headers=" +
            this.headers +
            ", networkResponse=" +
            this.networkResponse +
            ", cacheResponse=" +
            this.cacheResponse +
            ", priorResponse=" +
            this.priorResponse +
            '}'.toString()
        )
}
