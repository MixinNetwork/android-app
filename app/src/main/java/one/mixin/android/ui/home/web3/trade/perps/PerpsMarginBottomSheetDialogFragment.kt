package one.mixin.android.ui.home.web3.trade.perps

import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.request.perps.AdjustMarginRequest
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.api.response.perps.PerpsPosition
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.api.response.perps.toPosition
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putString
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.trade.InputContent
import one.mixin.android.ui.home.web3.trade.TRADE_INPUT_MAX_DECIMAL_PLACES
import one.mixin.android.ui.home.web3.trade.limitTradeInputDecimalPlaces
import one.mixin.android.ui.wallet.TokenListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.components.MixinButton

@AndroidEntryPoint
class PerpsMarginBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    companion object {
        const val TAG = "PerpsMarginBottomSheetDialogFragment"
        private const val ARGS_POSITION = "args_position"
        private const val ARGS_INCREASE = "args_increase"
        private const val ARGS_INITIAL_MARGIN = "args_initial_margin"
        private const val ARGS_SOURCE = "args_source"
        private const val PREF_REDUCE_BY_PERCENT = "perps_reduce_margin_by_percent"

        fun newInstance(position: PerpsPositionItem, increase: Boolean, source: String, initialMargin: String? = null) = PerpsMarginBottomSheetDialogFragment().withArgs {
            putParcelable(ARGS_POSITION, position)
            putBoolean(ARGS_INCREASE, increase)
            putString(ARGS_INITIAL_MARGIN, initialMargin)
            putString(ARGS_SOURCE, source)
        }
    }

    private val initialPosition by lazy {
        requireNotNull(requireArguments().getParcelableCompat(ARGS_POSITION, PerpsPositionItem::class.java))
    }
    private val increase by lazy { requireArguments().getBoolean(ARGS_INCREASE) }

    override fun getTheme() = R.style.AppTheme_Dialog

    override fun getBottomSheetHeight(view: View): Int =
        requireContext().screenHeight() - view.getSafeAreaInsetsTop()

    @Suppress("DEPRECATION")
    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    @Composable
    override fun ComposeContent() {
        val viewModel = hiltViewModel<PerpetualViewModel>()
        val scope = rememberCoroutineScope()
        val lifecycleOwner = LocalLifecycleOwner.current
        val position by remember(initialPosition.positionId) {
            viewModel.observePosition(initialPosition.positionId)
        }.collectAsStateWithLifecycle(initialValue = initialPosition)
        val currentPosition = position ?: initialPosition
        var amount by rememberSaveable { mutableStateOf(arguments?.getString(ARGS_INITIAL_MARGIN).orEmpty()) }
        val preferences = remember { requireContext().defaultSharedPreferences }
        var reduceByPercent by rememberSaveable { mutableStateOf(preferences.getBoolean(PREF_REDUCE_BY_PERCENT, true)) }
        var inputIsPercentage by rememberSaveable { mutableStateOf(reduceByPercent) }
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        var selectedToken by remember { mutableStateOf<TokenItem?>(null) }
        var availableTokens by remember { mutableStateOf<List<TokenItem>?>(null) }
        val acceptedAssets = availableTokens.orEmpty().map { it.assetId }
        var market by remember { mutableStateOf<PerpsMarket?>(null) }
        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var availableMargin by remember(currentPosition.margin, currentPosition.quantity) { mutableStateOf<BigDecimal?>(null) }
        fun changeAmount(value: String) {
            if (!loading) {
                amount = value
                if (!increase) inputIsPercentage = reduceByPercent
                error = null
            }
        }
        val amountValue = if (increase) marginAdjustmentAmount(amount) else reduceMarginAmount(currentPosition.margin, amount, inputIsPercentage)
        val normalizedAmount = amountValue?.stripTrailingZeros()?.toPlainString().orEmpty()
        var remoteLiquidationPrice by remember(normalizedAmount, increase, currentPosition.updatedAt) { mutableStateOf<String?>(null) }
        var liquidationError by remember(normalizedAmount, increase, currentPosition.updatedAt) { mutableStateOf<String?>(null) }
        var isLiquidationLoading by remember(normalizedAmount, increase, currentPosition.updatedAt) { mutableStateOf(false) }
        val totalMargin = marginAfterAdjustment(currentPosition.margin, normalizedAmount, increase, availableMargin)
        val marginValue = currentPosition.margin?.toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO }
        val maximumReduction = marginValue?.let { margin -> availableMargin?.min(margin) ?: margin }
        val exceedsReductionLimit = !increase && (
            (amountValue != null && maximumReduction != null && amountValue > maximumReduction) ||
                (inputIsPercentage && (amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal(100))
            )
        val percentage = marginReductionPercentage(currentPosition.margin, amountValue)
        val reduceInput = when {
            reduceByPercent == inputIsPercentage -> amount
            reduceByPercent -> percentage?.setScale(0, RoundingMode.HALF_UP)?.toPlainString().orEmpty()
            else -> amountValue?.let { formatMarginAdjustmentInput(it, false) }.orEmpty()
        }
        val tokenBalance = perpsMarginTokenBalance(selectedToken)
        val insufficientBalance = increase && amountValue != null && tokenBalance != null && amountValue > tokenBalance
        val isCurrentWallet = !currentPosition.walletId.isNullOrBlank() && currentPosition.walletId == Session.getAccountId()
        val canSubmit = !loading && !exceedsReductionLimit && position?.state == PerpsPosition.STATE_OPEN && totalMargin != null &&
            !isLiquidationLoading && remoteLiquidationPrice != null &&
            isCurrentWallet &&
            if (increase) {
                selectedToken?.assetId in acceptedAssets && tokenBalance != null && !insufficientBalance
            } else {
                availableMargin?.let { amountValue != null && amountValue <= it } ?: true
            }

        LaunchedEffect(normalizedAmount, increase, currentPosition.updatedAt, exceedsReductionLimit) {
            remoteLiquidationPrice = null
            isLiquidationLoading = false
            if (totalMargin == null || exceedsReductionLimit) return@LaunchedEffect
            isLiquidationLoading = true
            try {
                delay(200L)
                remoteLiquidationPrice = requestLiquidationPrice(
                    onFailure = { liquidationError = it ?: getString(R.string.Data_error) },
                    onLimitExceeded = { liquidationError = getString(R.string.error_perps_position_size_exceeds_leverage_limit) },
                    onMarginExceeded = if (increase) null else { available ->
                        availableMargin = available
                        liquidationError = if (available == null) getString(R.string.Data_error) else null
                    },
                ) {
                    viewModel.estimateLiquidationPrice(
                        amount = normalizedAmount,
                        positionId = currentPosition.positionId,
                        action = if (increase) "increase_margin" else "decrease_margin",
                    )
                }
            } finally {
                isLiquidationLoading = false
            }
        }
        LaunchedEffect(initialPosition.positionId, lifecycleOwner) {
            market = viewModel.getMarketFromDb(initialPosition.marketId)
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    viewModel.refreshSinglePosition(initialPosition.positionId, initialPosition.walletId)
                    viewModel.loadMarketDetail(initialPosition.marketId, onSuccess = { market = it }, onError = {})
                    delay(3_000L)
                }
            }
        }
        LaunchedEffect(increase) {
            val source = requireArguments().getString(ARGS_SOURCE).orEmpty()
            if (increase) {
                AnalyticsTracker.trackPerpsAddMarginStart(source)
                viewModel.loadPerpsTokens(
                    onSuccess = { tokens ->
                        availableTokens = tokens
                        selectedToken = tokens.firstOrNull()
                    },
                    onError = { error = it },
                )
            } else {
                AnalyticsTracker.trackPerpsReduceMarginStart(source)
            }
        }

        fun submit(submittedAmount: String) {
            if (loading || position?.state != PerpsPosition.STATE_OPEN || !isCurrentWallet) return
            val value = marginAdjustmentAmount(submittedAmount) ?: return
            if (marginAfterAdjustment(currentPosition.margin, submittedAmount, increase, availableMargin) == null) return
            val token = selectedToken
            val estimatedLiquidationPrice = remoteLiquidationPrice
            if (increase && (token == null || token.assetId !in acceptedAssets || tokenBalance == null || value > tokenBalance || estimatedLiquidationPrice == null)) return
            if (increase) AnalyticsTracker.trackPerpsAddMarginPreview()
            loading = true
            isCancelable = false
            error = null
            scope.launch {
                try {
                    val response = viewModel.adjustPerpsMargin(
                        currentPosition.positionId,
                        AdjustMarginRequest(
                            type = if (increase) "increase" else "decrease",
                            amount = value.stripTrailingZeros().toPlainString(),
                            assetId = token?.assetId.takeIf { increase },
                        ),
                    )
                    val data = response.data
                    if (response.isSuccess && data != null) {
                        if (increase && token != null) {
                            if (data.paymentUrl.isNullOrBlank() || (data.payAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO) <= BigDecimal.ZERO) {
                                error = getString(R.string.Data_error)
                                return@launch
                            }
                            PerpsConfirmBottomSheetDialogFragment.newInstance(
                                marketSymbol = currentPosition.displaySymbol ?: currentPosition.tokenSymbol.orEmpty(),
                                marketIcon = currentPosition.iconUrl.orEmpty(),
                                isLong = currentPosition.side.equals("long", ignoreCase = true),
                                amount = data.payAmount,
                                leverage = currentPosition.leverage,
                                entryPrice = market?.last ?: currentPosition.entryPrice,
                                marginAssetPrice = token.priceUsd,
                                tokenSymbol = token.symbol,
                                liquidationPrice = estimatedLiquidationPrice,
                                priceScale = market?.priceScale ?: currentPosition.priceScale,
                                payUrl = data.paymentUrl,
                                isAddMargin = true,
                                marginBefore = currentPosition.margin,
                            ).show(parentFragmentManager, PerpsConfirmBottomSheetDialogFragment.TAG)
                        } else {
                            AnalyticsTracker.trackPerpsReduceMarginEnd()
                            toast(R.string.perps_margin_submitted)
                            viewModel.refreshSinglePosition(currentPosition.positionId, currentPosition.walletId)
                            viewModel.refreshOrders(currentPosition.walletId)
                        }
                        dismiss()
                    } else if (!increase && response.errorCode == 10653) {
                        availableMargin = availableMarginFromError(response.error?.extra)
                        error = if (availableMargin == null) requireContext().getMixinErrorStringByCode(response.errorCode, response.errorDescription) else null
                    } else {
                        error = if (response.isSuccess) getString(R.string.Data_error) else
                            requireContext().getMixinErrorStringByCode(response.errorCode, response.errorDescription)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    error = ErrorHandler.getErrorMessage(e)
                } finally {
                    loading = false
                    isCancelable = true
                }
            }
        }

        val submitCurrent by rememberUpdatedState(::submit)
        if (!increase) {
            DisposableEffect(lifecycleOwner) {
                val manager = childFragmentManager
                manager.setFragmentResultListener(PerpsCloseBottomSheetDialogFragment.RESULT_MARGIN_CONFIRMED, lifecycleOwner) { _, result ->
                    if (result.getString(PerpsCloseBottomSheetDialogFragment.RESULT_POSITION_ID) == initialPosition.positionId) {
                        result.getString(PerpsCloseBottomSheetDialogFragment.RESULT_AMOUNT)?.let { submitCurrent(it) }
                    }
                }
                onDispose { manager.clearFragmentResultListener(PerpsCloseBottomSheetDialogFragment.RESULT_MARGIN_CONFIRMED) }
            }
        }
        MixinAppTheme {
            val reductionLimitError = if (exceedsReductionLimit && maximumReduction != null) {
                val limit = formatPerpsMarginLimit(currentPosition.margin, maximumReduction, reduceByPercent)
                stringResource(R.string.max_removable, limit)
            } else null
            val errorText = if (insufficientBalance) stringResource(R.string.insufficient_balance) else error ?: liquidationError
            val onSubmit = {
                if (increase) {
                    submit(normalizedAmount)
                } else if (canSubmit && childFragmentManager.findFragmentByTag(PerpsCloseBottomSheetDialogFragment.TAG) == null) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    val approvedAmount = normalizedAmount
                    AnalyticsTracker.trackPerpsReduceMarginPreview()
                    PerpsCloseBottomSheetDialogFragment.newInstance(currentPosition.toPosition(), reduceMarginAmount = approvedAmount)
                        .show(childFragmentManager, PerpsCloseBottomSheetDialogFragment.TAG)
                }
            }
            PerpsMarginContent(
                position = currentPosition,
                marketPrice = market?.last,
                priceScale = market?.priceScale ?: currentPosition.priceScale,
                increase = increase,
                totalMargin = totalMargin?.takeIf { errorText == null && !exceedsReductionLimit },
                estimatedLiquidationPrice = remoteLiquidationPrice?.takeIf { errorText == null && !exceedsReductionLimit },
                isLiquidationLoading = isLiquidationLoading,
                errorText = errorText,
                canSubmit = canSubmit,
                loading = loading,
                onCancel = {
                    if (increase) AnalyticsTracker.trackPerpsAddMarginCancel() else AnalyticsTracker.trackPerpsReduceMarginCancel()
                    dismiss()
                },
                onSubmit = onSubmit,
                onGuide = { tab ->
                    PerpetualGuideBottomSheetDialogFragment.newInstance(tab)
                        .show(parentFragmentManager, PerpetualGuideBottomSheetDialogFragment.TAG)
                },
            ) {
                if (increase) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
                            .padding(16.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.Amount),
                                color = MixinAppTheme.colors.textPrimary,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = selectedToken?.chainName.orEmpty(),
                                color = MixinAppTheme.colors.textAssist,
                                fontSize = 12.sp,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        InputContent(
                            token = selectedToken?.toSwapToken(),
                            text = amount,
                            selectClick = if (!loading && availableTokens != null) ({
                                TokenListBottomSheetDialogFragment.newInstance(
                                    fromType = TokenListBottomSheetDialogFragment.TYPE_FROM_PERP,
                                    currentAssetId = selectedToken?.assetId,
                                    perpsTokens = availableTokens,
                                ).setOnAssetClick { token ->
                                    if (token.assetId in acceptedAssets) {
                                        selectedToken = token
                                        AnalyticsTracker.trackPerpsAddMarginTokenSelect(token.chainName, token.symbol)
                                    }
                                }.show(parentFragmentManager, TokenListBottomSheetDialogFragment.TAG)
                            }) else null,
                            onInputChanged = ::changeAmount,
                            readOnly = loading,
                            tokenIconSize = 25.dp,
                            inputFontSize = 24.sp,
                            inputFontWeight = FontWeight.W500,
                            autoFocus = true,
                            maxDecimalPlaces = TRADE_INPUT_MAX_DECIMAL_PLACES,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painterResource(R.drawable.ic_web3_wallet),
                                null,
                                tint = MixinAppTheme.colors.textAssist,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = selectedToken?.balance?.numberFormat8() ?: "0",
                                color = MixinAppTheme.colors.textAssist,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable(enabled = !loading && tokenBalance != null) {
                                    tokenBalance?.let { changeAmount(limitTradeInputDecimalPlaces(it.stripTrailingZeros().toPlainString())) }
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = selectedToken?.name.orEmpty(),
                                color = MixinAppTheme.colors.textAssist,
                                fontSize = 12.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                } else {
                    PerpsReduceMarginInput(
                        input = reduceInput,
                        isPercentage = reduceByPercent,
                        amount = amountValue,
                        percentage = percentage,
                        margin = marginValue,
                        errorText = reductionLimitError ?: errorText,
                        enabled = !loading,
                        onInputChanged = ::changeAmount,
                        onApplyReductionLimit = maximumReduction?.takeIf { reductionLimitError != null }?.let { maximum ->
                            {
                                if (reduceByPercent && !canFillMarginReductionAsPercentage(currentPosition.margin, maximum)) {
                                    reduceByPercent = false
                                    preferences.putBoolean(PREF_REDUCE_BY_PERCENT, false)
                                }
                                changeAmount(maximumMarginReductionInput(currentPosition.margin, maximum, reduceByPercent))
                            }
                        },
                        onToggleMode = {
                            amount = amountValue?.let { formatMarginAdjustmentInput(it, false) }.orEmpty()
                            inputIsPercentage = false
                            reduceByPercent = !reduceByPercent
                            preferences.putBoolean(PREF_REDUCE_BY_PERCENT, reduceByPercent)
                            error = null
                        },
                    )
                }
            }
        }
    }

    override fun showError(error: String) = Unit
}

@Composable
private fun PerpsMarginContent(
    position: PerpsPositionItem,
    marketPrice: String?,
    priceScale: Int,
    increase: Boolean,
    totalMargin: BigDecimal?,
    estimatedLiquidationPrice: String?,
    isLiquidationLoading: Boolean,
    errorText: String?,
    canSubmit: Boolean,
    loading: Boolean,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
    onGuide: (Int) -> Unit,
    inputContent: @Composable () -> Unit,
) {
    val actionTitle = stringResource(if (increase) R.string.Add else R.string.perps_reduce_action)
    Column(
        modifier = Modifier.fillMaxSize().background(MixinAppTheme.colors.background).imePadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        ) {
            val entryPrice = formatPerpsPrice(position.entryPrice, priceScale)
            val currentPrice = formatPerpsPrice(
                marketPrice?.takeIf { it.isNotBlank() } ?: position.markPrice ?: position.entryPrice,
                priceScale,
            )
            val subtitle = stringResource(R.string.auto_close_subtitle_after_open, entryPrice, currentPrice)
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoilImage(
                    model = position.iconUrl,
                    placeholder = R.drawable.ic_avatar_place_holder,
                    modifier = Modifier.size(30.dp).clip(CircleShape),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (increase) R.string.perps_add_margin_title else R.string.perps_reduce_margin_title,
                            stringResource(if (position.side.equals("long", true)) R.string.Long else R.string.Short),
                            position.tokenSymbol ?: position.displaySymbol.orEmpty(),
                        ),
                        color = MixinAppTheme.colors.textPrimary,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.W600,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = buildAnnotatedString {
                            append(subtitle)
                            addStyle(SpanStyle(color = MixinAppTheme.colors.textRemarks), 0, subtitle.length)
                            listOf(entryPrice, currentPrice).forEach { value ->
                                val start = subtitle.indexOf(value)
                                if (start >= 0) addStyle(SpanStyle(color = MixinAppTheme.colors.textAssist), start, start + value.length)
                            }
                        },
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }
                IconButton(onClick = onCancel, enabled = !loading) {
                    Icon(
                        painterResource(R.drawable.ic_circle_close),
                        stringResource(R.string.close),
                        tint = Color.Unspecified,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            inputContent()
            Spacer(Modifier.height(18.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                val currentMarginText = position.margin?.toBigDecimalOrNull()?.let { formatPerpsMarginAmount(it) } ?: "-"
                PerpsAddInfoRow(
                    title = stringResource(R.string.Margin),
                    value = if (totalMargin != null) "$currentMarginText → ${formatPerpsMarginAmount(totalMargin)}" else currentMarginText,
                )
                Spacer(Modifier.height(16.dp))
                PerpsAddInfoRow(
                    title = stringResource(R.string.Liquidation_Price),
                    value = listOfNotNull(
                        position.liquidationPrice?.takeIf { it.isNotBlank() }?.let { formatPerpsPrice(it, priceScale) } ?: "-",
                        estimatedLiquidationPrice?.let { formatPerpsPrice(it, priceScale) },
                    ).joinToString(" → "),
                    isLoading = isLiquidationLoading,
                    onTipClick = { onGuide(PerpetualGuideBottomSheetDialogFragment.TAB_LIQUIDATION) },
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        PerpsMarginActions(increase, actionTitle, errorText, canSubmit, loading, onCancel = onCancel, onSubmit = onSubmit)
    }
}

@Composable
private fun PerpsMarginActions(
    increase: Boolean,
    actionTitle: String,
    errorText: String?,
    canSubmit: Boolean,
    loading: Boolean,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().background(MixinAppTheme.colors.background)) {
        if (increase) {
            Text(
                text = errorText.orEmpty(),
                color = MixinAppTheme.colors.walletRed,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 20.dp),
        ) {
            MixinButton(
                onClick = onCancel,
                enabled = !loading,
                modifier = Modifier.weight(1f).height(48.dp),
                backgroundColor = MixinAppTheme.colors.backgroundGrayLight,
                shape = RoundedCornerShape(32.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                Text(stringResource(R.string.Cancel), color = MixinAppTheme.colors.textPrimary, fontSize = 16.sp)
            }
            MixinButton(
                onClick = onSubmit,
                enabled = canSubmit,
                modifier = Modifier.weight(1f).height(48.dp),
                backgroundColor = when {
                    !canSubmit -> MixinAppTheme.colors.backgroundGrayLight
                    increase -> MixinAppTheme.colors.walletGreen
                    else -> MixinAppTheme.colors.accent
                },
                shape = RoundedCornerShape(32.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = MixinAppTheme.colors.textAssist, strokeWidth = 2.dp)
                else Text(actionTitle, color = if (canSubmit) Color.White else MixinAppTheme.colors.textAssist, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun PerpsReduceMarginInput(
    input: String,
    isPercentage: Boolean,
    amount: BigDecimal?,
    percentage: BigDecimal?,
    margin: BigDecimal?,
    errorText: String?,
    enabled: Boolean,
    onInputChanged: (String) -> Unit,
    onToggleMode: () -> Unit,
    onApplyReductionLimit: (() -> Unit)? = null,
) {
    var textFieldValue by remember(isPercentage) { mutableStateOf(TextFieldValue(input, TextRange(input.length))) }
    LaunchedEffect(input) {
        textFieldValue = syncPerpsMarginInput(textFieldValue, input)
    }
    fun updateInput(value: String) {
        textFieldValue = TextFieldValue(value, TextRange(value.length))
        onInputChanged(value)
    }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val inputValue = input.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val sliderPercentage = (if (isPercentage) inputValue.toFloat() else percentage?.toFloat() ?: 0f).coerceIn(0f, 100f)
    val activeMarkerColor = MixinAppTheme.colors.accent
    val inactiveMarkerColor = MixinAppTheme.colors.textRemarks
    val maximum = if (isPercentage) BigDecimal(100) else margin ?: BigDecimal.ZERO
    val inputColor = when {
        errorText != null -> MixinAppTheme.colors.marketRed
        inputValue <= BigDecimal.ZERO -> MixinAppTheme.colors.textRemarks
        else -> MixinAppTheme.colors.textPrimary
    }
    val inputTextStyle = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.W600, color = inputColor, textAlign = TextAlign.Center)
    val textMeasurer = rememberTextMeasurer()
    val placeholder = "0"
    val inputWidth = with(LocalDensity.current) {
        textMeasurer.measure(input.ifEmpty { placeholder }, style = inputTextStyle, softWrap = false).size.width.toDp() + 2.dp
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    fun selectPercentage(value: BigDecimal) {
        val selected = if (isPercentage) value else reduceMarginAmount(margin?.toPlainString(), value.toPlainString(), true) ?: BigDecimal.ZERO
        updateInput(formatMarginAdjustmentInput(selected, isPercentage))
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(start = 16.dp, top = 40.dp, end = 16.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { updateInput(formatMarginAdjustmentInput((inputValue - BigDecimal.ONE).max(BigDecimal.ZERO), isPercentage)) },
                enabled = enabled && inputValue > BigDecimal.ZERO,
            ) {
                Icon(painterResource(R.drawable.ic_perps_minus), stringResource(R.string.perps_reduce_action), tint = Color.Unspecified, modifier = Modifier.size(16.dp))
            }
            Row(modifier = Modifier.weight(1f, fill = false), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                if (!isPercentage) Text(PERPS_USD_SYMBOL, style = inputTextStyle)
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { value ->
                        val text = value.text
                        if (text.length <= 40 && text.all { it in '0'..'9' || (!isPercentage && it == '.') } && text.count { it == '.' } <= 1 &&
                            text.substringAfter('.', "").length <= 2) {
                            textFieldValue = value
                            if (text != input) onInputChanged(text)
                        }
                    },
                    enabled = enabled,
                    singleLine = true,
                    modifier = Modifier.widthIn(min = 20.dp, max = 160.dp).width(inputWidth).focusRequester(focusRequester),
                    textStyle = inputTextStyle,
                    keyboardOptions = KeyboardOptions(keyboardType = if (isPercentage) KeyboardType.Number else KeyboardType.Decimal),
                    cursorBrush = SolidColor(MixinAppTheme.colors.accent),
                    decorationBox = { field ->
                        Box(contentAlignment = Alignment.Center) {
                            if (input.isEmpty()) Text(placeholder, style = inputTextStyle)
                            field()
                        }
                    },
                )
                if (isPercentage) Text("%", style = inputTextStyle)
            }
            IconButton(
                onClick = { updateInput(formatMarginAdjustmentInput((inputValue + BigDecimal.ONE).min(maximum), isPercentage)) },
                enabled = enabled && inputValue < maximum,
            ) {
                Icon(painterResource(R.drawable.ic_perps_add), stringResource(R.string.Add), tint = Color.Unspecified, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = enabled, onClick = onToggleMode).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isPercentage) formatPerpsMarginAmount(amount) else "${percentage?.setScale(0, RoundingMode.HALF_UP)?.toPlainString() ?: "0"}%",
                color = MixinAppTheme.colors.textRemarks,
                fontSize = 16.sp,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                painterResource(R.drawable.ic_switch),
                stringResource(if (isPercentage) R.string.Amount else R.string.perps_percentage),
                tint = MixinAppTheme.colors.textRemarks,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = with(LocalDensity.current) { 16.sp.toDp() })
                .then(if (onApplyReductionLimit != null) Modifier.clickable(enabled = enabled, onClick = onApplyReductionLimit) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = errorText.orEmpty(),
                color = MixinAppTheme.colors.badgeRed,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(28.dp))
        Slider(
            value = sliderPercentage,
            onValueChange = { selectPercentage(BigDecimal(snapMarginReductionPercentage(it))) },
            enabled = enabled && margin != null && margin > BigDecimal.ZERO,
            valueRange = 0f..100f,
            steps = 99,
            modifier = Modifier.fillMaxWidth().height(32.dp).drawWithContent {
                drawContent()
                val trackInset = 10.dp.toPx()
                val thumbFraction = if (layoutDirection == LayoutDirection.Rtl) 1f - sliderPercentage / 100f else sliderPercentage / 100f
                val thumbCenter = Offset(trackInset + (size.width - 2 * trackInset) * thumbFraction, size.height / 2)
                val thumbPath = Path().apply { addOval(Rect(thumbCenter, trackInset)) }
                clipPath(thumbPath, ClipOp.Difference) {
                    for (value in 0..100 step 25) {
                        val fraction = if (layoutDirection == LayoutDirection.Rtl) 1f - value / 100f else value / 100f
                        drawCircle(
                            color = if (value <= sliderPercentage) activeMarkerColor else inactiveMarkerColor,
                            radius = 3.dp.toPx(),
                            center = Offset(trackInset + (size.width - 2 * trackInset) * fraction, size.height / 2),
                        )
                    }
                }
            },
            colors = SliderDefaults.colors(
                thumbColor = MixinAppTheme.colors.accent,
                activeTrackColor = MixinAppTheme.colors.accent,
                inactiveTrackColor = MixinAppTheme.colors.backgroundGrayLight,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
        Layout(modifier = Modifier.fillMaxWidth(), content = {
            listOf(0, 25, 50, 75, 100).forEach { value ->
                Text(
                    text = if (isPercentage) "$value%" else formatPerpsMarginAmount(reduceMarginAmount(margin?.toPlainString(), value.toString(), true)),
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 12.sp,
                    fontWeight = if (isPercentage) FontWeight.W500 else FontWeight.Normal,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable(enabled = enabled && margin != null && margin > BigDecimal.ZERO) {
                        selectPercentage(BigDecimal(value))
                    }.padding(top = 4.dp),
                )
            }
        }) { measurables, constraints ->
            val labels = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
            val width = constraints.maxWidth
            val trackInset = 10.dp.roundToPx()
            val trackEdge = trackInset - 3.dp.roundToPx()
            layout(width, labels.maxOf { it.height }) {
                labels.forEachIndexed { index, label ->
                    val center = trackInset + (width - 2 * trackInset) * index / (labels.size - 1)
                    val x = when (index) {
                        0 -> trackEdge
                        labels.lastIndex -> width - trackEdge - label.width
                        else -> center - label.width / 2
                    }
                    label.placeRelative(x, 0)
                }
            }
        }
    }
}

internal fun syncPerpsMarginInput(value: TextFieldValue, input: String): TextFieldValue =
    if (value.text == input) value else TextFieldValue(input, TextRange(input.length))

@Preview(name = "100%", widthDp = 360, heightDp = 560, locale = "zh")
@Composable
private fun PerpsMarginPercentagePreview() {
    PerpsMarginPreviewContent("100", true)
}

@Preview(name = "0%", widthDp = 360, heightDp = 560, locale = "zh")
@Composable
private fun PerpsMarginZeroPreview() {
    PerpsMarginPreviewContent("", true)
}

@Preview(name = "USD", widthDp = 360, heightDp = 560, locale = "zh")
@Composable
private fun PerpsMarginDollarPreview() {
    PerpsMarginPreviewContent("25.00", false)
}

@Composable
private fun PerpsMarginPreviewContent(initialInput: String, initialIsPercentage: Boolean) {
    val position = remember {
        PerpsPositionItem(
            positionId = "preview-position",
            marketId = "preview-btc",
            side = "long",
            quantity = "0.02",
            entryPrice = "60000",
            leverage = 10,
            margin = "123.45678901",
            liquidationPrice = "55000",
            tokenSymbol = "BTC",
        )
    }
    var input by remember { mutableStateOf(initialInput) }
    var isPercentage by remember { mutableStateOf(initialIsPercentage) }
    var inputIsPercentage by remember { mutableStateOf(initialIsPercentage) }
    val amount = reduceMarginAmount(position.margin, input, inputIsPercentage)
    val percentage = marginReductionPercentage(position.margin, amount)
    val displayInput = when {
        isPercentage == inputIsPercentage -> input
        isPercentage -> percentage?.setScale(0, RoundingMode.HALF_UP)?.toPlainString().orEmpty()
        else -> amount?.let { formatMarginAdjustmentInput(it, false) }.orEmpty()
    }
    val totalMargin = marginAfterAdjustment(position.margin, amount?.toPlainString().orEmpty(), false)
    MixinAppTheme {
        PerpsMarginContent(
            position = position,
            marketPrice = "65000",
            priceScale = position.priceScale,
            increase = false,
            totalMargin = totalMargin,
            estimatedLiquidationPrice = null,
            isLiquidationLoading = false,
            errorText = null,
            canSubmit = totalMargin != null,
            loading = false,
            onCancel = {},
            onSubmit = {},
            onGuide = {},
        ) {
            PerpsReduceMarginInput(
                input = displayInput,
                isPercentage = isPercentage,
                amount = amount,
                percentage = percentage,
                margin = position.margin?.toBigDecimalOrNull(),
                errorText = null,
                enabled = true,
                onInputChanged = {
                    input = it
                    inputIsPercentage = isPercentage
                },
                onToggleMode = {
                    input = amount?.let { formatMarginAdjustmentInput(it, false) }.orEmpty()
                    inputIsPercentage = false
                    isPercentage = !isPercentage
                },
            )
        }
    }
}
