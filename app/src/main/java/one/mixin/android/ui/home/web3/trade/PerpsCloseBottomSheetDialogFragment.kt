package one.mixin.android.ui.home.web3.trade

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsPosition
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.composeDp
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.putLong
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.home.web3.components.ActionBottom
import one.mixin.android.ui.wallet.components.WalletLabel
import one.mixin.android.util.SystemUIManager
import timber.log.Timber
import java.math.BigDecimal

@AndroidEntryPoint
class PerpsCloseBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {

    companion object {
        const val TAG = "PerpsCloseBottomSheetDialogFragment"
        private const val ARGS_POSITION_ID = "args_position_id"
        private const val ARGS_MARKET_SYMBOL = "args_market_symbol"
        private const val ARGS_SIDE = "args_side"
        private const val ARGS_QUANTITY = "args_quantity"
        private const val ARGS_LEVERAGE = "args_leverage"
        private const val ARGS_ENTRY_PRICE = "args_entry_price"
        private const val ARGS_MARK_PRICE = "args_mark_price"
        private const val ARGS_UNREALIZED_PNL = "args_unrealized_pnl"
        private const val ARGS_ROE = "args_roe"
        private const val ARGS_WALLET_NAME = "args_wallet_name"

        fun newInstance(
            position: PerpsPosition,
            walletName: String
        ): PerpsCloseBottomSheetDialogFragment {
            return PerpsCloseBottomSheetDialogFragment().withArgs {
                putString(ARGS_POSITION_ID, position.positionId)
                putString(ARGS_MARKET_SYMBOL, position.marketSymbol)
                putString(ARGS_SIDE, position.side)
                putString(ARGS_QUANTITY, position.quantity)
                putInt(ARGS_LEVERAGE, position.leverage)
                putString(ARGS_ENTRY_PRICE, position.entryPrice)
                putString(ARGS_MARK_PRICE, position.markPrice)
                putString(ARGS_UNREALIZED_PNL, position.unrealizedPnl)
                putString(ARGS_ROE, position.roe)
                putString(ARGS_WALLET_NAME, walletName)
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

    enum class Step {
        Pending,
        Sending,
        Done,
        Error,
    }

    private val positionId by lazy { requireNotNull(requireArguments().getString(ARGS_POSITION_ID)) }
    private val marketSymbol by lazy { requireNotNull(requireArguments().getString(ARGS_MARKET_SYMBOL)) }
    private val side by lazy { requireNotNull(requireArguments().getString(ARGS_SIDE)) }
    private val quantity by lazy { requireNotNull(requireArguments().getString(ARGS_QUANTITY)) }
    private val leverage by lazy { requireArguments().getInt(ARGS_LEVERAGE) }
    private val entryPrice by lazy { requireNotNull(requireArguments().getString(ARGS_ENTRY_PRICE)) }
    private val markPrice by lazy { requireNotNull(requireArguments().getString(ARGS_MARK_PRICE)) }
    private val unrealizedPnl by lazy { requireNotNull(requireArguments().getString(ARGS_UNREALIZED_PNL)) }
    private val roe by lazy { requireNotNull(requireArguments().getString(ARGS_ROE)) }
    private val walletName by lazy { requireNotNull(requireArguments().getString(ARGS_WALLET_NAME)) }

    private var step by mutableStateOf(Step.Pending)
    private var errorInfo: String? by mutableStateOf(null)
    private var isLoading by mutableStateOf(true)
    
    private var latestMarkPrice by mutableStateOf(markPrice)
    private var latestUnrealizedPnl by mutableStateOf(unrealizedPnl)
    private var latestRoe by mutableStateOf(roe)

    @Composable
    override fun ComposeContent() {
        androidx.compose.runtime.LaunchedEffect(positionId) {
            viewModel.loadPositionDetail(
                positionId = positionId,
                onSuccess = { position ->
                    latestMarkPrice = position.markPrice
                    latestUnrealizedPnl = position.unrealizedPnl
                    latestRoe = position.roe
                    isLoading = false
                },
                onError = { error ->
                    Timber.e("Failed to load position detail: $error")
                    isLoading = false
                }
            )
        }

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
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .background(MixinAppTheme.colors.backgroundWindow),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = marketSymbol.take(3),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MixinAppTheme.colors.accent
                                )
                            }
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

                    val pnl = try {
                        BigDecimal(latestUnrealizedPnl)
                    } catch (e: Exception) {
                        BigDecimal.ZERO
                    }
                    val pnlColor = if (pnl >= BigDecimal.ZERO) {
                        MixinAppTheme.colors.walletGreen
                    } else {
                        MixinAppTheme.colors.walletRed
                    }
                    
                    val estimatedReceive = try {
                        val margin = BigDecimal(quantity)
                        margin + pnl
                    } catch (e: Exception) {
                        BigDecimal.ZERO
                    }

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                color = MixinAppTheme.colors.accent
                            )
                        }
                    } else {
                        CloseInfoItem(
                            title = "Estimate Receive".uppercase(),
                            value = "${if (estimatedReceive >= BigDecimal.ZERO) "+" else ""}${String.format("%.2f", estimatedReceive)} USDT",
                            valueColor = if (estimatedReceive >= BigDecimal.ZERO) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed,
                            subValue = "Ethereum"
                        )
                        Box(modifier = Modifier.height(20.dp))

                        CloseInfoItem(
                            title = "PnL".uppercase(),
                            value = "${if (pnl >= BigDecimal.ZERO) "+" else ""}${latestUnrealizedPnl} USDT (${latestRoe}%)",
                            valueColor = pnlColor
                        )
                        Box(modifier = Modifier.height(20.dp))

                        CloseInfoItem(
                            title = stringResource(R.string.Receiver).uppercase(),
                            value = walletName
                        )
                        Box(modifier = Modifier.height(20.dp))

                        CloseInfoItem(
                            title = stringResource(R.string.Sender).uppercase(),
                            value = "Mixin Futures (7000105155)"
                        )
                        Box(modifier = Modifier.height(20.dp))

                        CloseInfoItem(
                            title = "Memo".uppercase(),
                            value = positionId
                        )
                    }
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
                                confirmAction = { closePosition() },
                            )
                        }
                        Step.Pending -> {
                            ActionBottom(
                                modifier = Modifier.align(Alignment.BottomCenter),
                                cancelTitle = stringResource(R.string.Cancel),
                                confirmTitle = stringResource(id = R.string.Confirm),
                                cancelAction = { dismiss() },
                                confirmAction = { closePosition() },
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
    private fun CloseInfoItem(
        title: String,
        value: String,
        valueColor: Color = MixinAppTheme.colors.textPrimary,
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
                color = valueColor,
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

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop()
    }

    private var onDoneAction: (() -> Unit)? = null

    fun setOnDone(callback: () -> Unit): PerpsCloseBottomSheetDialogFragment {
        onDoneAction = callback
        return this
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
    }

    private fun closePosition() = lifecycleScope.launch(Dispatchers.IO) {
        try {
            step = Step.Sending
            
            viewModel.closePerpsOrder(
                positionId = positionId,
                onSuccess = {
                    step = Step.Done
                },
                onError = { error ->
                    errorInfo = error
                    step = Step.Error
                }
            )
        } catch (e: Exception) {
            handleException(e)
        }
    }

    private fun handleException(t: Throwable) {
        Timber.e(t)
        errorInfo = t.message ?: t.toString()
        step = Step.Error
    }

    override fun dismiss() {
        dismissAllowingStateLoss()
    }

    override fun showError(error: String) {
        errorInfo = error
        step = Step.Error
    }
}
