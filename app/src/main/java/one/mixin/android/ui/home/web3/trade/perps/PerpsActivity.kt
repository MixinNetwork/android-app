package one.mixin.android.ui.home.web3.trade.perps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshPerpsPositionsJob
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.wallet.TokenListBottomSheetDialogFragment
import one.mixin.android.vo.safe.TokenItem
import javax.inject.Inject

@AndroidEntryPoint
class PerpsActivity : BaseActivity() {

    @Inject
    lateinit var jobManager: MixinJobManager

    private var selectedToken by mutableStateOf<TokenItem?>(null)

    companion object {
        private const val EXTRA_MARKET_ID = "extra_market_id"
        private const val EXTRA_MARKET_SYMBOL = "extra_market_symbol"
        private const val EXTRA_MARKET_DISPLAY_SYMBOL = "extra_market_display_symbol"
        private const val EXTRA_MODE = "extra_mode"
        private const val EXTRA_IS_LONG = "extra_is_long"

        const val MODE_DETAIL = "detail"
        const val MODE_OPEN_POSITION = "open_position"

        fun showDetail(context: Context, marketId: String, marketSymbol: String, marketDisplaySymbol: String) {
            val intent = Intent(context, PerpsActivity::class.java).apply {
                putExtra(EXTRA_MARKET_ID, marketId)
                putExtra(EXTRA_MARKET_SYMBOL, marketSymbol)
                putExtra(EXTRA_MARKET_DISPLAY_SYMBOL, marketDisplaySymbol)
                putExtra(EXTRA_MODE, MODE_DETAIL)
            }
            context.startActivity(intent)
        }

        fun showOpenPosition(context: Context, marketId: String, marketSymbol: String, marketDisplaySymbol: String, isLong: Boolean) {
            val intent = Intent(context, PerpsActivity::class.java).apply {
                putExtra(EXTRA_MARKET_ID, marketId)
                putExtra(EXTRA_MARKET_SYMBOL, marketSymbol)
                putExtra(EXTRA_MARKET_DISPLAY_SYMBOL, marketDisplaySymbol)
                putExtra(EXTRA_MODE, MODE_OPEN_POSITION)
                putExtra(EXTRA_IS_LONG, isLong)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val marketId = intent.getStringExtra(EXTRA_MARKET_ID) ?: ""
        val marketSymbol = intent.getStringExtra(EXTRA_MARKET_SYMBOL) ?: ""
        val displaySymbol = intent.getStringExtra(EXTRA_MARKET_DISPLAY_SYMBOL) ?: ""
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_DETAIL
        val isLong = intent.getBooleanExtra(EXTRA_IS_LONG, true)

        refreshPositions()

        setContent {
            MixinAppTheme {
                when (mode) {
                    MODE_OPEN_POSITION -> {
                        OpenPositionPage(
                            marketId = marketId,
                            marketSymbol = marketSymbol,
                            displaySymbol = displaySymbol,
                            isLong = isLong,
                            onBack = { finish() },
                            selectedToken = selectedToken,
                            onTokenSelect = { showTokenSelection() }
                        )
                    }

                    else -> {
                        PerpsMarketDetailPage(
                            marketId = marketId,
                            marketSymbol = marketSymbol,
                            displaySymbol = displaySymbol,
                            onBack = { finish() }
                        )
                    }
                }
            }
        }
    }

    private fun showTokenSelection() {
        TokenListBottomSheetDialogFragment.newInstance(
            fromType = TokenListBottomSheetDialogFragment.TYPE_FROM_PERP,
            currentAssetId = selectedToken?.assetId
        ).setOnAssetClick { token ->
            selectedToken = token
        }.show(supportFragmentManager, TokenListBottomSheetDialogFragment.TAG)
    }

    private fun refreshPositions() {
        val walletId = Session.getAccountId()
        walletId?.let {
            jobManager.addJobInBackground(RefreshPerpsPositionsJob(it))
        }
    }
}
