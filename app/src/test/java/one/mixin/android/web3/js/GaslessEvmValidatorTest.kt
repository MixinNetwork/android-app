package one.mixin.android.web3.js

import one.mixin.android.api.response.web3.EIP7702SignRequest
import one.mixin.android.api.response.web3.EthGaslessSignRequests
import one.mixin.android.api.response.web3.EthGaslessTxPayload
import one.mixin.android.api.response.web3.UserOperationJson
import one.mixin.android.api.response.web3.UserOpSignRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.web3j.crypto.Hash
import java.math.BigInteger

class GaslessEvmValidatorTest {
    private val address = "0xe6cae83bde06e4c305530e199d7217f42808555b"
    private val digest = "0x5ff19acadaf683f545b800216672c273bcb8563df38a3f451ad080f8b936979b"
    private val auth = EIP7702SignRequest(null, digest, "1", address, "0")

    @Test
    fun authorizationDigestMatchesKnownRlpVector() {
        assertEquals(digest, GaslessEvmValidator.authorizationDigest(auth, BigInteger.ONE))
        assertEquals(
            digest,
            GaslessEvmValidator.authorizationDigest(
                auth.copy(chainId = "0x1", nonce = "0x0", address = address.uppercase()),
                BigInteger.ONE,
            ),
        )
        val baseDigest = "0xa0e2f32aadf3cb41a0b826c942b125f90a103d7f22466af057772f7b6c974a79"
        assertEquals(
            baseDigest,
            GaslessEvmValidator.authorizationDigest(auth.copy(chainId = "8453", message = baseDigest), BigInteger("8453")),
        )
        val nonceDigest = "0xd8b62c69c3d3376caee21f220244f51d6e13958b12ef48e3dce6e6a30ff24d7b"
        assertEquals(nonceDigest, GaslessEvmValidator.authorizationDigest(auth.copy(nonce = "1", message = nonceDigest), BigInteger.ONE))
    }

    @Test
    fun rejectsWildcardChainEvenWithMatchingDigest() {
        val wildcardDigest = Hash.sha3("0x05d78094e6cae83bde06e4c305530e199d7217f42808555b80")
        for (chainId in listOf("0", "0x0")) {
            for (expectedChain in listOf(BigInteger.ZERO, BigInteger.ONE)) {
                assertThrows(IllegalArgumentException::class.java) {
                    GaslessEvmValidator.authorizationDigest(auth.copy(chainId = chainId, message = wildcardDigest), expectedChain)
                }
            }
        }
    }

    @Test
    fun rejectsWrongChainTargetNonceAndDigest() {
        assertThrows(IllegalArgumentException::class.java) {
            GaslessEvmValidator.authorizationDigest(auth, BigInteger("8453"))
        }
        for (invalid in listOf(
            auth.copy(address = "0x" + "11".repeat(20)),
            auth.copy(nonce = "1"),
            auth.copy(nonce = "-1"),
            auth.copy(nonce = "18446744073709551616"),
            auth.copy(nonce = null),
            auth.copy(chainId = null),
            auth.copy(chainId = "-1"),
            auth.copy(message = "0x" + "00".repeat(32)),
            auth.copy(message = "0x1234"),
            auth.copy(message = "0x" + "gg".repeat(32)),
        )) {
            assertThrows(IllegalArgumentException::class.java) {
                GaslessEvmValidator.authorizationDigest(invalid, BigInteger.ONE)
            }
        }
    }

    @Test
    fun validatesSenderBeforeSigningWithOrWithoutAuthorization() {
        val payload = EthGaslessTxPayload(
            UserOperationJson(address, "0x0", "0x", "0x", "0x0", "0x0", "0x0", "0x0", "0x0", "0x", "0x"),
            EthGaslessSignRequests(UserOpSignRequest("", digest), auth),
        )
        assertEquals(digest, GaslessEvmValidator.validate(payload, address, BigInteger.ONE))
        val withoutAuth = payload.copy(signing = payload.signing.copy(eip7702Auth = null))
        assertNull(GaslessEvmValidator.validate(withoutAuth, address.uppercase().replace("0X", "0x"), BigInteger.ONE))
        assertNull(GaslessEvmValidator.validate(payload.copy(signing = payload.signing.copy(eip7702Auth = auth.copy(message = ""))), address, BigInteger.ONE))
        for (candidate in listOf(payload, withoutAuth)) {
            assertThrows(IllegalArgumentException::class.java) {
                GaslessEvmValidator.validate(candidate, "0x" + "11".repeat(20), BigInteger.ONE)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            GaslessEvmValidator.validate(payload.copy(signing = payload.signing.copy(userOperation = UserOpSignRequest("", "0x1234"))), address, BigInteger.ONE)
        }
    }
}
