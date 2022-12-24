package one.mixin.android.api.listener

import okhttp3.Call
import okhttp3.EventListener
import okhttp3.EventListener.Factory
import okhttp3.Handshake
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy


class NetworkListener : EventListener() {

    companion object {
        fun get(): okhttp3.EventListener.Factory {
            return Factory { NetworkListener() }
        }
    }

    override fun callStart(call: Call) {
        super.callStart(call)
        Timber.e("callStart ${call.request().url}")
    }

    override fun dnsStart(call: Call, domainName: String) {
        super.dnsStart(call, domainName)
        Timber.e("dnsStart ${call.request().url}")
    }

    override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) {
        super.dnsEnd(call, domainName, inetAddressList)
        Timber.e("dnsEnd ${call.request().url}")
    }

    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        super.connectStart(call, inetSocketAddress, proxy)
        Timber.e("connectStart ${call.request().url}")
    }

    override fun secureConnectStart(call: Call) {
        super.secureConnectStart(call)
        Timber.e("secureConnectStart ${call.request().url}")
    }

    override fun secureConnectEnd(call: Call, handshake: Handshake?) {
        super.secureConnectEnd(call, handshake)
        Timber.e("secureConnectEnd ${call.request().url}")
    }

    override fun connectEnd(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: Protocol?
    ) {
        super.connectEnd(call, inetSocketAddress, proxy, protocol)
        Timber.e("connectEnd ${call.request().url}")
    }

    override fun connectFailed(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: Protocol?,
        ioe: IOException
    ) {
        super.connectFailed(call, inetSocketAddress, proxy, protocol, ioe)
        Timber.e("connectFailed ${call.request().url}")
    }

    override fun responseHeadersStart(call: Call) {
        super.responseHeadersStart(call)
        Timber.e("responseHeadersStart ${call.request().url}")
    }

    override fun requestHeadersEnd(call: Call, request: Request) {
        super.requestHeadersEnd(call, request)
        Timber.e("requestHeadersEnd ${call.request().url}")
    }

    override fun requestBodyStart(call: Call) {
        super.requestBodyStart(call)
        Timber.e("requestBodyStart ${call.request().url}")
    }

    override fun requestBodyEnd(call: Call, byteCount: Long) {
        super.requestBodyEnd(call, byteCount)
        Timber.e("requestBodyEnd ${call.request().url}")
    }

    override fun requestHeadersStart(call: Call) {
        super.requestHeadersStart(call)
        Timber.e("requestHeadersStart ${call.request().url}")
    }

    override fun responseHeadersEnd(call: Call, response: Response) {
        super.responseHeadersEnd(call, response)
        Timber.e("responseHeadersEnd ${call.request().url}")
    }

    override fun responseBodyStart(call: Call) {
        super.responseBodyStart(call)
        Timber.e("responseBodyStart ${call.request().url}")
    }

    override fun responseBodyEnd(call: Call, byteCount: Long) {
        super.responseBodyEnd(call, byteCount)
        Timber.e("responseBodyEnd ${call.request().url}")
    }

    override fun callEnd(call: Call) {
        super.callEnd(call)
        Timber.e("callEnd ${call.request().url}")
    }

    override fun callFailed(call: Call, ioe: IOException) {
        super.callFailed(call, ioe)
        Timber.e("callFailed ${call.request().url}")
    }

}