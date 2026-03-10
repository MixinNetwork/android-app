package one.mixin.android.ui.home.web3.trade.perps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.perps.PerpsMarketDao
import one.mixin.android.extension.toast
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshPerpsPositionsJob
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.wallet.TokenListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.vo.safe.TokenItem
import javax.inject.Inject

@AndroidEntryPoint
class PerpsActivity : BaseActivity() {

    @Inject
    lateinit var jobManager: MixinJobManager
    @Inject
    lateinit var perpsMarketDao: PerpsMarketDao

    private var selectedToken by mutableStateOf<TokenItem?>(null)

    companion object {
        private const val EXTRA_MARKET_ID = "extra_market_id"
        private const val EXTRA_MARKET_SYMBOL = "extra_market_symbol"
        private const val EXTRA_MARKET_DISPLAY_SYMBOL = "extra_market_display_symbol"
        private const val EXTRA_MARKET_TOKEN_SYMBOL = "extra_market_token_symbol"
        private const val EXTRA_MODE = "extra_mode"
        private const val EXTRA_IS_LONG = "extra_is_long"
        private const val POSITION_REFRESH_INTERVAL_MS = 3_000L

        const val MODE_DETAIL = "detail"
        const val MODE_OPEN_POSITION = "open_position"

        fun showDetail(
            context: Context,
            marketId: String,
            marketSymbol: String,
            marketDisplaySymbol: String,
            marketTokenSymbol: String = "",
        ) {
            val intent = Intent(context, PerpsActivity::class.java).apply {
                putExtra(EXTRA_MARKET_ID, marketId)
                putExtra(EXTRA_MARKET_SYMBOL, marketSymbol)
                putExtra(EXTRA_MARKET_DISPLAY_SYMBOL, marketDisplaySymbol)
                putExtra(EXTRA_MARKET_TOKEN_SYMBOL, marketTokenSymbol)
                putExtra(EXTRA_MODE, MODE_DETAIL)
            }
            context.startActivity(intent)
        }

        fun showOpenPosition(
            context: Context,
            marketId: String,
            marketSymbol: String,
            marketDisplaySymbol: String,
            marketTokenSymbol: String = "",
            isLong: Boolean,
        ) {
            val intent = Intent(context, PerpsActivity::class.java).apply {
                putExtra(EXTRA_MARKET_ID, marketId)
                putExtra(EXTRA_MARKET_SYMBOL, marketSymbol)
                putExtra(EXTRA_MARKET_DISPLAY_SYMBOL, marketDisplaySymbol)
                putExtra(EXTRA_MARKET_TOKEN_SYMBOL, marketTokenSymbol)
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
        val tokenSymbol = intent.getStringExtra(EXTRA_MARKET_TOKEN_SYMBOL) ?: ""
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_DETAIL
        val isLong = intent.getBooleanExtra(EXTRA_IS_LONG, true)

        observePositionRefresh()

        if (mode == MODE_OPEN_POSITION) {
            lifecycleScope.launch {
                val market = withContext(Dispatchers.IO) {
                    perpsMarketDao.getMarket(marketId)
                }
                if (market == null) {
                    toast(R.string.Alert_Not_Support)
                    finish()
                    return@launch
                }
                setContent {
                    MixinAppTheme {
                        OpenPositionPage(
                            market = market,
                            isLong = isLong,
                            onBack = { finish() },
                            selectedToken = selectedToken,
                            onTokenSelect = { showTokenSelection() },
                            onCurrentTokenChange = { token -> selectedToken = token }
                        )
                    }
                }
            }
            return
        }

        setContent {
            MixinAppTheme {
                PerpsMarketDetailPage(
                    marketId = marketId,
                    marketSymbol = marketSymbol,
                    displaySymbol = displaySymbol,
                    tokenSymbol = tokenSymbol,
                    onBack = { finish() }
                )
            }
        }
    }

    private fun showTokenSelection() {
        TokenListBottomSheetDialogFragment.newInstance(
            fromType = TokenListBottomSheetDialogFragment.TYPE_FROM_PERP,
            currentAssetId = selectedToken?.assetId
        ).setOnAssetClick { token ->
            selectedToken = token
        }.setOnDepositClick {
            showDepositAssetSelection()
        }.show(supportFragmentManager, TokenListBottomSheetDialogFragment.TAG)
    }

    private fun showDepositAssetSelection() {
        val token = selectedToken
        if (token == null) {
            toast(R.string.Not_found)
            return
        }
        WalletActivity.showDeposit(this, token)
    }

    private fun refreshPositions() {
        val walletId = Session.getAccountId()
        walletId?.let {
            jobManager.addJobInBackground(RefreshPerpsPositionsJob(it))
        }
    }

    private fun observePositionRefresh() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    refreshPositions()
                    delay(POSITION_REFRESH_INTERVAL_MS)
                }
            }
        }
    }
}
