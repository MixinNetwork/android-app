package one.mixin.android.ui.home.web3.trade.perps

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withResumed
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.DataErrorException
import one.mixin.android.api.MixinResponseException
import one.mixin.android.api.request.perps.OpenOrderRequest
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.api.response.perps.PerpsPosition
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.perps.PerpsMarketDao
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.findFragmentActivityOrNull
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.toast
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshPerpsPositionsJob
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.common.biometric.buildTransferBiometricItem
import one.mixin.android.ui.wallet.TokenListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.wallet.transfer.TransferBalanceErrorBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.toUser
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class PerpsActivity : BaseActivity() {

    @Inject
    lateinit var jobManager: MixinJobManager
    @Inject
    lateinit var perpsMarketDao: PerpsMarketDao

    private val viewModel by viewModels<PerpetualViewModel>()

    private var selectedToken by mutableStateOf<TokenItem?>(null)
    private var leaderPositionId by mutableStateOf<String?>(null)
    private var renderJob: Job? = null
    private var directOrderHandled = false
    private var positionFailureDialog: AlertDialog? = null
    private val openPositionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            intent.removeExtra(EXTRA_LEADER_POSITION_ID)
            leaderPositionId = null
        }
    }

    companion object {
        private const val EXTRA_MARKET_ID = "extra_market_id"
        private const val EXTRA_MARKET_SYMBOL = "extra_market_symbol"
        private const val EXTRA_MARKET_DISPLAY_SYMBOL = "extra_market_display_symbol"
        private const val EXTRA_MARKET_TOKEN_SYMBOL = "extra_market_token_symbol"
        private const val EXTRA_MODE = "extra_mode"
        private const val EXTRA_IS_LONG = "extra_is_long"
        private const val EXTRA_SOURCE = "extra_source"
        private const val EXTRA_RETURN_TO_DETAIL = "extra_return_to_detail"
        private const val EXTRA_LEADER_POSITION_ID = "extra_leader_position_id"
        private const val EXTRA_INITIAL_LEVERAGE = "extra_initial_leverage"
        private const val EXTRA_INITIAL_MARGIN = "extra_initial_margin"
        private const val EXTRA_FROM_TRADE_LINK = "extra_from_trade_link"
        private const val STATE_DIRECT_ORDER_HANDLED = "state_direct_order_handled"
        private const val POSITION_REFRESH_INTERVAL_MS = 3_000L

        const val MODE_DETAIL = "detail"
        const val MODE_OPEN_POSITION = "open_position"

        fun showDetail(
            context: Context,
            marketId: String,
            marketSymbol: String,
            marketDisplaySymbol: String,
            marketTokenSymbol: String = "",
            source: String? = null,
            reuseCurrentActivity: Boolean = true,
            leaderPositionId: String? = null,
        ) {
            val intent = Intent(context, PerpsActivity::class.java).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (reuseCurrentActivity) {
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                putExtra(EXTRA_MARKET_ID, marketId)
                putExtra(EXTRA_MARKET_SYMBOL, marketSymbol)
                putExtra(EXTRA_MARKET_DISPLAY_SYMBOL, marketDisplaySymbol)
                putExtra(EXTRA_MARKET_TOKEN_SYMBOL, marketTokenSymbol)
                putExtra(EXTRA_MODE, MODE_DETAIL)
                source?.let { putExtra(EXTRA_SOURCE, it) }
                leaderPositionId?.let { putExtra(EXTRA_LEADER_POSITION_ID, it) }
            }
            context.startActivity(intent)
        }

        fun showOpenPosition(
            context: Context,
            marketId: String,
            marketSymbol: String,
            marketDisplaySymbol: String,
            marketTokenSymbol: String = "",
            isLong: Boolean?,
            source: String,
            returnToDetail: Boolean = false,
            leaderPositionId: String? = null,
            initialLeverage: Int? = null,
            initialMargin: String? = null,
            fromTradeLink: Boolean = false,
        ) {
            val intent = Intent(context, PerpsActivity::class.java).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                putExtra(EXTRA_MARKET_ID, marketId)
                putExtra(EXTRA_MARKET_SYMBOL, marketSymbol)
                putExtra(EXTRA_MARKET_DISPLAY_SYMBOL, marketDisplaySymbol)
                putExtra(EXTRA_MARKET_TOKEN_SYMBOL, marketTokenSymbol)
                putExtra(EXTRA_MODE, MODE_OPEN_POSITION)
                isLong?.let { putExtra(EXTRA_IS_LONG, it) }
                putExtra(EXTRA_SOURCE, source)
                putExtra(EXTRA_RETURN_TO_DETAIL, returnToDetail)
                leaderPositionId?.let { putExtra(EXTRA_LEADER_POSITION_ID, it) }
                initialLeverage?.let { putExtra(EXTRA_INITIAL_LEVERAGE, it) }
                initialMargin?.let { putExtra(EXTRA_INITIAL_MARGIN, it) }
                putExtra(EXTRA_FROM_TRADE_LINK, fromTradeLink)
            }
            val hostActivity = context.findFragmentActivityOrNull() as? PerpsActivity
            if (hostActivity != null && returnToDetail && leaderPositionId != null) {
                hostActivity.openPositionLauncher.launch(intent)
            } else {
                context.startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        directOrderHandled = savedInstanceState?.getBoolean(STATE_DIRECT_ORDER_HANDLED) == true
        if (directOrderHandled) intent.putExtra(EXTRA_MODE, MODE_DETAIL)
        observePositionRefresh()
        renderPage()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_DIRECT_ORDER_HANDLED, directOrderHandled)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        positionFailureDialog?.dismiss()
        setIntent(intent)
        directOrderHandled = false
        renderPage()
    }

    override fun onDestroy() {
        positionFailureDialog?.dismiss()
        super.onDestroy()
    }

    private fun renderPage() {
        renderJob?.cancel()
        val currentIntent = intent
        val marketId = currentIntent.getStringExtra(EXTRA_MARKET_ID) ?: ""
        val marketSymbolExtra = currentIntent.getStringExtra(EXTRA_MARKET_SYMBOL).orEmpty()
        val displaySymbolExtra = currentIntent.getStringExtra(EXTRA_MARKET_DISPLAY_SYMBOL).orEmpty()
        val tokenSymbolExtra = currentIntent.getStringExtra(EXTRA_MARKET_TOKEN_SYMBOL).orEmpty()
        val mode = currentIntent.getStringExtra(EXTRA_MODE) ?: MODE_DETAIL
        val isLong = currentIntent
            .takeIf { it.hasExtra(EXTRA_IS_LONG) }
            ?.getBooleanExtra(EXTRA_IS_LONG, true)
        val source = currentIntent.getStringExtra(EXTRA_SOURCE) ?: AnalyticsTracker.PerpsSource.PERPS_MARKET_DETAIL
        val returnToDetail = currentIntent.getBooleanExtra(EXTRA_RETURN_TO_DETAIL, false)
        leaderPositionId = currentIntent.getStringExtra(EXTRA_LEADER_POSITION_ID)
        val initialLeverage = currentIntent
            .takeIf { it.hasExtra(EXTRA_INITIAL_LEVERAGE) }
            ?.getIntExtra(EXTRA_INITIAL_LEVERAGE, 0)
        val initialMargin = currentIntent.getStringExtra(EXTRA_INITIAL_MARGIN)
        val fromTradeLink = currentIntent.getBooleanExtra(EXTRA_FROM_TRADE_LINK, false)

        if (mode == MODE_OPEN_POSITION && canPreviewPerpsLinkOrder(isLong, initialLeverage, initialMargin) && !directOrderHandled) {
            directOrderHandled = true
            currentIntent.putExtra(EXTRA_MODE, MODE_DETAIL)
            renderJob = lifecycleScope.launch {
                previewLinkOrder(marketId, requireNotNull(isLong), requireNotNull(initialLeverage), requireNotNull(initialMargin), source)
            }
            return
        }

        if (mode == MODE_OPEN_POSITION) {
            renderJob = lifecycleScope.launch {
                val market = viewModel.getMarketById(marketId)
                if (market == null) {
                    toast(R.string.Alert_Not_Support)
                    finish()
                    return@launch
                }
                val walletId = Session.getAccountId()
                val openedPosition = if (walletId.isNullOrEmpty()) {
                    null
                } else {
                    viewModel.getOpenPositionsFromDb(walletId).firstOrNull { it.marketId == marketId }
                }
                if (!canOpenNewPerpsPosition(openedPosition != null)) {
                    if (fromTradeLink && isLong != null && openedPosition != null) {
                        directOrderHandled = true
                        currentIntent.putExtra(EXTRA_MODE, MODE_DETAIL)
                        showMarketDetail(marketId, market.displaySymbol, market.displaySymbol, market.tokenSymbol, market, source)
                        showLeaderPositionFailure(market, openedPosition, isLong, initialLeverage, initialMargin)
                    } else if (returnToDetail) {
                        finish()
                    } else {
                        showDetail(
                            context = this@PerpsActivity,
                            marketId = market.marketId,
                            marketSymbol = market.displaySymbol,
                            marketDisplaySymbol = market.displaySymbol,
                            marketTokenSymbol = market.tokenSymbol,
                            source = source,
                            reuseCurrentActivity = false,
                            leaderPositionId = leaderPositionId,
                        )
                        finish()
                    }
                    return@launch
                }
                AnalyticsTracker.trackPerpsOpenStart(
                    direction = if (isLong == false) AnalyticsTracker.PerpsDirection.SHORT else AnalyticsTracker.PerpsDirection.LONG,
                    source = source,
                )

                setContent {
                    MixinAppTheme {
                        OpenPositionPage(
                            market = market,
                            isLong = isLong,
                            source = source,
                            onBack = { finish() },
                            onOrderCreated = {
                                if (leaderPositionId != null) {
                                    intent.removeExtra(EXTRA_LEADER_POSITION_ID)
                                    leaderPositionId = null
                                }
                                if (returnToDetail) {
                                    setResult(Activity.RESULT_OK)
                                }
                            },
                            onOpenSuccess = { openedMarketId ->
                                if (returnToDetail) {
                                    finish()
                                } else {
                                    showDetail(this@PerpsActivity, openedMarketId, "", "", "")
                                }
                            },
                            selectedToken = selectedToken,
                            onTokenSelect = { showTokenSelection() },
                            onCurrentTokenChange = { token ->
                                selectedToken = token
                            },
                            leaderPositionId = leaderPositionId,
                            initialLeverage = initialLeverage,
                            initialMargin = initialMargin,
                        )
                    }
                }
            }
            return
        }

        AnalyticsTracker.trackMarketDetail(
            type = AnalyticsTracker.MarketType.PERPS,
            source = AnalyticsTracker.normalizeMarketDetailSource(source),
        )

        renderJob = lifecycleScope.launch {
            val market = withContext(Dispatchers.IO) {
                perpsMarketDao.getMarket(marketId)
            }
            val displaySymbol = displaySymbolExtra.ifBlank { market?.displaySymbol.orEmpty() }
            val marketSymbol = marketSymbolExtra.ifBlank { displaySymbol }
            val tokenSymbol = tokenSymbolExtra.ifBlank { market?.tokenSymbol.orEmpty() }

            showMarketDetail(marketId, marketSymbol, displaySymbol, tokenSymbol, market, source)
        }
    }

    private fun showMarketDetail(
        marketId: String,
        marketSymbol: String,
        displaySymbol: String,
        tokenSymbol: String,
        market: PerpsMarket?,
        source: String,
    ) {
        setContent {
            MixinAppTheme {
                PerpsMarketDetailPage(
                    marketId = marketId,
                    marketSymbol = marketSymbol,
                    displaySymbol = displaySymbol,
                    tokenSymbol = tokenSymbol,
                    initialMarket = market,
                    onBack = { finish() },
                    onSharePosition = ::showSharePosition,
                    source = source,
                    leaderPositionId = leaderPositionId,
                )
            }
        }
    }

    private suspend fun previewLinkOrder(marketId: String, isLong: Boolean, leverage: Int, margin: String, source: String) {
        val progress = indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply { setCancelable(false) }
        var marketLoaded = false
        try {
            val account = Session.getAccount() ?: throw DataErrorException()
            val market = viewModel.getMarketById(marketId, refresh = true) ?: throw DataErrorException()
            showMarketDetail(marketId, market.displaySymbol, market.displaySymbol, market.tokenSymbol, market, source)
            marketLoaded = true
            viewModel.getOpenPerpsPosition(account.userId, marketId)?.let { position ->
                showLeaderPositionFailure(market, position, isLong, leverage, margin)
                return
            }

            val amount = margin.toBigDecimal()
            val minimum = market.minAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val maximum = market.maxAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
            if (leverage > market.leverage) {
                toast(getString(R.string.perps_maximum_leverage, market.leverage))
                return
            }
            if (minimum > BigDecimal.ZERO && amount < minimum) {
                toast(getString(R.string.perps_minimum_margin, market.minAmount, market.quoteSymbol))
                return
            }
            if (maximum > BigDecimal.ZERO && amount > maximum) {
                toast(getString(R.string.perps_maximum_margin, market.maxAmount, market.quoteSymbol))
                return
            }
            if ((market.last.toBigDecimalOrNull() ?: BigDecimal.ZERO) <= BigDecimal.ZERO) throw DataErrorException()
            val token = viewModel.loadPerpsMarginToken()
            suspend fun showInsufficientBalance(requiredAmount: String) {
                lifecycle.withResumed {
                    TransferBalanceErrorBottomSheetDialogFragment.newInstance(
                        buildTransferBiometricItem(account.toUser(), token, requiredAmount, null, null, null),
                    ).showNow(supportFragmentManager, TransferBalanceErrorBottomSheetDialogFragment.TAG)
                }
            }
            if ((token.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO) < amount) {
                showInsufficientBalance(margin)
                return
            }
            val side = if (isLong) "long" else "short"
            val liquidationPrice = when (val result = viewModel.estimateLiquidationPrice(margin, marketId, side, leverage)) {
                is LiquidationPriceResult.Success -> result.price
                is LiquidationPriceResult.LimitExceeded -> {
                    toast(R.string.error_perps_position_size_exceeds_leverage_limit)
                    return
                }
                else -> throw DataErrorException()
            }
            viewModel.getOpenPerpsPosition(account.userId, marketId)?.let { position ->
                showLeaderPositionFailure(market, position, isLong, leverage, margin)
                return
            }
            AnalyticsTracker.trackPerpsOpenStart(
                direction = if (isLong) AnalyticsTracker.PerpsDirection.LONG else AnalyticsTracker.PerpsDirection.SHORT,
                source = source,
            )
            AnalyticsTracker.trackPerpsOpenPreview()
            val response = viewModel.openPerpsOrder(
                OpenOrderRequest(
                    assetId = token.assetId,
                    marketId = marketId,
                    side = side,
                    amount = amount.stripTrailingZeros().toPlainString(),
                    leverage = leverage,
                    walletId = account.userId,
                    leaderPositionId = leaderPositionId,
                ),
                entryPrice = market.last,
            )
            intent.removeExtra(EXTRA_LEADER_POSITION_ID)
            leaderPositionId = null
            val payUrl = response.paymentUrl?.takeIf { it.isNotBlank() } ?: throw DataErrorException()
            val payAmount = response.payAmount.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: throw DataErrorException()
            if ((token.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO) < payAmount) {
                showInsufficientBalance(response.payAmount)
                return
            }
            lifecycle.withResumed {
                PerpsConfirmBottomSheetDialogFragment.newInstance(
                    marketSymbol = market.displaySymbol,
                    marketIcon = market.iconUrl,
                    isLong = isLong,
                    amount = response.payAmount,
                    leverage = leverage,
                    entryPrice = market.last,
                    marginAssetPrice = token.priceUsd,
                    tokenSymbol = token.symbol,
                    liquidationPrice = liquidationPrice,
                    priceScale = market.priceScale,
                    payUrl = payUrl,
                ).setOnDone {
                    refreshPositions()
                }.showNow(supportFragmentManager, PerpsConfirmBottomSheetDialogFragment.TAG)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: MixinResponseException) {
            toast(getMixinErrorStringByCode(e.errorCode, e.errorDescription))
            if (!marketLoaded) finish()
        } catch (e: Exception) {
            toast(ErrorHandler.getErrorMessage(e))
            if (!marketLoaded) finish()
        } finally {
            progress.dismiss()
        }
    }

    private suspend fun showLeaderPositionFailure(
        market: PerpsMarket,
        position: PerpsPositionItem,
        isLong: Boolean,
        leverage: Int?,
        margin: String?,
    ) {
        val leaderId = leaderPositionId
        val direction = getString(if (isLong) R.string.Long else R.string.Short)
        val message = listOfNotNull(
            getString(R.string.error_already_had_open_position),
            market.displaySymbol,
            "${getString(R.string.Direction)}: $direction" + (leverage?.let { " ${it}x" } ?: ""),
            margin?.let { "${getString(R.string.Amount)}: $it ${market.quoteSymbol}" },
            "${getString(R.string.Entry_Price)}: ${formatPerpsPrice(market.last, market.priceScale)}",
            "${getString(R.string.Sender)}: ${getString(R.string.Privacy_Wallet)}",
        ).joinToString("\n\n")
        lifecycle.withResumed {
            positionFailureDialog?.dismiss()
            positionFailureDialog = alertDialogBuilder()
                .setTitle(R.string.position_opening_failed)
                .setMessage(message)
                .setNegativeButton(R.string.Cancel, null)
                .apply {
                    if (canAddPerpsLeaderPosition(position, isLong, leverage)) {
                        setPositiveButton(R.string.add_position) { _, _ ->
                            lifecycleScope.launch {
                                try {
                                    val walletId = Session.getAccountId() ?: return@launch
                                    val currentPosition = viewModel.getOpenPerpsPosition(walletId, market.marketId)
                                    if (currentPosition?.positionId != position.positionId ||
                                        !canAddPerpsLeaderPosition(currentPosition, isLong, leverage)
                                    ) {
                                        toast(R.string.error_waiting_other_orders)
                                        return@launch
                                    }
                                    lifecycle.withResumed {
                                        showPerpsAddPosition(
                                            viewModel = viewModel,
                                            position = currentPosition,
                                            market = market,
                                            initialMargin = margin,
                                            leaderPositionId = leaderId,
                                            onOrderCreated = {
                                                if (leaderPositionId == leaderId) {
                                                    intent.removeExtra(EXTRA_LEADER_POSITION_ID)
                                                    leaderPositionId = null
                                                }
                                            },
                                        )
                                    }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    toast(ErrorHandler.getErrorMessage(e))
                                }
                            }
                        }
                    }
                }
                .show()
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

    private fun showSharePosition(position: PerpsPositionItem) {
        PerpsPositionShareBottomFragment.newInstance(position)
            .show(supportFragmentManager, PerpsPositionShareBottomFragment.TAG)
    }
}

internal fun canOpenNewPerpsPosition(hasOpenPosition: Boolean): Boolean = !hasOpenPosition

internal fun canPreviewPerpsLinkOrder(isLong: Boolean?, leverage: Int?, margin: String?): Boolean =
    isLong != null && leverage != null && leverage > 0 &&
        margin?.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true

internal fun canAddPerpsLeaderPosition(position: PerpsPositionItem?, isLong: Boolean, leverage: Int?): Boolean =
    position?.state == PerpsPosition.STATE_OPEN &&
        position.side.equals(if (isLong) "long" else "short", ignoreCase = true) &&
        (leverage == null || leverage == position.leverage)
