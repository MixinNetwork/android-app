package one.mixin.android.ui.home.web3.trade.perps

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsPosition
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.composeDp
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.components.ActionBottom
import one.mixin.android.ui.tip.wc.compose.ItemWalletContent
import one.mixin.android.ui.wallet.ItemUserContent
import one.mixin.android.ui.wallet.components.WalletLabel
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.components.MixinButton
import java.math.BigDecimal
import java.math.RoundingMode

@AndroidEntryPoint
class PerpsBatchCloseBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {

    companion object {
        const val TAG = "PerpsBatchCloseBottomSheetDialogFragment"
        private const val ARGS_POSITIONS = "args_positions"

        fun newInstance(positions: List<PerpsPositionItem>) =
            PerpsBatchCloseBottomSheetDialogFragment().withArgs {
                putParcelableArrayList(ARGS_POSITIONS, ArrayList(positions))
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
    private val positions by lazy {
        requireNotNull(
            requireArguments().getParcelableArrayListCompat(
                ARGS_POSITIONS,
                PerpsPositionItem::class.java,
            )
        ) { "positions are required" }
    }

    private var step by mutableStateOf(Step.Pending)
    private var remainingPositions by mutableStateOf<List<PerpsPositionItem>>(emptyList())
    private var errorInfo by mutableStateOf<String?>(null)
    private var settleAsset by mutableStateOf<TokenItem?>(null)
    private var receiver by mutableStateOf<User?>(null)

    enum class Step {
        Pending,
        Sending,
        Done,
        Error,
    }

    @Composable
    override fun ComposeContent() {
        LaunchedEffect(Unit) {
            remainingPositions = positions
            refreshPaymentDetails()
            AnalyticsTracker.trackPerpsClosePositionPreview()
        }

        val displayPositions = if (step == Step.Done) positions else remainingPositions
        val totalMargin = displayPositions.sumOf { it.margin.toBigDecimalOrZero() }
        val totalPnl = displayPositions.sumOf { it.unrealizedPnl.toBigDecimalOrZero() }
        val estimatedReceive = (totalMargin + totalPnl).max(BigDecimal.ZERO)
        val pnlPercent = calculatePnlPercent(totalPnl, totalMargin)
        val settleAssetSymbol = settleAsset?.symbol ?: "USDT"

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
                    Spacer(modifier = Modifier.height(50.dp))
                    BatchCloseStatusIcon(step, positions)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(
                            when (step) {
                                Step.Pending -> R.string.confirm_closing_position
                                Step.Sending -> R.string.Sending
                                Step.Done -> R.string.Positions_Closed
                                Step.Error -> R.string.Positions_Close_Failed
                            }
                        ),
                        style = TextStyle(
                            color = MixinAppTheme.colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.W600,
                        ),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = errorInfo ?: stringResource(
                            when (step) {
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
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .height(10.dp)
                            .fillMaxWidth()
                            .background(MixinAppTheme.colors.backgroundWindow),
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    if (displayPositions.isNotEmpty()) {
                        BatchClosePositions(displayPositions)
                        Spacer(modifier = Modifier.height(20.dp))
                        BatchCloseSummary(
                            asset = settleAsset,
                            assetSymbol = settleAssetSymbol,
                            estimatedReceive = estimatedReceive,
                            totalPnl = totalPnl,
                            pnlPercent = pnlPercent,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    ItemUserContent(
                        title = stringResource(R.string.Receiver).uppercase(),
                        user = receiver,
                        address = null,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    ItemWalletContent(title = stringResource(R.string.Sender).uppercase())
                    Spacer(modifier = Modifier.height(16.dp))
                }

                when (step) {
                    Step.Done -> {
                        Row(
                            modifier = Modifier
                                .background(MixinAppTheme.colors.background)
                                .padding(20.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            MixinButton(
                                onClick = { dismiss() },
                                shape = RoundedCornerShape(30.dp),
                                contentPadding = PaddingValues(horizontal = 35.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.Done),
                                    fontSize = 16.sp,
                                    color = Color.White,
                                )
                            }
                        }
                    }

                    Step.Error -> {
                        ActionBottom(
                            modifier = Modifier,
                            cancelTitle = stringResource(R.string.Cancel),
                            confirmTitle = stringResource(R.string.Retry),
                            cancelAction = {
                                AnalyticsTracker.trackPerpsClosePositionPreviewCancel()
                                dismiss()
                            },
                            confirmAction = {
                                AnalyticsTracker.trackPerpsClosePositionPreviewConfirm()
                                showVerifyPinThenClose()
                            },
                        )
                    }

                    Step.Pending -> {
                        ActionBottom(
                            modifier = Modifier,
                            cancelTitle = stringResource(R.string.Cancel),
                            confirmTitle = stringResource(R.string.Confirm),
                            cancelAction = {
                                AnalyticsTracker.trackPerpsClosePositionPreviewCancel()
                                dismiss()
                            },
                            confirmAction = {
                                AnalyticsTracker.trackPerpsClosePositionPreviewConfirm()
                                showVerifyPinThenClose()
                            },
                        )
                    }

                    Step.Sending -> Unit
                }
            }
        }
    }

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop()
    }

    override fun showError(error: String) {
        errorInfo = error
        step = Step.Error
    }

    private suspend fun refreshPaymentDetails() {
        val firstPosition = positions.firstOrNull() ?: return
        firstPosition.settleAssetId?.let { assetId ->
            settleAsset = bottomViewModel.findAssetItemById(assetId)
        }
        firstPosition.botId?.let { botId ->
            receiver = bottomViewModel.refreshUser(botId)
        }
    }

    private fun showVerifyPinThenClose() {
        VerifyBottomSheetDialogFragment.newInstance(
            title = getString(R.string.Verify_PIN),
        ).apply {
            disableToast = true
        }.setOnPinSuccess {
            closePositions()
        }.showNow(parentFragmentManager, VerifyBottomSheetDialogFragment.TAG)
    }

    private fun closePositions() {
        if (remainingPositions.isEmpty()) return

        errorInfo = null
        step = Step.Sending
        val attemptedPositions = remainingPositions
        viewModel.closePerpsOrders(attemptedPositions) { result ->
            val successfulCount = attemptedPositions.size - result.failedPositions.size
            if (successfulCount > 0) {
                AnalyticsTracker.trackPerpsClosePositionEnd()
                step = Step.Done
            } else {
                remainingPositions = result.failedPositions
                errorInfo = result.errors.firstOrNull()
                step = Step.Error
            }
        }
    }
}

@Composable
private fun BatchCloseStatusIcon(
    step: PerpsBatchCloseBottomSheetDialogFragment.Step,
    positions: List<PerpsPositionItem>,
) {
    when (step) {
        PerpsBatchCloseBottomSheetDialogFragment.Step.Sending -> {
            CircularProgressIndicator(
                modifier = Modifier.size(70.dp),
                color = MixinAppTheme.colors.accent,
            )
        }

        PerpsBatchCloseBottomSheetDialogFragment.Step.Error -> {
            Icon(
                modifier = Modifier.size(70.dp),
                painter = painterResource(R.drawable.ic_transfer_status_failed),
                contentDescription = null,
                tint = Color.Unspecified,
            )
        }

        PerpsBatchCloseBottomSheetDialogFragment.Step.Done -> {
            Icon(
                modifier = Modifier.size(70.dp),
                painter = painterResource(R.drawable.ic_transfer_status_success),
                contentDescription = null,
                tint = Color.Unspecified,
            )
        }

        PerpsBatchCloseBottomSheetDialogFragment.Step.Pending -> {
            BatchCloseStackedIcons(positions)
        }
    }
}

@Composable
private fun BatchCloseStackedIcons(positions: List<PerpsPositionItem>) {
    if (positions.isEmpty()) {
        Icon(
            modifier = Modifier.size(70.dp),
            painter = painterResource(R.drawable.ic_solana),
            contentDescription = null,
            tint = Color.Unspecified,
        )
        return
    }

    val displayedPositions = if (positions.size > 3) positions.take(2) else positions.take(3)
    val remainingCount = positions.size - displayedPositions.size
    val itemCount = displayedPositions.size + if (remainingCount > 0) 1 else 0
    val iconSize = 70.dp
    val iconOffset = 54.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(iconSize + iconOffset * (itemCount - 1))
                .height(iconSize),
        ) {
            displayedPositions.forEachIndexed { index, position ->
                CoilImage(
                    model = position.iconUrl,
                    modifier = Modifier
                        .size(iconSize)
                        .offset(x = iconOffset * index)
                        .zIndex(index.toFloat())
                        .border(2.dp, MixinAppTheme.colors.background, CircleShape)
                        .clip(CircleShape),
                    placeholder = R.drawable.ic_avatar_place_holder,
                )
            }

            if (remainingCount > 0) {
                Surface(
                    modifier = Modifier
                        .size(iconSize)
                        .offset(x = iconOffset * displayedPositions.size)
                        .zIndex(displayedPositions.size.toFloat())
                        .border(2.dp, MixinAppTheme.colors.background, CircleShape)
                        .clip(CircleShape),
                    color = MixinAppTheme.colors.backgroundWindow,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "+$remainingCount",
                            color = MixinAppTheme.colors.textAssist,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchClosePositions(positions: List<PerpsPositionItem>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(
            text = stringResource(R.string.Perpetual).uppercase(),
            color = MixinAppTheme.colors.textRemarks,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        positions.forEach { position ->
            BatchClosePositionItem(position)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun BatchClosePositionItem(position: PerpsPositionItem) {
    val context = LocalContext.current
    val quoteColorPref = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val isLong = position.side.equals("long", ignoreCase = true)
    val isPending = position.state.equals(PerpsPosition.STATE_OPENING, true) ||
        position.state.equals(PerpsPosition.STATE_ADDING, true)
    val sideColor = if (isLong) {
        if (quoteColorPref) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    } else {
        if (quoteColorPref) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    }
    val leverageTextColor = if (isPending) MixinAppTheme.colors.textAssist else sideColor
    val leverageBackgroundColor = if (isPending) {
        MixinAppTheme.colors.backgroundGrayLight
    } else {
        sideColor.copy(alpha = 0.1f)
    }
    val direction = stringResource(if (isLong) R.string.Long else R.string.Short)
    val symbol = position.tokenSymbol ?: stringResource(R.string.Unknown)

    Row(verticalAlignment = Alignment.CenterVertically) {
        CoilImage(
            model = position.iconUrl,
            placeholder = R.drawable.ic_avatar_place_holder,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = direction,
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.W500,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = symbol,
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.W500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${position.leverage}x",
            color = leverageTextColor,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(leverageBackgroundColor)
                .padding(horizontal = 3.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun BatchCloseSummary(
    asset: TokenItem?,
    assetSymbol: String,
    estimatedReceive: BigDecimal,
    totalPnl: BigDecimal,
    pnlPercent: BigDecimal,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(
            text = stringResource(R.string.Estimated_Receive).uppercase(),
            color = MixinAppTheme.colors.textRemarks,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            asset?.let {
                CoilImage(
                    model = it.iconUrl,
                    placeholder = R.drawable.ic_avatar_place_holder,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = "+${formatBatchAmount(estimatedReceive)} $assetSymbol",
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.W500,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = asset?.chainName.orEmpty(),
                color = MixinAppTheme.colors.textAssist,
                fontSize = 14.sp,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${stringResource(R.string.PnL)}: ${formatSignedBatchAmount(totalPnl)} $assetSymbol (${formatPerpsSignedPercent(pnlPercent)})",
            color = if (totalPnl < BigDecimal.ZERO) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen,
            fontSize = 14.sp,
        )
    }
}

private fun String?.toBigDecimalOrZero(): BigDecimal = this?.toBigDecimalOrNull() ?: BigDecimal.ZERO

private fun calculatePnlPercent(pnl: BigDecimal, margin: BigDecimal): BigDecimal {
    if (margin <= BigDecimal.ZERO) return BigDecimal.ZERO
    return pnl
        .divide(margin, 8, RoundingMode.HALF_UP)
        .multiply(BigDecimal(100))
}

private fun formatBatchAmount(value: BigDecimal): String =
    value.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

private fun formatSignedBatchAmount(value: BigDecimal): String =
    if (value > BigDecimal.ZERO) "+${formatBatchAmount(value)}" else formatBatchAmount(value)
