package one.mixin.android.mock

import okhttp3.Request

fun mockRequest(): Request {
    return Request.Builder()
        .url("https://api.mixin.one/me")
        .method("GET", null)
        .build()
}