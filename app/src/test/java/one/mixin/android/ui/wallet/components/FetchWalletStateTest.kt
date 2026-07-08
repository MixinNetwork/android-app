package one.mixin.android.ui.wallet.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FetchWalletStateTest {
    @Test
    fun initialFetchFailureShowsRetryState() {
        assertEquals(FetchWalletState.FETCH_ERROR, fetchWalletFailureState(hasExistingWallets = false))
    }

    @Test
    fun loadMoreFailureKeepsSelectionState() {
        assertEquals(FetchWalletState.SELECT, fetchWalletFailureState(hasExistingWallets = true))
    }
}
