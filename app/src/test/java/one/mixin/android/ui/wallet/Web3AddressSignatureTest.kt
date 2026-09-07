package one.mixin.android.ui.wallet

import one.mixin.android.Constants
import org.junit.Assert.assertTrue
import org.junit.Test
import org.web3j.utils.Numeric

class Web3AddressSignatureTest {
    private val privateKey = ByteArray(32).apply { this[lastIndex] = 2 }
    private val message = "address\nuser-id\n1700000000"

    @Test
    fun pearlUsesRecoverableP2pkhFlagForTaprootValidation() {
        val signature = signUtxoAddressMessage(
            privateKey = privateKey,
            message = message,
            chainId = Constants.ChainId.PEARL_CHAIN_ID,
        )

        assertTrue(recoveryFlag(signature) in 31..34)
    }

    @Test
    fun bitcoinKeepsBip137P2wpkhFlag() {
        val signature = signUtxoAddressMessage(
            privateKey = privateKey,
            message = message,
            chainId = Constants.ChainId.BITCOIN_CHAIN_ID,
        )

        assertTrue(recoveryFlag(signature) in 39..42)
    }

    private fun recoveryFlag(signature: String): Int =
        Numeric.hexStringToByteArray(signature).first().toInt() and 0xff
}
