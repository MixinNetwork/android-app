package one.mixin.android.pay

import one.mixin.android.Constants
import one.mixin.android.extension.isExternalTransferUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DepositUriTest {
    @Test
    fun pearlDepositUriUsesPearlScheme() {
        val address = "prl1p5rg2k5twnlggzdqhcw994xkgwqfuvvwhjnjrx84xsv9f834r887q4j2j5p"
        assertEquals(
            "pearl:$address?amount=1.25",
            generateDepositUri(
                assetId = Constants.ChainId.PEARL_CHAIN_ID,
                chainId = Constants.ChainId.PEARL_CHAIN_ID,
                assetKey = null,
                address = address,
                amount = "1.25",
            ),
        )
    }

    @Test
    fun bitcoinDepositUriUnchanged() {
        val address = "bc1qa7a84sq2nnkpxua5dly6fg553d5v06nsl608ss"
        assertEquals(
            "bitcoin:$address?amount=0.001",
            generateDepositUri(
                assetId = Constants.ChainId.BITCOIN_CHAIN_ID,
                chainId = Constants.ChainId.BITCOIN_CHAIN_ID,
                assetKey = null,
                address = address,
                amount = "0.001",
            ),
        )
    }

    @Test
    fun unsupportedChainReturnsNull() {
        assertNull(
            generateDepositUri(
                assetId = Constants.ChainId.TRON_CHAIN_ID,
                chainId = Constants.ChainId.TRON_CHAIN_ID,
                assetKey = null,
                address = "TXYZ",
                amount = "1",
            ),
        )
    }

    @Test
    fun pearlUriIsExternalTransferUrl() {
        assertTrue("pearl:prl1p5rg2k5twnlggzdqhcw994xkgwqfuvvwhjnjrx84xsv9f834r887q4j2j5p?amount=1".isExternalTransferUrl())
    }
}
