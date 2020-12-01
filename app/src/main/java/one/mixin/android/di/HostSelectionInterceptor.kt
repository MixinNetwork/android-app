package one.mixin.android.di

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
import one.mixin.android.Constants
import one.mixin.android.Constants.API.URL
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.ProtocolException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.jvm.Throws

fun Throwable.isNeedSwitch(): Boolean {
    return (this is SocketTimeoutException || this is UnknownHostException || this is ConnectException || this is ProtocolException || this is NoRouteToHostException || this is SocketException)
}

class HostSelectionInterceptor private constructor() : Interceptor {
    @Volatile
    private var host: HttpUrl? = URL.toHttpUrlOrNull()

    private fun setHost(url: String) {
        this.host = url.toHttpUrlOrNull()
    }

    fun switch(request: Request) {
        val currentUrl = "${request.url.scheme}://${request.url.host}/"
        if (currentUrl != host.toString()) return
        if (currentUrl == URL) {
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
            val newUrl = request.url.newBuilder()
                .host(it.toUrl().toURI().host)
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
