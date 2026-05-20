package one.mixin.android.ui.home.web3.trade.perps

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.composeDp
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.putLong
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.common.UtxoConsolidationBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.buildTransferBiometricItem
import one.mixin.android.ui.home.web3.components.ActionBottom
import one.mixin.android.ui.tip.wc.compose.ItemWalletContent
import one.mixin.android.ui.wallet.ItemUserContent
import one.mixin.android.ui.wallet.components.WalletLabel
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.User
import one.mixin.android.vo.toUser
import one.mixin.android.widget.components.MixinButton
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@AndroidEntryPoint
class PerpsConfirmBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {

    companion object {
        const val TAG = "PerpsConfirmBottomSheetDialogFragment"
        private const val ARGS_MARKET_SYMBOL = "args_market_symbol"
        private const val ARGS_MARKET_ICON = "args_market_icon"
        private const val ARGS_IS_LONG = "args_is_long"
        private const val ARGS_AMOUNT = "args_amount"
        private const val ARGS_LEVERAGE = "args_leverage"
        private const val ARGS_ENTRY_PRICE = "args_entry_price"
        private const val ARGS_TOKEN_SYMBOL = "args_token_symbol"
        private const val ARGS_TAKE_PROFIT_PRICE = "args_take_profit_price"
        private const val ARGS_STOP_LOSS_PRICE = "args_stop_loss_price"
        private const val ARGS_PAY_URL = "args_pay_url"

        fun newInstance(
            marketSymbol: String,
            marketIcon: String,
            isLong: Boolean,
            amount: String,
            leverage: Int,
            entryPrice: String,
            tokenSymbol: String,
            takeProfitPrice: String? = null,
            stopLossPrice: String? = null,
            payUrl: String?,
        ): PerpsConfirmBottomSheetDialogFragment {
            return PerpsConfirmBottomSheetDialogFragment().withArgs {
                putString(ARGS_MARKET_SYMBOL, marketSymbol)
                putString(ARGS_MARKET_ICON, marketIcon)
                putBoolean(ARGS_IS_LONG, isLong)
                putString(ARGS_AMOUNT, amount)
                putInt(ARGS_LEVERAGE, leverage)
                putString(ARGS_ENTRY_PRICE, entryPrice)
                putString(ARGS_TOKEN_SYMBOL, tokenSymbol)
                putString(ARGS_TAKE_PROFIT_PRICE, takeProfitPrice)
                putString(ARGS_STOP_LOSS_PRICE, stopLossPrice)
                putString(ARGS_PAY_URL, payUrl)
            }
        }
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, R.style.MixinBottomSheet)
        dialog.window?.let { window ->
            SystemUIManager.lightUI(window, requireContext().isNightMode())
        }
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night),
            )
        }
    }

    private val bottomViewModel by viewModels<BottomSheetViewModel>()

    enum class Step {
        Pending,
        Sending,
        Done,
        Error,
    }

    private val marketSymbol by lazy { requireNotNull(requireArguments().getString(ARGS_MARKET_SYMBOL)) }
    private val marketIcon by lazy { requireNotNull(requireArguments().getString(ARGS_MARKET_ICON)) }
    private val isLong by lazy { requireArguments().getBoolean(ARGS_IS_LONG) }
    private val amount by lazy { requireNotNull(requireArguments().getString(ARGS_AMOUNT)) }
    private val leverage by lazy { requireArguments().getInt(ARGS_LEVERAGE) }
    private val entryPrice by lazy { requireNotNull(requireArguments().getString(ARGS_ENTRY_PRICE)) }
    private val tokenSymbol by lazy { requireNotNull(requireArguments().getString(ARGS_TOKEN_SYMBOL)) }
    private val takeProfitPrice by lazy { requireArguments().getString(ARGS_TAKE_PROFIT_PRICE).orEmpty() }
    private val stopLossPrice by lazy { requireArguments().getString(ARGS_STOP_LOSS_PRICE).orEmpty() }
    private val fiatRate by lazy { BigDecimal(Fiats.getRate()) }
    private val fiatSymbol by lazy { Fiats.getSymbol() }

    private val payUrl by lazy { requireArguments().getString(ARGS_PAY_URL) }
    private val entryFiatPrice by lazy {
        val price = entryPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
        "$PERPS_USD_SYMBOL${price.priceFormat()}"
    }
    private val takeProfitFiatPrice by lazy { formatOptionalPerpsPrice(takeProfitPrice) }
    private val stopLossFiatPrice by lazy { formatOptionalPerpsPrice(stopLossPrice) }

    private val liquidationPrice by lazy {
        try {
            if (leverage <= 0) {
                "${PERPS_USD_SYMBOL}0"
            } else {
                val price = entryPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
                if (price == BigDecimal.ZERO) {
                    "${PERPS_USD_SYMBOL}0"
                } else {
                    val liquidationPercent = BigDecimal(100.0 / leverage)
                    val liquidationRatio = liquidationPercent.divide(BigDecimal(100), 8, RoundingMode.HALF_UP)
                    val liquidation = if (isLong) {
                        price * (BigDecimal.ONE - liquidationRatio)
                    } else {
                        price * (BigDecimal.ONE + liquidationRatio)
                    }
                    "$PERPS_USD_SYMBOL${liquidation.priceFormat()}"
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to calculate liquidation price")
            "${PERPS_USD_SYMBOL}0"
        }
    }

    private var step by mutableStateOf(Step.Pending)
    private var errorInfo: String? by mutableStateOf(null)
    private var receiver: User? by mutableStateOf(null)

    @Composable
    override fun ComposeContent() {
        LaunchedEffect(payUrl) {
            payUrl?.let { url ->
                try {
                    val uri = Uri.parse(url)
                    val receiverId = uri.lastPathSegment
                    receiverId?.let { userId ->
                        receiver = bottomViewModel.refreshUser(userId)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse receiver from payUrl")
                }
            }
        }

        MixinAppTheme {
            Column(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(topStart = 8.composeDp, topEnd = 8.composeDp))
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(MixinAppTheme.colors.background),
            ) {
                WalletLabel(walletName = getString(R.string.Privacy_Wallet), isWeb3 = false)
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .weight(weight = 1f, fill = true),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(modifier = Modifier.height(50.dp))
                    when (step) {
                        Step.Sending -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(70.dp),
                                color = MixinAppTheme.colors.accent,
                            )
                        }

                        Step.Error -> {
                            Icon(
                                modifier = Modifier.size(70.dp),
                                painter = painterResource(id = R.drawable.ic_transfer_status_failed),
                                contentDescription = null,
                                tint = Color.Unspecified,
                            )
                        }

                        Step.Done -> {
                            Icon(
                                modifier = Modifier.size(70.dp),
                                painter = painterResource(id = R.drawable.ic_transfer_status_success),
                                contentDescription = null,
                                tint = Color.Unspecified,
                            )
                        }

                        else -> {
                            CoilImage(
                                model = marketIcon,
                                placeholder = R.drawable.ic_avatar_place_holder,
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                    Box(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(
                            id = when (step) {
                                Step.Pending -> R.string.confirm_opening_position
                                Step.Done -> R.string.Position_Submitted
                                Step.Error -> R.string.swap_failed
                                Step.Sending -> R.string.Sending
                            }
                        ),
                        style = TextStyle(
                            color = MixinAppTheme.colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.W600,
                        ),
                    )
                    Box(modifier = Modifier.height(8.dp))
                    Text(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = errorInfo ?: stringResource(
                            id =
                                when (step) {
                                    Step.Done -> R.string.swap_message_success
                                    Step.Error -> R.string.Data_error
                                    else -> R.string.swap_inner_desc
                                },
                        ),
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            color = if (errorInfo != null) MixinAppTheme.colors.tipError else MixinAppTheme.colors.textMinor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W400,
                        ),
                        maxLines = 3,
                        minLines = 3,
                    )
                    Box(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .height(10.dp)
                            .fillMaxWidth()
                            .background(MixinAppTheme.colors.backgroundWindow),
                    )
                    Box(modifier = Modifier.height(20.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.perps_market).uppercase(),
                            color = MixinAppTheme.colors.textRemarks,
                            fontSize = 14.sp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CoilImage(
                                model = marketIcon,
                                placeholder = R.drawable.ic_avatar_place_holder,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = marketSymbol,
                                color = MixinAppTheme.colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.W400
                            )
                        }
                    }
                    Box(modifier = Modifier.height(20.dp))

                    PerpsInfoItem(
                        title = stringResource(R.string.Direction).uppercase(),
                        value = "${if (isLong) stringResource(R.string.Long) else stringResource(R.string.Short)} ${leverage}x"
                    )
                    Box(modifier = Modifier.height(6.dp))
                    ProfitLossInfo(
                        amount = amount,
                        leverage = leverage,
                        isLong = isLong
                    )
                    Box(modifier = Modifier.height(20.dp))

                    PerpsInfoItem(
                        title = stringResource(R.string.Amount).uppercase(),
                        value = "$amount $tokenSymbol"
                    )
                    Box(modifier = Modifier.height(20.dp))

                    PerpsInfoItem(
                        title = stringResource(R.string.Entry_Price).uppercase(),
                        value = entryFiatPrice
                    )
                    Box(modifier = Modifier.height(20.dp))

                    takeProfitFiatPrice?.let { takeProfit ->
                        val tpSubValue = calculateTpSlSubValue(
                            targetPrice = takeProfitPrice,
                            entryPrice = entryPrice,
                            leverage = leverage,
                            amount = amount,
                            isLong = isLong,
                            isTakeProfit = true,
                        )
                        PerpsInfoItem(
                            title = stringResource(R.string.Take_Profit).uppercase(),
                            value = takeProfit,
                            subValueAnnotated = tpSubValue,
                            info = true,
                            guideTab = PerpetualGuideBottomSheetDialogFragment.TAB_TP_SL,
                        )
                        Box(modifier = Modifier.height(20.dp))
                    }

                    stopLossFiatPrice?.let { stopLoss ->
                        val slSubValue = calculateTpSlSubValue(
                            targetPrice = stopLossPrice,
                            entryPrice = entryPrice,
                            leverage = leverage,
                            amount = amount,
                            isLong = isLong,
                            isTakeProfit = false,
                        )
                        PerpsInfoItem(
                            title = stringResource(R.string.Stop_Loss).uppercase(),
                            value = stopLoss,
                            subValueAnnotated = slSubValue,
                            info = true,
                            guideTab = PerpetualGuideBottomSheetDialogFragment.TAB_TP_SL,
                        )
                        Box(modifier = Modifier.height(20.dp))
                    }

                    val lossPercent = remember(leverage) {
                        val percent = String.format("%.2f", 100.0 / leverage)
                        Timber.d("LossPercent - leverage: $leverage, lossPercent: $percent")
                        percent
                    }

                    val lossSubValue = if (isLong) {
                        val text = stringResource(
                            R.string.Price_Down_Loss,
                            lossPercent,
                            amount,
                            tokenSymbol
                        )
                        Timber.d("LossSubValue (Long) - lossPercent: $lossPercent, amount: $amount, tokenSymbol: $tokenSymbol, text: $text")
                        text
                    } else {
                        val text = stringResource(
                            R.string.Price_Up_Loss,
                            lossPercent,
                            amount,
                            tokenSymbol
                        )
                        Timber.d("LossSubValue (Short) - lossPercent: $lossPercent, amount: $amount, tokenSymbol: $tokenSymbol, text: $text")
                        text
                    }

                    PerpsInfoItem(
                        title = stringResource(R.string.Estimated_Liquidation_Price).uppercase(),
                        value = liquidationPrice,
                        subValue = lossSubValue,
                        info = true,
                        guideTab = PerpetualGuideBottomSheetDialogFragment.TAB_LIQUIDATION,
                    )

                    Box(modifier = Modifier.height(20.dp))

                    ItemUserContent(title = stringResource(id = R.string.Receiver).uppercase(), receiver, null)
                    Box(modifier = Modifier.height(20.dp))

                    ItemWalletContent(title = stringResource(id = R.string.Sender).uppercase(), fontSize = 16.sp)
                    Box(modifier = Modifier.height(16.dp))
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    when (step) {
                        Step.Done -> {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .background(MixinAppTheme.colors.background)
                                    .padding(20.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                MixinButton(
                                    onClick = {
                                        onDoneAction?.invoke()
                                        dismiss()
                                    },
                                    shape = RoundedCornerShape(30.dp),
                                    contentPadding = PaddingValues(horizontal = 35.dp, vertical = 10.dp),
                                ) {
                                    Text(text = stringResource(id = R.string.Done), fontSize = 16.sp, color = Color.White)
                                }
                            }
                        }

                        Step.Error -> {
                            ActionBottom(
                                modifier = Modifier.align(Alignment.BottomCenter),
                                cancelTitle = stringResource(R.string.Cancel),
                                confirmTitle = stringResource(id = R.string.Retry),
                                cancelAction = {
                                    AnalyticsTracker.trackPerpsPreviewCancel()
                                    dismiss()
                                },
                                confirmAction = {
                                    AnalyticsTracker.trackPerpsPreviewConfirm()
                                    showPin()
                                },
                            )
                        }

                        Step.Pending -> {
                            ActionBottom(
                                modifier = Modifier.align(Alignment.BottomCenter),
                                cancelTitle = stringResource(R.string.Cancel),
                                confirmTitle = stringResource(id = R.string.Confirm),
                                cancelAction = {
                                    AnalyticsTracker.trackPerpsPreviewCancel()
                                    dismiss()
                                },
                                confirmAction = {
                                    AnalyticsTracker.trackPerpsPreviewConfirm()
                                    showPin()
                                },
                            )
                        }

                        Step.Sending -> {}
                    }
                }
                Box(modifier = Modifier.height(32.dp))
            }
        }
    }

    @Composable
    private fun PerpsInfoItem(
        title: String,
        value: String,
        subValue: String? = null,
        subValueAnnotated: AnnotatedString? = null,
        info: Boolean = false,
        guideTab: Int = PerpetualGuideBottomSheetDialogFragment.TAB_POSITION,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = MixinAppTheme.colors.textRemarks,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
                if (info) {
                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        painter = painterResource(id = R.drawable.ic_tip),
                        contentDescription = null,
                        modifier = Modifier
                            .size(12.dp)
                            .clickable {
                                PerpetualGuideBottomSheetDialogFragment.newInstance(
                                    guideTab
                                ).show(parentFragmentManager, PerpetualGuideBottomSheetDialogFragment.TAG)
                            },
                        tint = MixinAppTheme.colors.textAssist
                    )
                }
            }
            Box(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.W400
            )
            if (subValueAnnotated != null) {
                Box(modifier = Modifier.height(4.dp))
                Text(
                    text = subValueAnnotated,
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 14.sp,
                )
            } else {
                subValue?.let {
                    Box(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        color = MixinAppTheme.colors.textAssist,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }

    @Composable
    private fun ProfitLossInfo(
        amount: String,
        leverage: Int,
        isLong: Boolean,
    ) {
        val context = LocalContext.current
        val amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val profitPercent = 1.0 * leverage
        val profitAmount = amountValue * BigDecimal(profitPercent / 100)
        val formattedProfitAmount = formatPerpsUsdDecimal(profitAmount)

        Timber.d("ProfitLossInfo - amount: $amount, amountValue: $amountValue, leverage: $leverage, isLong: $isLong, profitPercent: $profitPercent, profitAmount: $profitAmount")

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            text = context.formatPerpsProfitPreview(
                isLong = isLong,
                priceChangeText = "1",
                profitPercentText = String.format("%.1f", profitPercent),
                profitAmountText = formattedProfitAmount,
            ),
            color = MixinAppTheme.colors.textAssist,
            fontSize = 14.sp,
        )
    }


    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop()
    }

    private var onDoneAction: (() -> Unit)? = null
    private var onDestroyAction: (() -> Unit)? = null

    fun setOnDone(callback: () -> Unit): PerpsConfirmBottomSheetDialogFragment {
        onDoneAction = callback
        return this
    }

    fun setOnDestroy(callback: () -> Unit): PerpsConfirmBottomSheetDialogFragment {
        onDestroyAction = callback
        return this
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDestroyAction?.invoke()
    }

    private fun showPin() {
        PinInputBottomSheetDialogFragment.newInstance(biometricInfo = getBiometricInfo(), from = 1)
            .setOnPinComplete { pin ->
                lifecycleScope.launch(
                    CoroutineExceptionHandler { _, error ->
                        handleException(error)
                    },
                ) {
                    doAfterPinComplete(pin)
                }
            }.showNow(parentFragmentManager, PinInputBottomSheetDialogFragment.TAG)
    }

    private fun doAfterPinComplete(pin: String) = lifecycleScope.launch(Dispatchers.IO) {
        try {
            step = Step.Sending

            if (payUrl != null) {
                handlePayment(payUrl!!, pin)
            } else {
                defaultSharedPreferences.putLong(
                    Constants.BIOMETRIC_PIN_CHECK,
                    System.currentTimeMillis(),
                )
                context?.updatePinCheck()
                step = Step.Done
                trackOpenPositionSuccess()
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    private fun handleException(t: Throwable) {
        Timber.e(t)
        errorInfo = t.message ?: t.toString()
        step = Step.Error
    }

    fun getBiometricInfo() = BiometricInfo(
        getString(R.string.Verify_by_Biometric),
        "",
        "",
    )

    private suspend fun handlePayment(payUrl: String, pin: String) {
        try {
            val uri = Uri.parse(payUrl)

            val assetId = requireNotNull(uri.getQueryParameter("asset"))
            val payAmount = requireNotNull(uri.getQueryParameter("amount"))
            val receiverId = requireNotNull(uri.lastPathSegment)
            val memo = uri.getQueryParameter("memo")
            val traceId = uri.getQueryParameter("trace") ?: UUID.randomUUID().toString()

            val consolidationAmount = bottomViewModel.checkUtxoSufficiency(assetId, payAmount)
            val token = bottomViewModel.findAssetItemById(assetId)

            if (consolidationAmount != null && token != null) {
                UtxoConsolidationBottomSheetDialogFragment.newInstance(
                    buildTransferBiometricItem(
                        Session.getAccount()!!.toUser(),
                        token,
                        consolidationAmount,
                        UUID.randomUUID().toString(),
                        null,
                        null
                    )
                ).show(parentFragmentManager, UtxoConsolidationBottomSheetDialogFragment.TAG)
                step = Step.Pending
                return
            } else if (token == null) {
                errorInfo = getString(R.string.Data_error)
                step = Step.Error
                return
            }

            val paymentResponse = bottomViewModel.kernelTransaction(
                assetId,
                listOf(receiverId),
                1.toByte(),
                payAmount,
                pin,
                traceId,
                memo
            )

            if (paymentResponse.isSuccess) {
                defaultSharedPreferences.putLong(
                    Constants.BIOMETRIC_PIN_CHECK,
                    System.currentTimeMillis(),
                )
                context?.updatePinCheck()
                step = Step.Done
                trackOpenPositionSuccess()
            } else {
                errorInfo = paymentResponse.errorDescription
                step = Step.Error
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    override fun dismiss() {
        dismissAllowingStateLoss()
    }

    override fun showError(error: String) {
        errorInfo = error
        step = Step.Error
    }

    private fun trackOpenPositionSuccess() {
        AnalyticsTracker.trackPerpsOpenPositionEnd(
            leverage = leverage,
            amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            price = entryPrice,
        )
    }
}

private fun formatOptionalPerpsPrice(rawPrice: String): String? {
    val value = rawPrice.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return null
    return "$PERPS_USD_SYMBOL${value.priceFormat()}"
}

@Composable
private fun calculateTpSlSubValue(
    targetPrice: String,
    entryPrice: String,
    leverage: Int,
    amount: String,
    isLong: Boolean,
    isTakeProfit: Boolean,
): AnnotatedString? {
    val target = targetPrice.toBigDecimalOrNull() ?: return null
    val entry = entryPrice.toBigDecimalOrNull() ?: return null
    val margin = amount.toBigDecimalOrNull() ?: return null
    if (entry <= BigDecimal.ZERO || target <= BigDecimal.ZERO) return null

    val priceChangePercent = target.subtract(entry)
        .multiply(BigDecimal(100))
        .divide(entry, 2, RoundingMode.HALF_UP)

    val absPriceChange = priceChangePercent.abs()
    val pnlPercent = absPriceChange.multiply(BigDecimal(leverage))
    val pnlAmount = margin.multiply(pnlPercent).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
    val priceChangeStr = absPriceChange.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    val context = LocalContext.current
    val quoteColorReversed = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val profitColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val lossColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed

    val isPriceUp = if (isLong) isTakeProfit else !isTakeProfit
    val priceChangeText = if (isPriceUp) {
        "+$priceChangeStr%"
    } else {
        "-$priceChangeStr%"
    }

    return if (isTakeProfit) {
        val amountText = "+${formatPerpsRawUsdDecimal(pnlAmount)}"
        val pnlPercentStr = pnlPercent.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        val outcomeText = "$amountText ($pnlPercentStr%)"
        val fullText = stringResource(R.string.price_change_take_profit, priceChangeText, outcomeText)
        buildAnnotatedString {
            append(fullText)
            val outcomeStart = fullText.indexOf(outcomeText)
            if (outcomeStart >= 0) {
                addStyle(
                    style = SpanStyle(color = profitColor),
                    start = outcomeStart,
                    end = outcomeStart + outcomeText.length,
                )
            }
        }
    } else {
        val amountText = "-${formatPerpsRawUsdDecimal(pnlAmount)}"
        val fullText = stringResource(R.string.price_change_stop_loss, priceChangeText, amountText)
        buildAnnotatedString {
            append(fullText)
            val outcomeStart = fullText.indexOf(amountText)
            if (outcomeStart >= 0) {
                addStyle(
                    style = SpanStyle(color = lossColor),
                    start = outcomeStart,
                    end = outcomeStart + amountText.length,
                )
            }
        }
    }
}
