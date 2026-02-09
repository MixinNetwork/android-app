package one.mixin.android.ui.home.web3.trade

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.ui.common.BaseActivity

@AndroidEntryPoint
class MarketDetailActivity : BaseActivity() {

    companion object {
        private const val EXTRA_MARKET_ID = "extra_market_id"
        private const val EXTRA_MARKET_SYMBOL = "extra_market_symbol"

        fun show(context: Context, marketId: String, marketSymbol: String) {
            val intent = Intent(context, MarketDetailActivity::class.java).apply {
                putExtra(EXTRA_MARKET_ID, marketId)
                putExtra(EXTRA_MARKET_SYMBOL, marketSymbol)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val marketId = intent.getStringExtra(EXTRA_MARKET_ID) ?: ""
        val marketSymbol = intent.getStringExtra(EXTRA_MARKET_SYMBOL) ?: ""

        setContent {
            MarketDetailPage(
                marketId = marketId,
                marketSymbol = marketSymbol,
                onBack = { finish() }
            )
        }
    }
}
