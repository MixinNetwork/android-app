package one.mixin.android.ui.wallet.home

import com.google.gson.Gson
import one.mixin.android.api.response.CashAccount
import org.junit.Assert.assertEquals
import org.junit.Test

class WalletHomeCashAccountTest {
    private data class CashAccountEnvelope(
        val data: CashAccount,
    )

    @Test
    fun cashAccountParsesRewardApyFromApiResponse() {
        val json = """
            {"data":{"balance":"5.8","chain_id":"64692c23-8971-4cf4-84a7-4dd1271dd887","destination":"8ZftmiYXTGdwqXzus1tcDewMWUeD4AFMfFwdKyvqn3ef","min_amount":"0.01","reward_apy":"3.5"}}
        """.trimIndent()

        val response = Gson().fromJson(json, CashAccountEnvelope::class.java)

        assertEquals("3.5", response.data.rewardApy)
    }

    @Test
    fun walletHomeCashAccountFormatsRewardApy() {
        val account = CashAccount(
            balance = "5.8",
            minAmount = "0.01",
            rewardApy = "3.5",
        ).toWalletHomeCashAccount()

        assertEquals("3.5% APY", account?.apyText)
    }
}
