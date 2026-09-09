package one.mixin.android.ui.wallet

import android.app.Application
import one.mixin.android.api.response.web3.SwapChain
import one.mixin.android.api.response.web3.SwapToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class DepositNetworkSelectionTest {
    @Test
    fun networkRowsMapToTheCorrectTokenWithOrWithoutDepositNotice() {
        val tokens = listOf(token("ethereum"), token("base"))
        for (showNotice in listOf(false, true)) {
            val adapter = DepositChooseNetworkBottomSheetDialogFragment.AssetAdapter(tokens, null, null, showNotice)
            val offset = if (showNotice) 1 else 0
            assertEquals(tokens.size + offset, adapter.itemCount)
            tokens.forEachIndexed { index, token ->
                assertEquals(token, adapter.tokenAt(index + offset))
            }
            if (showNotice) assertNull(adapter.tokenAt(0))
        }
    }

    private fun token(chain: String) = SwapToken(
        walletId = "wallet",
        address = "",
        assetId = chain,
        decimals = 18,
        name = "Ether",
        symbol = "ETH",
        icon = "",
        chain = SwapChain(chain, chain, "ETH", ""),
    )
}
