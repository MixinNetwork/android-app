package one.mixin.android.ui.home.web3.trade

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
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
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
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
import one.mixin.android.api.request.perps.OpenOrderResponse
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.composeDp
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.putLong
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.common.UtxoConsolidationBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.buildTransferBiometricItem
import one.mixin.android.ui.home.web3.components.ActionBottom
import one.mixin.android.ui.wallet.components.WalletLabel
import one.mixin.android.util.SystemUIManager
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.toUser
import one.mixin.android.web3.js.Web3Signer
import timber.log.Timber
import java.math.BigDecimal
import java.util.UUID
import kotlin.math.abs

@AndroidEntryPoint
class PerpsConfirmBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {

    companion object {
        const val TAG = "PerpsConfirmBottomSheetDialogFragment"
        private const val ARGS_MARKET_ID = "args_market_id"
        private const val ARGS_MARKET_SYMBOL = "args_market_symbol"
        private const val ARGS_IS_LONG = "args_is_long"
        private const val ARGS_AMOUNT = "args_amount"
        private const val ARGS_LEVERAGE = "args_leverage"
        private const val ARGS_ENTRY_PRICE = "args_entry_price"
        private const val ARGS_LIQUIDATION_PRICE = "args_liquidation_price"
        private const val ARGS_TOKEN_SYMBOL = "args_token_symbol"
        private const val ARGS_TOKEN_ICON = "args_token_icon"
        private const val ARGS_TOKEN_ASSET_ID = "args_token_asset_id"
        private const val ARGS_WALLET_NAME = "args_wallet_name"
        private const val ARGS_PAY_URL = "args_pay_url"

        fun newInstance(
            marketId: String,
            marketSymbol: String,
            isLong: Boolean,
            amount: String,
            leverage: Int,
            entryPrice: String,
            liquidationPrice: String,
            tokenSymbol: String,
            tokenIcon: String,
            tokenAssetId: String,
            walletName: String,
            payUrl: String?
        ): PerpsConfirmBottomSheetDialogFragment {
            return PerpsConfirmBottomSheetDialogFragment().withArgs {
                putString(ARGS_MARKET_ID, marketId)
                putString(ARGS_MARKET_SYMBOL, marketSymbol)
                putBoolean(ARGS_IS_LONG, isLong)
                putString(ARGS_AMOUNT, amount)
                putInt(ARGS_LEVERAGE, leverage)
                putString(ARGS_ENTRY_PRICE, entryPrice)
                putString(ARGS_LIQUIDATION_PRICE, liquidationPrice)
                putString(ARGS_TOKEN_SYMBOL, tokenSymbol)
                putString(ARGS_TOKEN_ICON, tokenIcon)
                putString(ARGS_TOKEN_ASSET_ID, tokenAssetId)
                putString(ARGS_WALLET_NAME, walletName)
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

    private val viewModel by viewModels<PerpetualViewModel>()
    private val bottomViewModel by viewModels<one.mixin.android.ui.common.BottomSheetViewModel>()

    enum class Step {
        Pending,
        Sending,
        Done,
        Error,
    }

    private val marketId by lazy { requireNotNull(requireArguments().getString(ARGS_MARKET_ID)) }
    private val marketSymbol by lazy { requireNotNull(requireArguments().getString(ARGS_MARKET_SYMBOL)) }
    private val isLong by lazy { requireArguments().getBoolean(ARGS_IS_LONG) }
    private val amount by lazy { requireNotNull(requireArguments().getString(ARGS_AMOUNT)) }
    private val leverage by lazy { requireArguments().getInt(ARGS_LEVERAGE) }
    private val entryPrice by lazy { requireNotNull(requireArguments().getString(ARGS_ENTRY_PRICE)) }
    private val liquidationPrice by lazy { requireNotNull(requireArguments().getString(ARGS_LIQUIDATION_PRICE)) }
    private val tokenSymbol by lazy { requireNotNull(requireArguments().getString(ARGS_TOKEN_SYMBOL)) }
    private val tokenIcon by lazy { requireNotNull(requireArguments().getString(ARGS_TOKEN_ICON)) }
    private val tokenAssetId by lazy { requireNotNull(requireArguments().getString(ARGS_TOKEN_ASSET_ID)) }
    private val walletName by lazy { requireNotNull(requireArguments().getString(ARGS_WALLET_NAME)) }
    private val payUrl by lazy { requireArguments().getString(ARGS_PAY_URL) }

    private var step by mutableStateOf(Step.Pending)
    private var errorInfo: String? by mutableStateOf(null)

    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            Column(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(topStart = 8.composeDp, topEnd = 8.composeDp))
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(MixinAppTheme.colors.background),
            ) {
                WalletLabel(walletName = walletName, isWeb3 = true)
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
                            androidx.compose.material.Icon(
                                modifier = Modifier.size(70.dp),
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_transfer_status_failed),
                                contentDescription = null,
                                tint = Color.Unspecified,
                            )
                        }
                        Step.Done -> {
                            androidx.compose.material.Icon(
                                modifier = Modifier.size(70.dp),
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_transfer_status_success),
                                contentDescription = null,
                                tint = Color.Unspecified,
                            )
                        }
                        else -> {
                            CoilImage(
                                model = tokenIcon,
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
                                Step.Pending -> R.string.Perpetual
                                Step.Done -> R.string.web3_sending_success
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
                        text = errorInfo ?: "$marketSymbol - USD",
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

                    PerpsInfoItem(
                        title = stringResource(R.string.Perpetual_Direction).uppercase(),
                        value = "${if (isLong) stringResource(R.string.Long) else stringResource(R.string.Short)} ${leverage}x"
                    )
                    Box(modifier = Modifier.height(20.dp))

                    ProfitLossInfo(
                        amount = amount,
                        leverage = leverage,
                        isLong = isLong
                    )
                    Box(modifier = Modifier.height(20.dp))

                    PerpsInfoItem(
                        title = stringResource(R.string.Amount).uppercase() + " (Isolated)",
                        value = "$amount $tokenSymbol"
                    )
                    Box(modifier = Modifier.height(20.dp))

                    PerpsInfoItem(
                        title = "Entry Price".uppercase(),
                        value = "$$entryPrice"
                    )
                    Box(modifier = Modifier.height(20.dp))

                    PerpsInfoItem(
                        title = stringResource(R.string.Liquidation_Price).uppercase(),
                        value = "$$liquidationPrice",
                        subValue = calculateLiquidationPercentage(entryPrice, liquidationPrice, isLong)
                    )
                    Box(modifier = Modifier.height(20.dp))

                    PerpsInfoItem(
                        title = stringResource(R.string.Receiver).uppercase(),
                        value = "Mixin Futures (7000105155)"
                    )
                    Box(modifier = Modifier.height(20.dp))

                    PerpsInfoItem(
                        title = stringResource(R.string.Sender).uppercase(),
                        value = walletName
                    )
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
                                Button(
                                    onClick = {
                                        onDoneAction?.invoke()
                                        dismiss()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        backgroundColor = MixinAppTheme.colors.accent,
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 36.dp, vertical = 11.dp),
                                ) {
                                    Text(text = stringResource(id = R.string.Done), color = Color.White)
                                }
                            }
                        }
                        Step.Error -> {
                            ActionBottom(
                                modifier = Modifier.align(Alignment.BottomCenter),
                                cancelTitle = stringResource(R.string.Cancel),
                                confirmTitle = stringResource(id = R.string.Retry),
                                cancelAction = { dismiss() },
                                confirmAction = { showPin() },
                            )
                        }
                        Step.Pending -> {
                            ActionBottom(
                                modifier = Modifier.align(Alignment.BottomCenter),
                                cancelTitle = stringResource(R.string.Cancel),
                                confirmTitle = stringResource(id = R.string.Confirm),
                                cancelAction = { dismiss() },
                                confirmAction = { showPin() },
                            )
                        }
                        Step.Sending -> {}
                    }
                }
                Box(modifier = Modifier.height(36.dp))
            }
        }
    }

    @Composable
    private fun PerpsInfoItem(
        title: String,
        value: String,
        subValue: String? = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = title,
                color = MixinAppTheme.colors.textRemarks,
                fontSize = 14.sp,
                maxLines = 1,
            )
            Box(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.W400
            )
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

    @Composable
    private fun ProfitLossInfo(
        amount: String,
        leverage: Int,
        isLong: Boolean
    ) {
        val amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val profitPercent = 1.0 * leverage
        val profitAmount = amountValue * BigDecimal(profitPercent / 100)
        val lossPercent = 100.0 / leverage

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = "价格${if (isLong) "上涨" else "下跌"} 1% → 盈利 ${String.format("%.1f", profitPercent)}% (+$${String.format("%.2f", profitAmount)})",
                color = MixinAppTheme.colors.walletGreen,
                fontSize = 14.sp,
            )
            Box(modifier = Modifier.height(4.dp))
            Text(
                text = "价格${if (isLong) "下跌" else "上涨"} ${String.format("%.2f", lossPercent)}% → 亏损 -$${amount}",
                color = MixinAppTheme.colors.walletRed,
                fontSize = 14.sp,
            )
        }
    }

    private fun calculateLiquidationPercentage(entryPrice: String, liquidationPrice: String, isLong: Boolean): String {
        return try {
            val entry = BigDecimal(entryPrice)
            val liquidation = BigDecimal(liquidationPrice)
            val diff = if (isLong) {
                (entry - liquidation).divide(entry, 4, java.math.RoundingMode.HALF_UP) * BigDecimal(100)
            } else {
                (liquidation - entry).divide(entry, 4, java.math.RoundingMode.HALF_UP) * BigDecimal(100)
            }
            "价格${if (isLong) "下跌" else "上涨"} ${String.format("%.2f", diff)}% → 亏损 -$${amount}"
        } catch (e: Exception) {
            ""
        }
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
}
