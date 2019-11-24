package one.mixin.android.di

import okhttp3.HttpUrl
import okhttp3.Interceptor
import java.io.IOException

class HostSelectionInterceptor : Interceptor {
    @Volatile
    private var host: HttpUrl? = null

    fun setHost(url: String) {
        this.host = HttpUrl.parse(url)
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        var request = chain.request()
        this.host?.let {
            val newUrl = request.url().newBuilder()
                .host(it.url().toURI().host)
                .build()
            request = request.newBuilder()
                .url(newUrl)
                .build()
        }
        val resp = chain.proceed(request)
        return resp
    }
}