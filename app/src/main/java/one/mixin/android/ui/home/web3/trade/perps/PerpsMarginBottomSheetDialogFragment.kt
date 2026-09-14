package one.mixin.android.ui.home.web3.trade.perps

import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.trade.InputContent
import one.mixin.android.ui.home.web3.trade.KeyboardAwareBox
import one.mixin.android.ui.home.web3.trade.TRADE_INPUT_MAX_DECIMAL_PLACES
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
        private const val PREF_REDUCE_BY_PERCENT = "perps_reduce_margin_by_percent"

        fun newInstance(position: PerpsPositionItem, increase: Boolean) = PerpsMarginBottomSheetDialogFragment().withArgs {
            putParcelable(ARGS_POSITION, position)
            putBoolean(ARGS_INCREASE, increase)
        }
    }

    private val initialPosition by lazy {
        requireNotNull(requireArguments().getParcelableCompat(ARGS_POSITION, PerpsPositionItem::class.java))
    }
    private val increase by lazy { requireArguments().getBoolean(ARGS_INCREASE) }

    override fun getTheme() = R.style.AppTheme_Dialog

    override fun getBottomSheetHeight(view: View): Int =
        requireContext().screenHeight() - view.getSafeAreaInsetsTop()

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
        var amount by rememberSaveable { mutableStateOf("") }
        val preferences = remember { requireContext().defaultSharedPreferences }
        var reduceByPercent by rememberSaveable { mutableStateOf(preferences.getBoolean(PREF_REDUCE_BY_PERCENT, true)) }
        var inputIsPercentage by rememberSaveable { mutableStateOf(reduceByPercent) }
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        var selectedToken by remember { mutableStateOf<TokenItem?>(null) }
        var acceptedAssets by remember { mutableStateOf<List<String>>(emptyList()) }
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
            reduceByPercent -> percentage?.stripTrailingZeros()?.toPlainString().orEmpty()
            else -> normalizedAmount
        }
        val balanceUsd = selectedToken?.let { token ->
            val balance = token.balance.toBigDecimalOrNull() ?: return@let null
            val price = token.priceUsd.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return@let null
            balance * price
        }
        val insufficientBalance = increase && amountValue != null && balanceUsd != null && amountValue > balanceUsd
        val isCurrentWallet = !currentPosition.walletId.isNullOrBlank() && currentPosition.walletId == Session.getAccountId()
        val canSubmit = !loading && !exceedsReductionLimit && position?.state == PerpsPosition.STATE_OPEN && totalMargin != null &&
            isCurrentWallet &&
            if (increase) {
                selectedToken?.assetId in acceptedAssets && balanceUsd != null && !insufficientBalance
            } else {
                availableMargin?.let { amountValue != null && amountValue <= it } ?: true
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
            if (increase) {
                AnalyticsTracker.trackPerpsAddStart(AnalyticsTracker.PerpsAddType.ADD_MARGIN)
                viewModel.loadAcceptedAssets(
                    onSuccess = { ids ->
                        acceptedAssets = ids
                        viewModel.loadUsdTokens { tokens ->
                            selectedToken = tokens.filter { it.assetId in ids }
                                .maxByOrNull { it.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO }
                        }
                    },
                    onError = { error = it },
                )
            }
        }

        fun submit(submittedAmount: String) {
            if (loading || position?.state != PerpsPosition.STATE_OPEN || !isCurrentWallet) return
            val value = marginAdjustmentAmount(submittedAmount) ?: return
            if (marginAfterAdjustment(currentPosition.margin, submittedAmount, increase, availableMargin) == null) return
            val token = selectedToken
            if (increase && (token == null || token.assetId !in acceptedAssets || balanceUsd == null || value > balanceUsd)) return
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
                                entryPrice = currentPosition.entryPrice,
                                marginAssetPrice = token.priceUsd,
                                tokenSymbol = token.symbol,
                                priceScale = currentPosition.priceScale,
                                payUrl = data.paymentUrl,
                                isAddMargin = true,
                            ).show(parentFragmentManager, PerpsConfirmBottomSheetDialogFragment.TAG)
                        } else {
                            toast(R.string.perps_margin_submitted)
                            viewModel.refreshSinglePosition(currentPosition.positionId, currentPosition.walletId)
                            currentPosition.walletId?.let { viewModel.refreshOrders(it) }
                        }
                        dismiss()
                    } else if (response.errorCode == 10653) {
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
        MixinAppTheme {
            val reductionLimitError = if (exceedsReductionLimit && maximumReduction != null) {
                val limit = if (reduceByPercent) {
                    "${marginReductionPercentage(currentPosition.margin, maximumReduction)?.stripTrailingZeros()?.toPlainString().orEmpty()}%"
                } else formatPerpsExactUsdDecimal(maximumReduction)
                stringResource(R.string.perps_available_margin, limit)
            } else null
            val errorText = if (insufficientBalance) stringResource(R.string.insufficient_balance) else error
            val onSubmit = {
                if (increase) {
                    submit(normalizedAmount)
                } else if (canSubmit && parentFragmentManager.findFragmentByTag(PerpsCloseBottomSheetDialogFragment.TAG) == null) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    val approvedAmount = normalizedAmount
                    PerpsCloseBottomSheetDialogFragment.newInstance(currentPosition.toPosition(), reduceMarginAmount = approvedAmount)
                        .setOnMarginConfirmed { submitCurrent(approvedAmount) }
                        .show(parentFragmentManager, PerpsCloseBottomSheetDialogFragment.TAG)
                }
            }
            val actionTitle = stringResource(if (increase) R.string.Add else R.string.perps_reduce_action)
            KeyboardAwareBox(
                modifier = Modifier.fillMaxSize().background(MixinAppTheme.colors.background),
                content = { availableHeight ->
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                        ) {
                            val priceScale = market?.priceScale ?: currentPosition.priceScale
                            val entryPrice = formatPerpsPrice(currentPosition.entryPrice, priceScale)
                            val currentPrice = formatPerpsPrice(
                                market?.last?.takeIf { it.isNotBlank() } ?: currentPosition.markPrice ?: currentPosition.entryPrice,
                                priceScale,
                            )
                            val subtitle = stringResource(R.string.auto_close_subtitle_after_open, entryPrice, currentPrice)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CoilImage(
                                    model = currentPosition.iconUrl,
                                    placeholder = R.drawable.ic_avatar_place_holder,
                                    modifier = Modifier.size(30.dp).clip(CircleShape),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(
                                            if (increase) R.string.perps_add_margin_title else R.string.perps_reduce_margin_title,
                                            stringResource(if (currentPosition.side.equals("long", true)) R.string.Long else R.string.Short),
                                            currentPosition.tokenSymbol ?: currentPosition.displaySymbol.orEmpty(),
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
                                IconButton(onClick = { dismiss() }, enabled = !loading) {
                                    Icon(
                                        painterResource(R.drawable.ic_circle_close),
                                        stringResource(R.string.close),
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(26.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
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
                                        selectClick = if (!loading) ({
                                            TokenListBottomSheetDialogFragment.newInstance(
                                                fromType = TokenListBottomSheetDialogFragment.TYPE_FROM_PERP,
                                                currentAssetId = selectedToken?.assetId,
                                            ).setOnAssetClick { token ->
                                                if (token.assetId in acceptedAssets) selectedToken = token
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
                                    errorText = reductionLimitError,
                                    enabled = !loading,
                                    onInputChanged = ::changeAmount,
                                    onToggleMode = {
                                        amount = normalizedAmount
                                        inputIsPercentage = false
                                        reduceByPercent = !reduceByPercent
                                        preferences.putBoolean(PREF_REDUCE_BY_PERCENT, reduceByPercent)
                                        error = null
                                    },
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                val currentMarginText = currentPosition.margin?.toBigDecimalOrNull()?.let(::formatPerpsExactUsdDecimal) ?: "-"
                                val validTotalMargin = totalMargin?.takeIf { !insufficientBalance && !exceedsReductionLimit }
                                PerpsAddInfoRow(
                                    title = stringResource(R.string.Margin),
                                    value = if (validTotalMargin != null) "$currentMarginText → ${formatPerpsExactUsdDecimal(validTotalMargin)}" else currentMarginText,
                                    onTipClick = {
                                        PerpetualGuideBottomSheetDialogFragment.newInstance(PerpetualGuideBottomSheetDialogFragment.TAB_LEVERAGE)
                                            .show(parentFragmentManager, PerpetualGuideBottomSheetDialogFragment.TAG)
                                    },
                                )
                                Spacer(Modifier.height(16.dp))
                                PerpsAddInfoRow(
                                    title = stringResource(R.string.Liquidation_Price),
                                    value = currentPosition.liquidationPrice?.takeIf { it.isNotBlank() }?.let { formatPerpsPrice(it, priceScale) } ?: "-",
                                    onTipClick = {
                                        PerpetualGuideBottomSheetDialogFragment.newInstance(PerpetualGuideBottomSheetDialogFragment.TAB_LIQUIDATION)
                                            .show(parentFragmentManager, PerpetualGuideBottomSheetDialogFragment.TAG)
                                    },
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                        if (availableHeight == null) {
                            PerpsMarginActions(increase, actionTitle, errorText, canSubmit, loading, onCancel = { dismiss() }, onSubmit = onSubmit)
                        }
                    }
                },
                floating = {
                    PerpsMarginActions(increase, actionTitle, errorText, canSubmit, loading, onCancel = { dismiss() }, onSubmit = onSubmit)
                },
            )
        }
    }

    override fun showError(error: String) = Unit
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
        errorText?.let {
            Text(
                text = it,
                color = MixinAppTheme.colors.walletRed,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
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
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val inputValue = input.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val maximum = if (isPercentage) BigDecimal(100) else margin ?: BigDecimal.ZERO
    val inputColor = when {
        errorText != null -> MixinAppTheme.colors.walletRed
        inputValue <= BigDecimal.ZERO -> MixinAppTheme.colors.textRemarks
        else -> MixinAppTheme.colors.textPrimary
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    fun selectPercentage(value: BigDecimal) {
        val selected = if (isPercentage) value else reduceMarginAmount(margin?.toPlainString(), value.toPlainString(), true) ?: BigDecimal.ZERO
        onInputChanged(selected.stripTrailingZeros().toPlainString())
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onInputChanged((inputValue - BigDecimal.ONE).max(BigDecimal.ZERO).stripTrailingZeros().toPlainString()) },
                enabled = enabled && inputValue > BigDecimal.ZERO,
            ) {
                Icon(painterResource(R.drawable.ic_perps_minus), stringResource(R.string.perps_reduce_action), tint = Color.Unspecified, modifier = Modifier.size(16.dp))
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                if (!isPercentage) Text(PERPS_USD_SYMBOL, fontSize = 48.sp, fontWeight = FontWeight.W500, color = inputColor)
                BasicTextField(
                    value = input,
                    onValueChange = { value ->
                        if (value.length <= 40 && value.all { it in '0'..'9' || it == '.' } && value.count { it == '.' } <= 1 &&
                            value.substringAfter('.', "").length <= TRADE_INPUT_MAX_DECIMAL_PLACES) onInputChanged(value)
                    },
                    enabled = enabled,
                    singleLine = true,
                    modifier = Modifier.width(IntrinsicSize.Min).widthIn(min = 20.dp, max = 160.dp).focusRequester(focusRequester),
                    textStyle = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.W500, color = inputColor, textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    cursorBrush = SolidColor(MixinAppTheme.colors.accent),
                    decorationBox = { field ->
                        if (input.isEmpty()) Text("0", fontSize = 48.sp, fontWeight = FontWeight.W500, color = inputColor)
                        field()
                    },
                )
                if (isPercentage) Text("%", fontSize = 48.sp, fontWeight = FontWeight.W500, color = inputColor)
            }
            IconButton(
                onClick = { onInputChanged((inputValue + BigDecimal.ONE).min(maximum).stripTrailingZeros().toPlainString()) },
                enabled = enabled && inputValue < maximum,
            ) {
                Icon(painterResource(R.drawable.ic_perps_add), stringResource(R.string.Add), tint = Color.Unspecified, modifier = Modifier.size(16.dp))
            }
        }
        Row(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = enabled, onClick = onToggleMode).padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isPercentage) formatPerpsExactUsdDecimal(amount) else "${percentage?.stripTrailingZeros()?.toPlainString() ?: "0"}%",
                color = MixinAppTheme.colors.textAssist,
                fontSize = 12.sp,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                painterResource(R.drawable.ic_switch),
                stringResource(if (isPercentage) R.string.Amount else R.string.perps_percentage),
                tint = MixinAppTheme.colors.textRemarks,
                modifier = Modifier.size(16.dp),
            )
        }
        errorText?.let {
            Text(it, color = MixinAppTheme.colors.walletRed, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(12.dp))
        Slider(
            value = (percentage?.toFloat() ?: 0f).coerceIn(0f, 100f),
            onValueChange = { selectPercentage(BigDecimal(it.toString()).setScale(2, RoundingMode.HALF_UP)) },
            enabled = enabled && margin != null && margin > BigDecimal.ZERO,
            valueRange = 0f..100f,
            modifier = Modifier.fillMaxWidth().height(32.dp),
            colors = SliderDefaults.colors(
                thumbColor = MixinAppTheme.colors.accent,
                activeTrackColor = MixinAppTheme.colors.accent,
                inactiveTrackColor = MixinAppTheme.colors.backgroundGrayLight,
            ),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf(0, 25, 50, 75, 100).forEach { value ->
                Text(
                    text = if (isPercentage) "$value%" else formatPerpsUsdDecimal(reduceMarginAmount(margin?.toPlainString(), value.toString(), true)),
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 10.sp,
                    textAlign = when (value) { 0 -> TextAlign.Start; 100 -> TextAlign.End; else -> TextAlign.Center },
                    modifier = Modifier.weight(1f).clickable(enabled = enabled && margin != null && margin > BigDecimal.ZERO) {
                        selectPercentage(BigDecimal(value))
                    }.padding(vertical = 12.dp),
                )
            }
        }
    }
}
