package one.mixin.android.util.image

import okhttp3.Dns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.UnknownHostException

class ImageNetworkPolicyTest {
    @Test
    fun `dns keeps only public addresses`() {
        val privateAddress = InetAddress.getByName("10.0.0.1")
        val publicAddress = InetAddress.getByName("8.8.8.8")
        val dns = PublicImageDns(dnsOf(privateAddress, publicAddress))

        assertEquals(listOf(publicAddress), dns.lookup("cdn.example.com"))
    }

    @Test(expected = UnknownHostException::class)
    fun `dns rejects private-only addresses`() {
        val dns = PublicImageDns(dnsOf(InetAddress.getByName("fd00::1")))

        dns.lookup("cdn.example.com")
    }

    @Test
    fun `private and reserved addresses are blocked`() {
        listOf(
            "0.0.0.0",
            "10.0.0.1",
            "100.64.0.1",
            "127.0.0.1",
            "169.254.1.1",
            "172.16.0.1",
            "192.168.0.1",
            "198.18.0.1",
            "224.0.0.1",
            "::1",
            "fe80::1",
            "fc00::1",
            "fd00::1",
            "2001:db8::1",
        ).forEach { address ->
            assertTrue(address, InetAddress.getByName(address).isBlockedImageAddress())
        }
    }

    @Test
    fun `public addresses are allowed`() {
        assertFalse(InetAddress.getByName("8.8.8.8").isBlockedImageAddress())
        assertFalse(InetAddress.getByName("2606:4700:4700::1111").isBlockedImageAddress())
    }

    private fun dnsOf(vararg addresses: InetAddress) =
        object : Dns {
            override fun lookup(hostname: String): List<InetAddress> = addresses.toList()
        }
}
