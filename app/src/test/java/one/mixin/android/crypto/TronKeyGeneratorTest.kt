package one.mixin.android.crypto

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.web3j.utils.Numeric

class TronKeyGeneratorTest {
    private val privateKey = Numeric.hexStringToByteArray(
        "0000000000000000000000000000000000000000000000000000000000000001"
    )

    @Test
    fun privateKeyProducesTronBase58CheckAddress() {
        assertEquals(
            "TMVQGm1qAQYVdetCeGRRkTWYYrLXuHK2HC",
            TronKeyGenerator.privateKeyToAddress(privateKey),
        )
    }

    @Test
    fun mnemonicUsesTronBip44Path() {
        val mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val derived = TronKeyGenerator.getPrivateKeyFromMnemonic(mnemonic)
        assertEquals(
            "b5a4cea271ff424d7c31dc12a3e43e401df7a40d7412a15750f3f0b6b5449a28",
            Numeric.toHexStringNoPrefix(derived),
        )
        assertEquals("TUEZSdKsoDHQMeZwihtdoBiN46zxhGWYdH", TronKeyGenerator.privateKeyToAddress(derived))
    }

    @Test
    fun messageV2MatchesTronWeb() {
        assertEquals(
            "0x1eb45a12a27653b832e450d0fc72b86e70b899fe7bcd2c573144f96d8c84aec72ad5057fa7ab80054ee06851b8209ce1957b45f6668c753247976cecad05109c1b",
            TronKeyGenerator.signMessageV2(privateKey, "hello"),
        )
    }

    @Test
    fun transactionSignatureMatchesTronWeb() {
        val txId = "0000000000000000000000000000000000000000000000000000000000000001"
        val signed = JSONObject(
            TronKeyGenerator.signTransaction(
                privateKey,
                """{"txID":"$txId","raw_data":{"contract":[{"parameter":{"value":{"owner_address":"417e5f4552091a69125d5dfcb7b8c2659029395bdf"}}}]}}""",
            )
        )
        assertEquals(txId, signed.getString("txID"))
        assertTrue(
            signed.getJSONArray("signature").getString(0).equals(
                "6673ffad2147741f04772b6f921f0ba6af0c1e77fc439e65c36dedf4092e88984c1a971652e0ada880120ef8025e709fff2080c4a39aae068d12eed009b68c891C",
                ignoreCase = true,
            )
        )
    }
}
