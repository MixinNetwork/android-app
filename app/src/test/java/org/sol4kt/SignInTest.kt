package org.sol4kt

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class SignInTest {
    private val address = "11111111111111111111111111111111"

    @Test
    fun fillsMissingDomainAndAddressFromPageAndLocalAccount() {
        val input = SignInInput.from("{}", address, "https://example.com/path")

        assertEquals("example.com", input.domain)
        assertEquals(address, input.address)
        assertEquals("example.com wants you to sign in with your Solana account:\n$address", input.toMessage())
    }

    @Test
    fun preservesMatchingSignInFields() {
        val data = JSONObject()
            .put("domain", "example.com")
            .put("address", address)
            .put("statement", "Sign in")
            .put("uri", "https://example.com/login")
            .put("version", "1")
            .put("chainId", "solana:mainnet")
            .put("nonce", "12345678")
            .put("resources", JSONArray().put("https://example.com/resource"))
        val input = SignInInput.from(data.toString(), address, "https://example.com/login")

        assertEquals("example.com", input.domain)
        assertEquals(address, input.address)
        assertEquals("Sign in", input.statement)
        assertEquals("https://example.com/login", input.uri)
        assertEquals("1", input.version)
        assertEquals("solana:mainnet", input.chainId)
        assertEquals("12345678", input.nonce)
        assertEquals(listOf("https://example.com/resource"), input.resources)
    }

    @Test
    fun authorityOmitsDefaultPortsAndKeepsNonDefaultPortsAndIpv6Brackets() {
        val pages = mapOf(
            "https://example.com:443/path" to "example.com",
            "http://example.com:80/path" to "example.com",
            "https://example.com:8443/path" to "example.com:8443",
            "http://example.com:443/path" to "example.com:443",
            "https://[::1]:8443/path" to "[::1]:8443",
        )
        pages.forEach { (page, authority) ->
            assertEquals(authority, SignInInput.from("{}", address, page).domain)
            val data = JSONObject().put("domain", authority)
            assertEquals(authority, SignInInput.from(data.toString(), address, page).domain)
        }
    }

    @Test
    fun rejectsMismatchedDomainPortAndAddress() {
        listOf("other.example", "example.com", "example.com:443", "example.com:8444", "").forEach { domain ->
            assertThrows(SignInException::class.java) {
                SignInInput.from(JSONObject().put("domain", domain).toString(), address, "https://example.com:8443")
            }
        }
        listOf("other-address", "").forEach { other ->
            assertThrows(SignInException::class.java) {
                SignInInput.from(JSONObject().put("address", other).toString(), address, "https://example.com")
            }
        }
    }

    @Test
    fun rejectsUnknownOriginAndMissingLocalAccountEvenWithCallerFields() {
        val data = JSONObject().put("domain", "example.com").put("address", address).toString()
        listOf(null, "", "example.com", "about:blank", "file:///page.html", "https://", "https://example.com\n").forEach { page ->
            assertThrows(SignInException::class.java) { SignInInput.from(data, address, page) }
        }
        listOf(null, "", " ").forEach { localAddress ->
            assertThrows(SignInException::class.java) { SignInInput.from(data, localAddress, "https://example.com") }
        }
    }

    @Test
    fun rejectsCarriageReturnAndNewlineInEveryFieldAndResource() {
        val fields = listOf("domain", "address", "statement", "uri", "version", "chainId", "nonce", "issuedAt", "expirationTime", "notBefore", "requestId", "resources")
        listOf("\r", "\n").forEach { newline ->
            fields.forEach { field ->
                val value = "before${newline}after"
                val data = JSONObject().put(field, if (field == "resources") JSONArray().put(value) else value)
                assertThrows("$field must reject ${newline.toByteArray().contentToString()}", SignInException::class.java) {
                    SignInInput.from(data.toString(), address, "https://example.com")
                }
            }
            assertThrows(SignInException::class.java) {
                SignInInput.from("{}", "$address$newline", "https://example.com")
            }
        }
    }
}
