package one.mixin.android.ui.home.web3.trade.perps

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
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
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsPosition
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.composeDp
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.components.ActionBottom
import one.mixin.android.ui.tip.wc.compose.ItemWalletContent
import one.mixin.android.ui.wallet.ItemUserContent
import one.mixin.android.ui.wallet.components.WalletLabel
import one.mixin.android.util.SystemUIManager
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.TokenItem
import timber.log.Timber
import java.math.BigDecimal

@AndroidEntryPoint
class PerpsCloseBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {

    companion object {
        const val TAG = "PerpsCloseBottomSheetDialogFragment"
        private const val ARGS_POSITION_ID = "args_position_id"
        private const val ARGS_SIDE = "args_side"
        private const val ARGS_MARGIN = "args_margin"
        private const val ARGS_LEVERAGE = "args_leverage"
        private const val ARGS_ENTRY_PRICE = "args_entry_price"
        private const val ARGS_MARK_PRICE = "args_mark_price"
        private const val ARGS_UNREALIZED_PNL = "args_unrealized_pnl"
        private const val ARGS_ROE = "args_roe"
        private const val ARGS_WALLET_NAME = "args_wallet_name"

        fun newInstance(
            position: PerpsPosition,
        ): PerpsCloseBottomSheetDialogFragment {
            return PerpsCloseBottomSheetDialogFragment().withArgs {
                putString(ARGS_POSITION_ID, position.positionId)
                putString(ARGS_SIDE, position.side)
                putString(ARGS_MARGIN, position.margin)
                putInt(ARGS_LEVERAGE, position.leverage)
                putString(ARGS_ENTRY_PRICE, position.entryPrice)
                putString(ARGS_MARK_PRICE, position.markPrice)
                putString(ARGS_UNREALIZED_PNL, position.unrealizedPnl)
                putString(ARGS_ROE, position.roe)
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
    private val bottomViewModel by viewModels<BottomSheetViewModel>()

    enum class Step {
        Pending,
        Sending,
        Done,
        Error,
    }

    private val positionId by lazy {
        requireNotNull(requireArguments().getString(ARGS_POSITION_ID)) { "positionId is null" }
    }

    private val margin by lazy {
        requireNotNull(requireArguments().getString(ARGS_MARGIN)) { "margin is null" }
    }
    private val markPrice by lazy {
        requireNotNull(requireArguments().getString(ARGS_MARK_PRICE)) { "markPrice is null" }
    }
    private val unrealizedPnl by lazy {
        requireNotNull(requireArguments().getString(ARGS_UNREALIZED_PNL)) { "unrealizedPnl is null" }
    }
    private val roe by lazy {
        requireNotNull(requireArguments().getString(ARGS_ROE)) { "roe is null" }
    }
    private var step by mutableStateOf(Step.Pending)
    private var errorInfo: String? by mutableStateOf(null)

    private var latestMarkPrice by mutableStateOf("")
    private var latestUnrealizedPnl by mutableStateOf("")
    private var latestRoe by mutableStateOf("")
    private var marketIconUrl by mutableStateOf("")
    private var marketSymbol by mutableStateOf("")
    private var settleAssetSymbol by mutableStateOf("USDT")
    private var settleAssetItem by mutableStateOf<TokenItem?>(null)
    private var sender by mutableStateOf<User?>(null)

    @Composable
    override fun ComposeContent() {
        val context = LocalContext.current
        val quoteColorReversed = context.defaultSharedPreferences
            .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
        val fiatRate = BigDecimal(Fiats.getRate())
        val fiatSymbol = Fiats.getSymbol()

        LaunchedEffect(Unit) {
            latestMarkPrice = markPrice
            latestUnrealizedPnl = unrealizedPnl
            latestRoe = roe
        }

        LaunchedEffect(positionId) {
            val localPosition = viewModel.getPositionFromDb(positionId)
            localPosition?.let { position ->
                latestMarkPrice = position.markPrice ?: latestMarkPrice
                latestUnrealizedPnl = position.unrealizedPnl ?: latestUnrealizedPnl
                latestRoe = position.roe ?: latestRoe
                marketIconUrl = position.iconUrl.orEmpty()
                marketSymbol = position.displaySymbol ?: position.tokenSymbol.orEmpty()
                refreshAssetAndSender(
                    settleAssetId = position.settleAssetId,
                    botId = position.botId
                )

                viewModel.getMarketFromDb(position.productId)?.let { market ->
                    marketIconUrl = market.iconUrl
                    marketSymbol = market.displaySymbol
                }
            }

            viewModel.loadPositionDetail(
                positionId = positionId,
                onSuccess = { position ->
                    latestMarkPrice = position.markPrice ?: "0"
                    latestUnrealizedPnl = position.unrealizedPnl ?: "0"
                    latestRoe = position.roe ?: "0"

                    lifecycleScope.launch {
                        viewModel.getMarketFromDb(position.productId)?.let { market ->
                            marketIconUrl = market.iconUrl
                            marketSymbol = market.displaySymbol
                        }
                    }

                    viewModel.loadMarketDetail(
                        marketId = position.productId,
                        onSuccess = { market ->
                            marketIconUrl = market.iconUrl
                            marketSymbol = market.displaySymbol
                        },
                        onError = {}
                    )
                    refreshAssetAndSender(
                        settleAssetId = position.settleAssetId,
                        botId = position.botId
                    )
                },
                onError = { error ->
                    Timber.e("Failed to load position detail: $error")
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
                            if (marketIconUrl.isNotEmpty()) {
                                CoilImage(
                                    model = marketIconUrl,
                                    placeholder = R.drawable.ic_avatar_place_holder,
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
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
                    }
                    Box(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(
                            id = when (step) {
                                Step.Pending -> R.string.Confirm_Close_Position
                                Step.Done -> R.string.Close_Position_Success
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
                            id = when (step) {
                                Step.Done -> R.string.swap_message_success
                                Step.Error -> R.string.Data_error
                                else -> R.string.swap_inner_desc
                            }
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

                    val pnl = try {
                        BigDecimal(latestUnrealizedPnl)
                    } catch (e: Exception) {
                        BigDecimal.ZERO
                    }
                    val risingColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
                    val fallingColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
                    val pnlColor = if (pnl >= BigDecimal.ZERO) {
                        risingColor
                    } else {
                        fallingColor
                    }

                    val estimatedReceive = try {
                        val margin = BigDecimal(margin)
                        val unrealizedPnl = BigDecimal(latestUnrealizedPnl)
                        margin + unrealizedPnl
                    } catch (e: Exception) {
                        BigDecimal.ZERO
                    }

                    val formattedRoe = try {
                        String.format("%f", latestRoe.toDouble())
                    } catch (e: Exception) {
                        latestRoe
                    }
                    val formattedPnlFiat = try {
                        val pnlFiat = pnl.multiply(fiatRate)
                        val sign = if (pnlFiat >= BigDecimal.ZERO) "+" else "-"
                        "$sign$fiatSymbol${pnlFiat.abs().priceFormat()}"
                    } catch (e: Exception) {
                        "${fiatSymbol}0"
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.Perpetual),
                            color = MixinAppTheme.colors.textRemarks,
                            fontSize = 14.sp,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CoilImage(
                                model = marketIconUrl,
                                placeholder = R.drawable.ic_avatar_place_holder,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = marketSymbol,
                                color = MixinAppTheme.colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(modifier = Modifier.height(20.dp))
                        settleAssetItem?.let { asset ->
                            Text(
                                text = stringResource(R.string.Estimated_Receive),
                                color = MixinAppTheme.colors.textRemarks,
                                fontSize = 14.sp,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CoilImage(
                                    model = asset.iconUrl,
                                    placeholder = R.drawable.ic_avatar_place_holder,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${String.format("%.8f", estimatedReceive)} ${asset.symbol}",
                                    color = MixinAppTheme.colors.textPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.W400
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = asset.chainName ?: "",
                                    color = MixinAppTheme.colors.textAssist,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            Text(
                                text = "${stringResource(R.string.Perpetual_PnL)}: ",
                                color = MixinAppTheme.colors.textAssist,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${if (pnl >= BigDecimal.ZERO) "+" else ""}${latestUnrealizedPnl} $settleAssetSymbol ($formattedRoe%)",
                                color = pnlColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Box(modifier = Modifier.height(20.dp))

                    ItemWalletContent(title = stringResource(id = R.string.Receiver).uppercase(), fontSize = 16.sp)
                    Box(modifier = Modifier.height(20.dp))

                    ItemUserContent(title = stringResource(id = R.string.Sender).uppercase(), sender, null)
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

    private fun closePosition() {
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
    }

    private fun refreshAssetAndSender(settleAssetId: String?, botId: String?) {
        lifecycleScope.launch {
            settleAssetId?.let { assetId ->
                val asset = bottomViewModel.findAssetItemById(assetId)
                asset?.let {
                    settleAssetSymbol = it.symbol
                    settleAssetItem = it
                }
            }

            botId?.let { userId ->
                sender = bottomViewModel.refreshUser(userId)
            }
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
