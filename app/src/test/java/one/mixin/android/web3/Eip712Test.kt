package one.mixin.android.web3

import com.google.gson.JsonParser
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.internal.WCEthereumSignMessage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import org.web3j.crypto.StructuredDataEncoder
import org.web3j.utils.Numeric
import java.math.BigInteger

class Eip712Test {
    private val typedData =
        """
        {
          "domain": {
            "name": "HyperliquidSignTransaction",
            "version": "1",
            "chainId": 42161,
            "verifyingContract": "0x0000000000000000000000000000000000000000"
          },
          "types": {
            "EIP712Domain": [
              {"name": "name", "type": "string"},
              {"name": "version", "type": "string"},
              {"name": "chainId", "type": "uint256"},
              {"name": "verifyingContract", "type": "address"}
            ],
            "HyperliquidTransaction:ApproveBuilderFee": [
              {"name": "hyperliquidChain", "type": "string"},
              {"name": "maxFeeRate", "type": "string"},
              {"name": "builder", "type": "address"},
              {"name": "nonce", "type": "uint64"}
            ]
          },
          "primaryType": "HyperliquidTransaction:ApproveBuilderFee",
          "message": {
            "hyperliquidChain": "Mainnet",
            "maxFeeRate": "0.001%",
            "builder": "0x1111111111111111111111111111111111111111",
            "nonce": 18446744073709551615
          }
        }
        """.trimIndent()

    @Test
    fun walletConnectIgnoresTopLevelMetadataAndPreservesMessageNonce() {
        val expectedHash = StructuredDataEncoder(typedData).hashStructuredData()
        val extended = JsonParser.parseString(typedData).asJsonObject.apply {
            addProperty("nonce", 123)
            addProperty("extra", "metadata")
        }
        assertArrayEquals(expectedHash, hashEip712Message(typedData))
        assertArrayEquals(expectedHash, hashEip712Message(extended.toString()))

        val privateKey = Numeric.toBytesPadded(BigInteger.ONE, 32)
        val signature = Sign.signMessage(expectedHash, ECKeyPair.create(privateKey), false)
        val expectedSignature = Numeric.toHexString(signature.r + signature.s + signature.v)
        val walletConnect = object : WalletConnect() {}
        assertEquals(
            expectedSignature,
            walletConnect.signMessage(
                privateKey,
                WCEthereumSignMessage(listOf("", extended.toString()), WCEthereumSignMessage.WCSignType.TYPED_MESSAGE),
            ),
        )
        extended.getAsJsonObject("message").addProperty("nonce", 123)
        assertFalse(expectedHash.contentEquals(hashEip712Message(extended.toString())))
    }

    @Test
    fun invalidTypedDataStillFailsValidation() {
        val invalid = JsonParser.parseString(typedData).asJsonObject
        invalid.getAsJsonObject("types").getAsJsonArray("HyperliquidTransaction:ApproveBuilderFee")[0].asJsonObject.addProperty("type", "invalid type")
        assertThrows(RuntimeException::class.java) { hashEip712Message(invalid.toString()) }
    }
}
