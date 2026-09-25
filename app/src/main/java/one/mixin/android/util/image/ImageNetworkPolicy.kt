package one.mixin.android.util.image

import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.Proxy
import java.net.UnknownHostException
import java.util.Locale

internal fun OkHttpClient.Builder.enforcePublicImageTargets(): OkHttpClient.Builder =
    dns(PublicImageDns())
        .proxy(Proxy.NO_PROXY)
        .addNetworkInterceptor(PublicImageNetworkInterceptor)

internal class PublicImageDns(
    private val delegate: Dns = Dns.SYSTEM,
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        if (hostname.isBlockedImageHostname()) {
            throw blockedImageTarget()
        }
        return delegate.lookup(hostname)
            .filterNot(InetAddress::isBlockedImageAddress)
            .ifEmpty { throw blockedImageTarget() }
    }
}

private object PublicImageNetworkInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val address = chain.connection()?.route()?.socketAddress?.address
        if (address == null || address.isBlockedImageAddress()) {
            throw IOException("Blocked non-public image target")
        }
        return chain.proceed(chain.request())
    }
}

internal fun String.isBlockedImageHostname(): Boolean {
    val host = trimEnd('.').lowercase(Locale.US)
    return host == "localhost" ||
        host.endsWith(".localhost") ||
        host.endsWith(".local") ||
        host.endsWith(".internal") ||
        (!host.contains('.') && !host.contains(':'))
}

internal fun InetAddress.isBlockedImageAddress(): Boolean {
    if (isAnyLocalAddress ||
        isLoopbackAddress ||
        isLinkLocalAddress ||
        isSiteLocalAddress ||
        isMulticastAddress
    ) {
        return true
    }
    val bytes = address.map { it.toInt() and 0xff }
    return when (this) {
        is Inet4Address ->
            bytes[0] == 0 ||
                bytes[0] == 100 && bytes[1] in 64..127 ||
                bytes[0] == 192 && bytes[1] == 0 ||
                bytes[0] == 198 && bytes[1] in 18..19 ||
                bytes[0] >= 224

        is Inet6Address ->
            bytes[0] and 0xfe == 0xfc ||
                bytes[0] == 0x20 && bytes[1] == 0x01 && bytes[2] == 0x0d && bytes[3] == 0xb8

        else -> true
    }
}

private fun blockedImageTarget() = UnknownHostException("Blocked non-public image target")
