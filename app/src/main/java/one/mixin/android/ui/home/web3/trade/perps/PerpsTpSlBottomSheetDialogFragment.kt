package one.mixin.android.ui.home.web3.trade.perps

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.composeDp
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.util.SystemUIManager
import one.mixin.android.widget.components.MixinButton
import java.math.BigDecimal
import java.math.RoundingMode

@AndroidEntryPoint
class PerpsTpSlBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {

    enum class Mode { TAKE_PROFIT, STOP_LOSS }

    companion object {
        const val TAG = "PerpsTpSlBottomSheetDialogFragment"
        private const val ARGS_MODE = "args_mode"
        private const val ARGS_PRICE = "args_price"
        private const val ARGS_CURRENT_PRICE = "args_current_price"
        private const val ARGS_IS_LONG = "args_is_long"
        private const val ARGS_MARKET_ICON_URL = "args_market_icon_url"
        private const val ARGS_MARKET_SYMBOL = "args_market_symbol"
        private const val ARGS_MARGIN_AMOUNT = "args_margin_amount"
        private const val ARGS_LEVERAGE = "args_leverage"
        private const val ARGS_ENTRY_PRICE = "args_entry_price"

        fun newInstance(
            mode: Mode,
            price: String?,
            currentPrice: String?,
            isLong: Boolean,
            marketIconUrl: String,
            marketSymbol: String,
            marginAmount: String,
            leverage: Int,
            entryPrice: String? = null,
        ): PerpsTpSlBottomSheetDialogFragment {
            return PerpsTpSlBottomSheetDialogFragment().withArgs {
                putString(ARGS_MODE, mode.name)
                putString(ARGS_PRICE, price)
                putString(ARGS_CURRENT_PRICE, currentPrice)
                putBoolean(ARGS_IS_LONG, isLong)
                putString(ARGS_MARKET_ICON_URL, marketIconUrl)
                putString(ARGS_MARKET_SYMBOL, marketSymbol)
                putString(ARGS_MARGIN_AMOUNT, marginAmount)
                putInt(ARGS_LEVERAGE, leverage)
                putString(ARGS_ENTRY_PRICE, entryPrice)
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

    private val mode by lazy {
        Mode.valueOf(requireNotNull(requireArguments().getString(ARGS_MODE)) { "mode is null" })
    }
    private val initialPrice by lazy { requireArguments().getString(ARGS_PRICE) }
    private val currentPrice by lazy {
        requireNotNull(requireArguments().getString(ARGS_CURRENT_PRICE)) { "currentPrice is null" }
    }
    private val isLong by lazy { requireArguments().getBoolean(ARGS_IS_LONG, true) }
    private val marketIconUrl by lazy { requireArguments().getString(ARGS_MARKET_ICON_URL).orEmpty() }
    private val marketSymbol by lazy { requireArguments().getString(ARGS_MARKET_SYMBOL).orEmpty() }
    private val marginAmount by lazy { requireArguments().getString(ARGS_MARGIN_AMOUNT) ?: "0" }
    private val leverage by lazy { requireArguments().getInt(ARGS_LEVERAGE, 1) }
    private val entryPrice by lazy { requireArguments().getString(ARGS_ENTRY_PRICE) }

    private var priceInput by mutableStateOf("")

    private var onApply: ((String?) -> Unit)? = null

    fun setOnApply(callback: (String?) -> Unit): PerpsTpSlBottomSheetDialogFragment {
        onApply = callback
        return this
    }

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop()
    }

    @Composable
    override fun ComposeContent() {
        val currentPriceValue = try {
            BigDecimal(currentPrice)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }

        val actualEntryPrice = try {
            entryPrice?.let { BigDecimal(it) }?.takeIf { it > BigDecimal.ZERO } ?: currentPriceValue
        } catch (e: Exception) {
            currentPriceValue
        }

        LaunchedEffect(Unit) {
            priceInput = initialPrice.orEmpty()
        }

        val inputPrice = try {
            priceInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
        } catch (e: Exception) {
            BigDecimal.ZERO
        }

        val margin = try {
            BigDecimal(marginAmount)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }

        // Calculate PnL
        val pnl = if (inputPrice > BigDecimal.ZERO && actualEntryPrice > BigDecimal.ZERO) {
            val priceDiff = if (isLong) {
                inputPrice.subtract(actualEntryPrice)
            } else {
                actualEntryPrice.subtract(inputPrice)
            }
            margin.multiply(priceDiff)
                .divide(actualEntryPrice, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal(leverage))
        } else {
            BigDecimal.ZERO
        }

        val pnlPercent = if (margin > BigDecimal.ZERO && actualEntryPrice > BigDecimal.ZERO) {
            val priceDiff = if (isLong) {
                inputPrice.subtract(actualEntryPrice)
            } else {
                actualEntryPrice.subtract(inputPrice)
            }
            priceDiff.divide(actualEntryPrice, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal(leverage))
                .multiply(BigDecimal(100))
        } else {
            BigDecimal.ZERO
        }

        // Calculate target price from profit %
        val orderValueUsdt = margin.multiply(BigDecimal(leverage))
        val tokenAmount = if (currentPriceValue > BigDecimal.ZERO) {
            orderValueUsdt.divide(currentPriceValue, 4, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val quoteColorReversed = false
        val risingColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
        val fallingColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
        val pnlColor = if (pnl >= BigDecimal.ZERO) risingColor else fallingColor

        MixinAppTheme {
            Column(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(topStart = 8.composeDp, topEnd = 8.composeDp))
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(MixinAppTheme.colors.background),
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .weight(weight = 1f, fill = true),
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Header: market icon + symbol + direction + leverage
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CoilImage(
                            model = marketIconUrl,
                            placeholder = R.drawable.ic_avatar_place_holder,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = marketSymbol,
                            color = MixinAppTheme.colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W600,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Direction chip
                        Text(
                            text = stringResource(if (isLong) R.string.Long else R.string.Short),
                            modifier = Modifier
                                .background(
                                    MixinAppTheme.colors.backgroundWindow,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            color = MixinAppTheme.colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.W500,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        // Leverage chip
                        Text(
                            text = "${leverage}x",
                            modifier = Modifier
                                .background(
                                    MixinAppTheme.colors.backgroundWindow,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            color = MixinAppTheme.colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.W500,
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Current price
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
                            .padding(16.dp),
                    ) {
                        Text(
                            text = currentPriceValue.priceFormat(),
                            color = MixinAppTheme.colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.perps_current_price),
                            color = MixinAppTheme.colors.textMinor,
                            fontSize = 14.sp,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
                            .padding(16.dp),
                    ) {
                        Text(
                            text = stringResource(
                                id = when (mode) {
                                    Mode.TAKE_PROFIT -> R.string.perps_tpsl_take_profit_when
                                    Mode.STOP_LOSS -> R.string.perps_tpsl_stop_loss_when
                                }
                            ),
                            color = MixinAppTheme.colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W600,
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Profit reaches % row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.perps_tpsl_profit_reaches_percent),
                                color = MixinAppTheme.colors.textMinor,
                                fontSize = 14.sp,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = if (inputPrice > BigDecimal.ZERO && actualEntryPrice > BigDecimal.ZERO) {
                                    "${formatPerpsDisplayDecimal(pnlPercent.abs())}%"
                                } else {
                                    ""
                                },
                                color = MixinAppTheme.colors.accent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.W500,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_action_edit),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MixinAppTheme.colors.textMinor,
                            )
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MixinAppTheme.colors.borderColor,
                            thickness = 0.5.dp,
                        )

                        // Price reaches row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.perps_tpsl_price_reaches),
                                color = MixinAppTheme.colors.textMinor,
                                fontSize = 14.sp,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            BasicTextField(
                                value = priceInput,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                        priceInput = newValue
                                    }
                                },
                                modifier = Modifier.width(120.dp),
                                textStyle = TextStyle(
                                    color = MixinAppTheme.colors.accent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.W500,
                                    textAlign = TextAlign.End,
                                ),
                                cursorBrush = SolidColor(MixinAppTheme.colors.accent),
                                singleLine = true,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_action_edit),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MixinAppTheme.colors.textMinor,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Summary card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
                            .padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                        ) {
                            // Left: Est. P&L and Est. ROE
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.perps_tpsl_est_pnl),
                                    color = MixinAppTheme.colors.textMinor,
                                    fontSize = 12.sp,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (inputPrice > BigDecimal.ZERO) {
                                        "${formatPerpsSignedDecimal(pnl)} USDT"
                                    } else {
                                        "--"
                                    },
                                    color = if (inputPrice > BigDecimal.ZERO) pnlColor else MixinAppTheme.colors.textMinor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.W600,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.perps_tpsl_est_roe),
                                    color = MixinAppTheme.colors.textMinor,
                                    fontSize = 12.sp,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (inputPrice > BigDecimal.ZERO) {
                                        "${formatPerpsSignedPercent(pnlPercent)}"
                                    } else {
                                        "--"
                                    },
                                    color = if (inputPrice > BigDecimal.ZERO) pnlColor else MixinAppTheme.colors.textMinor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.W600,
                                )
                                if (inputPrice > BigDecimal.ZERO && pnl > BigDecimal.ZERO) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_arrow_top_right),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MixinAppTheme.colors.walletGreen,
                                    )
                                }
                            }

                            // Right: Leverage + position size
                            Column(
                                horizontalAlignment = Alignment.End,
                            ) {
                                Text(
                                    text = stringResource(R.string.Leverage),
                                    color = MixinAppTheme.colors.textMinor,
                                    fontSize = 12.sp,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${leverage}x",
                                    color = MixinAppTheme.colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.W600,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.position_size),
                                    color = MixinAppTheme.colors.textMinor,
                                    fontSize = 12.sp,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${tokenAmount.stripTrailingZeros().toPlainString()} ${marketSymbol.substringBefore("-")}",
                                    color = MixinAppTheme.colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.W600,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Bottom buttons
                Row(
                    modifier = Modifier
                        .background(MixinAppTheme.colors.background)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Cancel button
                    MixinButton(
                        onClick = {
                            dismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(30.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        backgroundColor = MixinAppTheme.colors.backgroundWindow,
                    ) {
                        Text(
                            text = stringResource(R.string.Cancel),
                            fontSize = 16.sp,
                            color = MixinAppTheme.colors.textPrimary,
                        )
                    }
                    // Confirm button
                    MixinButton(
                        onClick = {
                            onApply?.invoke(priceInput.takeIf { it.isNotBlank() })
                            dismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(30.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.Confirm),
                            fontSize = 16.sp,
                            color = Color.White,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
    }

    override fun dismiss() {
        dismissAllowingStateLoss()
    }

    override fun showError(error: String) {
        // No-op for TP/SL bottom sheet
    }
}
