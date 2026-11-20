package one.mixin.android.ui.wallet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import one.mixin.android.Constants.Account.PREF_WALLET_RECEIVE
import one.mixin.android.Constants.Account.PREF_WALLET_SEARCH_RECENT
import one.mixin.android.Constants.Account.PREF_WALLET_SEND
import one.mixin.android.extension.addToList
import one.mixin.android.extension.getStringList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WalletSearchHistoryTest {
    @Test
    fun clearingSearchHistoryKeepsSharedHistoryButDoesNotRestoreIt() {
        val preferences = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("wallet_search_history_test", Context.MODE_PRIVATE)
        preferences.edit().clear().apply()
        preferences.addToList(PREF_WALLET_SEND, "send")
        preferences.addToList(PREF_WALLET_RECEIVE, "receive")
        preferences.edit().putString(PREF_WALLET_SEARCH_RECENT, "old").apply()

        assertEquals(listOf("send"), preferences.walletSearchSharedHistory(PREF_WALLET_SEND))
        assertEquals(listOf("receive"), preferences.walletSearchSharedHistory(PREF_WALLET_RECEIVE))

        preferences.clearWalletSearchHistory()
        preferences.edit().putString(PREF_WALLET_SEARCH_RECENT, "new").apply()

        assertEquals(emptyList<String>(), preferences.walletSearchSharedHistory(PREF_WALLET_SEND))
        assertEquals(emptyList<String>(), preferences.walletSearchSharedHistory(PREF_WALLET_RECEIVE))
        assertEquals(listOf("send"), preferences.getStringList(PREF_WALLET_SEND))
        assertEquals(listOf("receive"), preferences.getStringList(PREF_WALLET_RECEIVE))
        assertEquals("new", preferences.getString(PREF_WALLET_SEARCH_RECENT, null))
        preferences.clearWalletSearchHistory()
        assertNull(preferences.getString(PREF_WALLET_SEARCH_RECENT, null))
    }
}
