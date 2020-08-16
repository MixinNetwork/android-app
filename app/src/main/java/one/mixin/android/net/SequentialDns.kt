package one.mixin.android.net

import okhttp3.Dns
import timber.log.Timber
import java.net.InetAddress
import java.net.UnknownHostException

class SequentialDns(vararg dns: Dns) : Dns {
    private val dnsList: List<Dns> = listOf(*dns)

    @Throws(UnknownHostException::class)
    override fun lookup(hostname: String): List<InetAddress> {
        for (dns in dnsList) {
            try {
                val addresses = dns.lookup(hostname)
                if (addresses.isNotEmpty()) {
                    return addresses
                } else {
                    Timber.w("Didn't find any addresses for %s using %s. Continuing.", hostname, dns.javaClass.simpleName)
                }
            } catch (e: UnknownHostException) {
                Timber.w("Failed to resolve unknown host %s using %s. Continuing.", hostname, dns.javaClass.simpleName)
            } catch (e: IllegalStateException) {
                Timber.w("Failed to resolve illegal state %s using %s. Continuing.", hostname, dns.javaClass.simpleName)
            }
        }
        Timber.w("Failed to resolve using any DNS.")
        throw UnknownHostException(hostname)
    }
}
