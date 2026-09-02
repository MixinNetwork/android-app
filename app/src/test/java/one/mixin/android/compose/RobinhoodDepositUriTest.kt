package one.mixin.android.compose

import one.mixin.android.Constants
import one.mixin.android.pay.generateDepositUri
import org.junit.Assert.assertEquals
import org.junit.Test

class RobinhoodDepositUriTest {
    @Test
    fun nativeEthDepositUriUsesRobinhoodChainId() {
        assertEquals(
            "ethereum:0x1111111111111111111111111111111111111111@4663?value=1500000000000000000",
            generateDepositUri(
                assetId = Constants.ChainId.Robinhood,
                chainId = Constants.ChainId.Robinhood,
                assetKey = "0x0000000000000000000000000000000000000000",
                address = "0x1111111111111111111111111111111111111111",
                amount = "1.5",
                precision = 18,
            ),
        )
    }

    @Test
    fun robinhoodErc20DepositUriUsesTransferCall() {
        assertEquals(
            "ethereum:0x2222222222222222222222222222222222222222@4663/transfer?address=0x1111111111111111111111111111111111111111&amount=1.5&uint256=1500000",
            generateDepositUri(
                assetId = "token",
                chainId = Constants.ChainId.Robinhood,
                assetKey = "0x2222222222222222222222222222222222222222",
                address = "0x1111111111111111111111111111111111111111",
                amount = "1.5",
                precision = 6,
            ),
        )
    }
}
