package one.mixin.android.job

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import one.mixin.android.MixinApplication
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.ChainDapp
import one.mixin.android.vo.Dapp
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class RefreshDappJobTest {
    @Test
    fun savesSupportedChainConfigurationWithoutOverwritingRpcWithInvalidData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        MixinApplication.appContext = context
        val preferences = context.defaultSharedPreferences
        preferences.edit().clear().commit()
        val chains = listOf(Chain.Ethereum, Chain.Polygon, Chain.BinanceSmartChain, Chain.Base, Chain.Arbitrum, Chain.Optimism, Chain.Solana)
        val job = RefreshDappJob()
        val dapp = Dapp("Example", "https://app.example.com", emptyList(), "https://app.example.com/icon.png", "test")

        assertEquals("https://mainnet.base.org", Chain.Base.rpcUrl)
        job.saveChainDapps(
            chains.map { chain ->
                ChainDapp(chain.assetId, listOf("", "invalid", "https://rpc.example.com/${chain.chainReference}", "https://backup.example.com"), listOf(dapp))
            },
        )

        chains.forEach { chain ->
            val rpcUrl = "https://rpc.example.com/${chain.chainReference}"
            assertEquals(rpcUrl, preferences.getString(chain.chainId, null))
            assertEquals(rpcUrl, chain.rpcUrl)
            val cachedDapps = GsonHelper.customGson.fromJson(preferences.getString("dapp_${chain.chainId}", null), Array<Dapp>::class.java)
            assertEquals("Example", cachedDapps.single().name)
        }

        job.saveChainDapps(
            listOf(
                ChainDapp(Chain.Base.assetId, emptyList(), emptyList()),
                ChainDapp(Chain.Optimism.assetId, listOf("", "invalid", "javascript:alert(1)"), emptyList()),
            ),
        )
        assertEquals("https://rpc.example.com/8453", Chain.Base.rpcUrl)
        assertEquals("https://rpc.example.com/10", Chain.Optimism.rpcUrl)
        assertEquals("[]", preferences.getString("dapp_${Chain.Base.chainId}", null))

        val saved = preferences.all
        job.saveChainDapps(listOf(ChainDapp("unknown-chain", listOf("https://unknown.example.com"), listOf(dapp))))
        assertEquals(saved, preferences.all)
    }
}
