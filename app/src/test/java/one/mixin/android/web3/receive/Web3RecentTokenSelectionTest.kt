package one.mixin.android.web3.receive

import one.mixin.android.Constants
import one.mixin.android.ui.wallet.components.isWeb3RecentTokenChain
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Web3RecentTokenSelectionTest {
    @Test
    fun pearlIsVisibleInWeb3RecentTokens() {
        assertTrue(isWeb3RecentTokenChain(Constants.ChainId.PEARL_CHAIN_ID))
    }

    @Test
    fun pearlRecentTokenClickUsesMainListSelectionPath() {
        assertTrue(
            shouldHandleRecentTokenClick(
                Web3TokenListBottomSheetDialogFragment.TYPE_FROM_SEND,
                Constants.ChainId.PEARL_CHAIN_ID,
            )
        )
    }

    @Test
    fun unsupportedSendRecentTokenRemainsGuarded() {
        assertFalse(
            shouldHandleRecentTokenClick(
                Web3TokenListBottomSheetDialogFragment.TYPE_FROM_SEND,
                "unsupported-chain",
            )
        )
    }
}
