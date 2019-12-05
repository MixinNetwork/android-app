package one.mixin.android.di

import java.io.IOException
import okhttp3.HttpUrl
import okhttp3.Interceptor
import one.mixin.android.Constants
import one.mixin.android.Constants.API.URL

class HostSelectionInterceptor private constructor() : Interceptor {
    @Volatile
    private var host: HttpUrl? = HttpUrl.parse(URL)

    private fun setHost(url: String) {
        this.host = HttpUrl.parse(url)
    }

    private var flag = false
    fun switch() {
        flag = !flag
        if (flag) {
            setHost(Constants.API.Mixin_URL)
        } else {
            setHost(URL)
        }
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        var request = chain.request()
        if (request.header("Upgrade") == "websocket") {
            return chain.proceed(request)
        }
        this.host?.let {
            val newUrl = request.url().newBuilder()
                .host(it.url().toURI().host)
                .build()
            request = request.newBuilder()
                .url(newUrl)
                .build()
        }
        return chain.proceed(request)
    }

    companion object {
        @Synchronized
        fun get(): HostSelectionInterceptor {
            if (instance == null) {
                instance = HostSelectionInterceptor()
            }
            return instance as HostSelectionInterceptor
        }

        private var instance: HostSelectionInterceptor? = null
    }
}
