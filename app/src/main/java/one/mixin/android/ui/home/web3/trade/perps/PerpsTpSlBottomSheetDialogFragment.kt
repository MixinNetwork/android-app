package one.mixin.android.ui.home.web3.trade.perps

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.findFragmentActivityOrNull
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.putLong
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.putString
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.widget.components.MixinButton
import one.mixin.android.util.SystemUIManager
import java.math.BigDecimal
import java.math.RoundingMode

private enum class InputType { PNL, PRICE }

private const val PREF_TPSL_INPUT_TYPE = "pref_perps_tpsl_input_type"

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
        private const val ARGS_MARKET_ID = "args_market_id"

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
            marketId: String? = null,
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
                putString(ARGS_MARKET_ID, marketId)
            }
        }
    }

    private val mode by lazy {
        Mode.valueOf(requireNotNull(requireArguments().getString(ARGS_MODE)) { "mode is null" })
    }
    private val initialPrice by lazy { requireArguments().getString(ARGS_PRICE).orEmpty() }
    private val currentPrice by lazy { requireArguments().getString(ARGS_CURRENT_PRICE).orEmpty() }
    private val isLong by lazy { requireArguments().getBoolean(ARGS_IS_LONG, true) }
    private val marketIconUrl by lazy { requireArguments().getString(ARGS_MARKET_ICON_URL).orEmpty() }
    private val marketSymbol by lazy { requireArguments().getString(ARGS_MARKET_SYMBOL).orEmpty() }
    private val marginAmount by lazy { requireArguments().getString(ARGS_MARGIN_AMOUNT).orEmpty() }
    private val leverage by lazy { requireArguments().getInt(ARGS_LEVERAGE, 1) }
    private val entryPrice by lazy { requireArguments().getString(ARGS_ENTRY_PRICE).orEmpty() }
    private val marketId by lazy { requireArguments().getString(ARGS_MARKET_ID).orEmpty() }

    private var onApply: ((String?) -> Unit)? = null

    fun setOnApply(callback: (String?) -> Unit): PerpsTpSlBottomSheetDialogFragment {
        onApply = callback
        return this
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
        dialog.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE,
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

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop()
    }

    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            PerpsTpSlContent(
                mode = mode,
                initialPrice = initialPrice,
                currentPrice = currentPrice,
                isLong = isLong,
                marketIconUrl = marketIconUrl,
                marketSymbol = marketSymbol,
                marginAmount = marginAmount,
                leverage = leverage,
                entryPrice = entryPrice,
                marketId = marketId,
                onCancel = { dismiss() },
                onApply = { value ->
                    onApply?.invoke(value)
                    dismiss()
                },
            )
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
    }

    override fun dismiss() {
        dismissAllowingStateLoss()
    }

    override fun showError(error: String) = Unit
}

@Composable
private fun PerpsTpSlContent(
    mode: PerpsTpSlBottomSheetDialogFragment.Mode,
    initialPrice: String,
    currentPrice: String,
    isLong: Boolean,
    marketIconUrl: String,
    marketSymbol: String,
    marginAmount: String,
    leverage: Int,
    entryPrice: String,
    marketId: String,
    onCancel: () -> Unit,
    onApply: (String?) -> Unit,
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<PerpetualViewModel>()
    val preferences = remember(context) { context.defaultSharedPreferences }
    var latestCurrentPrice by rememberSaveable(marketId, currentPrice) { mutableStateOf(currentPrice) }
    LaunchedEffect(marketId) {
        if (marketId.isBlank()) return@LaunchedEffect
        while (isActive) {
            viewModel.loadMarketDetail(
                marketId = marketId,
                onSuccess = { market ->
                    latestCurrentPrice = market.last.ifBlank { market.markPrice }.ifBlank { latestCurrentPrice }
                },
                onError = {},
            )
            delay(10_000)
        }
    }
    val currentPriceValue = remember(latestCurrentPrice) { latestCurrentPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO }
    val validationCurrentPrice = remember(latestCurrentPrice) {
        latestCurrentPrice.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ZERO
    }
    val entryPriceValue = remember(entryPrice) {
        entryPrice.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }
    }
    val percentBasePrice = remember(entryPriceValue, validationCurrentPrice) {
        entryPriceValue ?: validationCurrentPrice
    }
    val liquidationBasePrice = remember(entryPriceValue, validationCurrentPrice) {
        entryPriceValue
            ?: validationCurrentPrice.takeIf { it > BigDecimal.ZERO }
            ?: BigDecimal.ZERO
    }
    val hasEntryPrice = entryPriceValue != null
    val leverageValue = leverage.coerceAtLeast(1)
    val storedInputType = remember(preferences) {
        preferences.getString(PREF_TPSL_INPUT_TYPE, null)
            ?.let { stored -> InputType.values().firstOrNull { it.name == stored } }
    }
    val defaultInputType = remember(initialPrice, storedInputType) {
        storedInputType ?: if (initialPrice.isBlank()) InputType.PNL else InputType.PRICE
    }
    var inputType by rememberSaveable(initialPrice, mode.name) {
        mutableStateOf(defaultInputType)
    }
    var priceFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(textFieldValueAtEnd(initialPrice))
    }
    var percentFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            textFieldValueAtEnd(
                normalizePercentInput(
                    derivePercentMagnitudeInput(
                        priceInput = initialPrice,
                        percentBasePrice = percentBasePrice,
                        leverage = leverageValue,
                        isLong = isLong,
                        mode = mode,
                    )
                )
            )
        )
    }

    val priceInput = priceFieldValue.text
    val percentMagnitudeInput = percentFieldValue.text
    val isTakeProfit = mode == PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT
    val priceErrorText = validateTpSlPrice(
        rawValue = priceInput,
        currentPrice = validationCurrentPrice,
        liquidationBasePrice = liquidationBasePrice,
        leverage = leverageValue,
        isLong = isLong,
        isTakeProfit = isTakeProfit,
    )
    val percentErrorText = validateTpSlPercent(
        rawValue = percentMagnitudeInput,
        currentPrice = validationCurrentPrice,
        percentBasePrice = percentBasePrice,
        liquidationBasePrice = liquidationBasePrice,
        leverage = leverageValue,
        isLong = isLong,
        mode = mode,
    )
    val errorText = if (inputType == InputType.PNL) percentErrorText else priceErrorText
    val currentPriceText = "$PERPS_USD_SYMBOL${currentPriceValue.priceFormat()}"
    val entryPriceText = remember(entryPriceValue) {
        entryPriceValue?.let { "$PERPS_USD_SYMBOL${it.priceFormat()}" }
    }
    val entryLabel = stringResource(R.string.perps_tpsl_entry_label)
    val currentLabel = stringResource(R.string.perps_tpsl_current_label)
    val subtitleLabelColor = MixinAppTheme.colors.textRemarks
    val subtitleValueColor = MixinAppTheme.colors.textAssist
    val subtitleText = remember(
        entryLabel,
        currentLabel,
        entryPriceText,
        currentPriceText,
        subtitleLabelColor,
        subtitleValueColor,
    ) {
        buildAnnotatedString {
            val labelStyle = SpanStyle(color = subtitleLabelColor)
            val valueStyle = SpanStyle(color = subtitleValueColor)

            if (entryPriceText != null) {
                pushStyle(labelStyle)
                append(entryLabel)
                append(" ")
                pop()

                pushStyle(valueStyle)
                append(entryPriceText)
                pop()

                pushStyle(labelStyle)
                append("  ·  ")
                pop()
            }

            pushStyle(labelStyle)
            append(currentLabel)
            append(" ")
            pop()

            pushStyle(valueStyle)
            append(currentPriceText)
            pop()
        }
    }
    val activePercentText = percentMagnitudeInput.takeIf { it.isNotBlank() }
        ?.toBigDecimalOrNull()?.stripTrailingZeros()?.toPlainString()
    val filledValue = when (inputType) {
        InputType.PNL -> percentMagnitudeInput.isNotBlank()
        InputType.PRICE -> priceInput.isNotBlank()
    }
    val pageColor = MixinAppTheme.colors.background
    val surfaceColor = MixinAppTheme.colors.background
    val quickOptions = if (isTakeProfit) {
        listOf("10", "25", "50", "100")
    } else {
        listOf("5", "10", "25", "100")
    }
    var showInfoCard by rememberSaveable(mode.name) {
        mutableStateOf(System.currentTimeMillis() >= preferences.getLong(PREF_HIDE_TPSL_GUIDE_UNTIL, 0L))
    }

    LaunchedEffect(latestCurrentPrice, inputType, hasEntryPrice, percentMagnitudeInput, leverageValue, isLong, mode) {
        if (hasEntryPrice || inputType != InputType.PNL) return@LaunchedEffect
        val recalculatedPrice = percentToPriceInput(
            percentMagnitudeInput = percentMagnitudeInput,
            percentBasePrice = percentBasePrice,
            leverage = leverageValue,
            isLong = isLong,
            mode = mode,
        )
        if (priceFieldValue.text != recalculatedPrice) {
            priceFieldValue = textFieldValueAtEnd(recalculatedPrice)
        }
    }

    fun selectInputType(newType: InputType) {
        inputType = newType
        preferences.putString(PREF_TPSL_INPUT_TYPE, newType.name)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = pageColor,
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            ),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoilImage(
                    model = marketIconUrl,
                    placeholder = R.drawable.ic_avatar_place_holder,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (mode == PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT) {
                                R.string.Take_Profit
                            } else {
                                R.string.Stop_Loss
                            }
                        ),
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.W600,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitleText,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }
                Icon(
                    painter = painterResource(id = R.drawable.ic_circle_close),
                    contentDescription = stringResource(R.string.close),
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .size(26.dp)
                        .clickable(onClick = onCancel),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TpSlTypeChip(
                    modifier = Modifier.wrapContentSize(),
                    text = stringResource(R.string.PnL),
                    selected = inputType == InputType.PNL,
                    onClick = { selectInputType(InputType.PNL) },
                )
                TpSlTypeChip(
                    modifier = Modifier.wrapContentSize(),
                    text = stringResource(R.string.limit_price),
                    selected = inputType == InputType.PRICE,
                    onClick = { selectInputType(InputType.PRICE) },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .cardBackground(surfaceColor, MixinAppTheme.colors.borderColor)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            if (mode == PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT) {
                                R.string.Take_Profit_When
                            } else {
                                R.string.Stop_Loss_When
                            }
                        ),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (filledValue) {
                        Text(
                            text = stringResource(R.string.Clear),
                            fontSize = 14.sp,
                            color = MixinAppTheme.colors.accent,
                            modifier = Modifier.clickable {
                                percentFieldValue = textFieldValueAtEnd("")
                                priceFieldValue = textFieldValueAtEnd("")
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    TpSlInputField(
                        inputType = inputType,
                        mode = mode,
                        percentFieldValue = percentFieldValue,
                        priceFieldValue = priceFieldValue,
                        onPercentFieldValueChange = { fieldValue ->
                            val normalized = normalizePercentInput(fieldValue.text)
                            percentFieldValue = fieldValue.copy(
                                text = normalized,
                                selection = TextRange(normalized.length),
                            )
                            priceFieldValue = textFieldValueAtEnd(
                                percentToPriceInput(
                                    percentMagnitudeInput = normalized,
                                    percentBasePrice = percentBasePrice,
                                    leverage = leverageValue,
                                    isLong = isLong,
                                    mode = mode,
                                )
                            )
                        },
                        onPriceFieldValueChange = { fieldValue ->
                            val normalized = normalizeDecimalInput(fieldValue.text)
                            priceFieldValue = fieldValue.copy(
                                text = normalized,
                                selection = TextRange(normalized.length),
                            )
                            percentFieldValue = textFieldValueAtEnd(
                                normalizePercentInput(
                                    derivePercentMagnitudeInput(
                                        priceInput = normalized,
                                        percentBasePrice = percentBasePrice,
                                        leverage = leverageValue,
                                        isLong = isLong,
                                        mode = mode,
                                    )
                                )
                            )
                        },
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    quickOptions.forEach { option ->
                        TpSlQuickChip(
                            modifier = Modifier.wrapContentSize(),
                            text = "${if (mode == PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT) "+" else "-"}$option%",
                            selected = activePercentText == option,
                            onClick = {
                                percentFieldValue = textFieldValueAtEnd(option)
                                priceFieldValue = textFieldValueAtEnd(
                                    percentToPriceInput(
                                        percentMagnitudeInput = option,
                                        percentBasePrice = percentBasePrice,
                                        leverage = leverageValue,
                                        isLong = isLong,
                                        mode = mode,
                                    )
                                )
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val marginValue = marginAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val pnlPercent = percentMagnitudeInput.toBigDecimalOrNull()
                val quoteColorReversed = context.defaultSharedPreferences
                    .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
                val profitColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
                val lossColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed

                if (pnlPercent != null && pnlPercent > BigDecimal.ZERO && marginValue > BigDecimal.ZERO) {
                    val pnlAmount = marginValue.multiply(pnlPercent)
                        .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                    val roePercent = pnlPercent.stripTrailingZeros().toPlainString()
                    if (mode == PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Max Profit ",
                                fontSize = 13.sp,
                                color = MixinAppTheme.colors.textAssist,
                            )
                            Text(
                                text = "+$PERPS_USD_SYMBOL${pnlAmount.stripTrailingZeros().toPlainString()} (${roePercent}%)",
                                fontSize = 13.sp,
                                color = profitColor,
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Max Loss ",
                                fontSize = 13.sp,
                                color = MixinAppTheme.colors.textAssist,
                            )
                            Text(
                                text = "-$PERPS_USD_SYMBOL${pnlAmount.stripTrailingZeros().toPlainString()}",
                                fontSize = 13.sp,
                                color = lossColor,
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.auto_close_description),
                        fontSize = 13.sp,
                        color = MixinAppTheme.colors.textAssist,
                    )
                }
            }

            if (errorText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorText,
                    fontSize = 12.sp,
                    color = MixinAppTheme.colors.walletRed,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            if (showInfoCard) {
                Spacer(modifier = Modifier.height(12.dp))

                PerpsTpSlGuideCard(
                    guideType = if (isTakeProfit) {
                        TpSlGuideType.TAKE_PROFIT
                    } else {
                        TpSlGuideType.STOP_LOSS
                    },
                    onClose = {
                        showInfoCard = false
                        preferences.putLong(
                            PREF_HIDE_TPSL_GUIDE_UNTIL,
                            System.currentTimeMillis() + HIDE_TPSL_GUIDE_DURATION_MS,
                        )
                    },
                    actionText = stringResource(R.string.Learn_More),
                    onActionClick = {
                        context.findFragmentActivityOrNull()?.let { activity ->
                            PerpetualGuideBottomSheetDialogFragment.newInstance(
                                PerpetualGuideBottomSheetDialogFragment.TAB_TP_SL
                            ).show(activity.supportFragmentManager, PerpetualGuideBottomSheetDialogFragment.TAG)
                        }
                    },
                    layout = PerpsTpSlGuideCardLayout.BOTTOM_SHEET,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceColor)
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp, top = 12.dp)
                .imePadding(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MixinButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(32.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                backgroundColor = MixinAppTheme.colors.backgroundGrayLight,
            ) {
                Text(
                    text = stringResource(R.string.Cancel),
                    fontSize = 16.sp,
                    color = MixinAppTheme.colors.textPrimary,
                )
            }
            MixinButton(
                onClick = {
                    if (errorText == null) {
                        val value = when (inputType) {
                            InputType.PNL -> percentToPriceInput(
                                percentMagnitudeInput = percentMagnitudeInput.trim(),
                                percentBasePrice = percentBasePrice,
                                leverage = leverageValue,
                                isLong = isLong,
                                mode = mode,
                            )
                            InputType.PRICE -> priceInput.trim()
                        }.takeIf { it.isNotEmpty() }
                        onApply(value)
                    }
                },
                enabled = errorText == null,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(32.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                backgroundColor = if (errorText == null) {
                    MixinAppTheme.colors.accent
                } else {
                    MixinAppTheme.colors.backgroundGrayLight
                },
            ) {
                Text(
                    text = stringResource(R.string.Set),
                    fontSize = 16.sp,
                    color = if (errorText == null) Color.White else MixinAppTheme.colors.textAssist,
                )
            }
        }
    }
}

@Composable
private fun TpSlTypeChip(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) MixinAppTheme.colors.accent else MixinAppTheme.colors.borderColor,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .widthIn(min = 20.dp),
            textAlign = TextAlign.Center,
            text = text,
            fontSize = 12.sp,
            color = if (selected) MixinAppTheme.colors.accent else MixinAppTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun TpSlQuickChip(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) MixinAppTheme.colors.accent else MixinAppTheme.colors.borderColor,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .widthIn(min = 20.dp),
            textAlign = TextAlign.Center,
            text = text,
            fontSize = 12.sp,
            color = if (selected) MixinAppTheme.colors.accent else MixinAppTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TpSlInputField(
    inputType: InputType,
    mode: PerpsTpSlBottomSheetDialogFragment.Mode,
    percentFieldValue: TextFieldValue,
    priceFieldValue: TextFieldValue,
    onPercentFieldValueChange: (TextFieldValue) -> Unit,
    onPriceFieldValueChange: (TextFieldValue) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(inputType, mode) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    when (inputType) {
        InputType.PNL -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (percentFieldValue.text.isNotBlank()) {
                    Text(
                        text = if (mode == PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT) "+" else "-",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.W600,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                }
                BasicTextField(
                    value = percentFieldValue,
                    onValueChange = { newValue ->
                        val raw = newValue.text

                        val filtered = buildString {
                            var dotSeen = false
                            var intCount = 0
                            var decCount = 0
                            for (ch in raw) {
                                when {
                                    ch == '.' && !dotSeen -> {
                                        dotSeen = true
                                        append(ch)
                                    }
                                    ch.isDigit() && !dotSeen && intCount < 8 -> {
                                        intCount++
                                        append(ch)
                                    }
                                    ch.isDigit() && dotSeen && decCount < 2 -> {
                                        decCount++
                                        append(ch)
                                    }
                                }
                            }
                        }

                        val cursorPos = newValue.selection.end.coerceAtMost(filtered.length)
                        onPercentFieldValueChange(
                            newValue.copy(
                                text = filtered,
                                selection = TextRange(cursorPos),
                            )
                        )
                    },
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .widthIn(min = 1.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    cursorBrush = SolidColor(MixinAppTheme.colors.accent),
                    textStyle = TextStyle(
                        color = MixinAppTheme.colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.W600,
                    ),
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box {
                                if (percentFieldValue.text.isBlank()) {
                                    Text(
                                        text = stringResource(
                                            if (mode == PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT) {
                                                R.string.profit_reaches_percent
                                            } else {
                                                R.string.profit_reaches_percent
                                            }
                                        ),
                                        fontSize = 18.sp,
                                        color = MixinAppTheme.colors.textAssist,
                                        maxLines = 1,  
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.width(IntrinsicSize.Min),
                                ) {
                                    Box(modifier = Modifier.widthIn(min = 1.dp)) {
                                        innerTextField()
                                    }
                                    if (percentFieldValue.text.isNotBlank()) {
                                        Text(
                                            text = "%",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.W600,
                                            color = MixinAppTheme.colors.textPrimary,
                                        )
                                    }
                                }
                            }
                        }
                    },
                )
            }
        }

        InputType.PRICE -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (priceFieldValue.text.isNotBlank()) {
                    Text(
                        text = PERPS_USD_SYMBOL,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.W600,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                }
                BasicTextField(
                    value = priceFieldValue,
                    onValueChange = onPriceFieldValueChange,
                    modifier = Modifier.focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    cursorBrush = SolidColor(MixinAppTheme.colors.accent),
                    textStyle = TextStyle(
                        color = MixinAppTheme.colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.W600,
                    ),
                    decorationBox = { innerTextField ->
                        if (priceFieldValue.text.isBlank()) {
                            Text(
                                text = "${stringResource(R.string.price_reaches_dollar)} $PERPS_USD_SYMBOL",
                                fontSize = 18.sp,
                                color = MixinAppTheme.colors.textAssist,
                            )
                        }
                        innerTextField()
                    },
                )
            }
        }
    }
}

private fun textFieldValueAtEnd(text: String): TextFieldValue =
    TextFieldValue(
        text = text,
        selection = TextRange(text.length),
    )

private fun normalizeDecimalInput(value: String): String {
    val filtered = buildString {
        var hasDot = false
        value.forEach { char ->
            when {
                char.isDigit() -> append(char)
                char == '.' && !hasDot -> {
                    append(char)
                    hasDot = true
                }
            }
        }
    }
    return if (filtered == ".") "" else filtered
}

private fun normalizePercentInput(value: String): String {
    val normalized = normalizeDecimalInput(value)
    if (normalized.isBlank()) {
        return normalized
    }
    val dotIndex = normalized.indexOf('.')
    val limitedDecimals = if (dotIndex >= 0) {
        normalized.take(dotIndex + 3)
    } else {
        normalized
    }
    return limitedDecimals.take(8)
}

private fun percentToPriceInput(
    percentMagnitudeInput: String,
    percentBasePrice: BigDecimal,
    leverage: Int,
    isLong: Boolean,
    mode: PerpsTpSlBottomSheetDialogFragment.Mode,
): String {
    val magnitude = percentMagnitudeInput.toBigDecimalOrNull() ?: return ""
    if (percentBasePrice <= BigDecimal.ZERO || leverage <= 0) {
        return ""
    }
    val signedPercent = if (mode == PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT) {
        magnitude
    } else {
        magnitude.negate()
    }
    val marketDeltaPercent = if (isLong) {
        signedPercent.divide(BigDecimal(leverage), 8, RoundingMode.HALF_UP)
    } else {
        signedPercent.negate().divide(BigDecimal(leverage), 8, RoundingMode.HALF_UP)
    }
    val multiplier = BigDecimal.ONE + marketDeltaPercent.divide(BigDecimal(100), 8, RoundingMode.HALF_UP)
    if (multiplier <= BigDecimal.ZERO) {
        return ""
    }
    return percentBasePrice.multiply(multiplier).stripTrailingZeros().toPlainString()
}

private fun derivePercentMagnitudeInput(
    priceInput: String,
    percentBasePrice: BigDecimal,
    leverage: Int,
    isLong: Boolean,
    mode: PerpsTpSlBottomSheetDialogFragment.Mode,
): String {
    val signedPercent = signedPercentFromPrice(
        priceInput = priceInput,
        percentBasePrice = percentBasePrice,
        leverage = leverage,
        isLong = isLong,
        mode = mode,
    ) ?: return ""
    return signedPercent.abs().stripTrailingZeros().toPlainString()
}

private fun signedPercentFromPrice(
    priceInput: String,
    percentBasePrice: BigDecimal,
    leverage: Int,
    isLong: Boolean,
    mode: PerpsTpSlBottomSheetDialogFragment.Mode,
): BigDecimal? {
    val targetPrice = priceInput.toBigDecimalOrNull() ?: return null
    if (targetPrice <= BigDecimal.ZERO || percentBasePrice <= BigDecimal.ZERO || leverage <= 0) {
        return null
    }
    val marketDeltaPercent = targetPrice
        .subtract(percentBasePrice)
        .multiply(BigDecimal(100))
        .divide(percentBasePrice, 8, RoundingMode.HALF_UP)
    val signedPercent = if (isLong) {
        marketDeltaPercent.multiply(BigDecimal(leverage))
    } else {
        marketDeltaPercent.negate().multiply(BigDecimal(leverage))
    }
    return when {
        mode == PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT && signedPercent > BigDecimal.ZERO -> signedPercent
        mode == PerpsTpSlBottomSheetDialogFragment.Mode.STOP_LOSS && signedPercent < BigDecimal.ZERO -> signedPercent
        else -> null
    }
}

private fun validateTpSlPrice(
    rawValue: String,
    currentPrice: BigDecimal,
    liquidationBasePrice: BigDecimal,
    leverage: Int,
    isLong: Boolean,
    isTakeProfit: Boolean,
): String? {
    val trimmed = rawValue.trim()
    if (trimmed.isEmpty()) {
        return null
    }

    val price = trimmed.toBigDecimalOrNull() ?: return MixinApplicationHolder.getString(R.string.error_invalid_number)
    if (price <= BigDecimal.ZERO) {
        return MixinApplicationHolder.getString(R.string.error_invalid_number)
    }
    if (currentPrice <= BigDecimal.ZERO) {
        return null
    }
    if (leverage <= 0) {
        return null
    }

    val liquidationOffset = BigDecimal.ONE.divide(BigDecimal(leverage), 8, RoundingMode.HALF_UP)
    val liquidationPriceLong = liquidationBasePrice.multiply(BigDecimal.ONE.subtract(liquidationOffset))
    val liquidationPriceShort = liquidationBasePrice.multiply(BigDecimal.ONE.add(liquidationOffset))

    return when {
        isLong && isTakeProfit -> {
            if (price <= currentPrice) {
                MixinApplicationHolder.getString(
                    R.string.error_price_must_be_greater_than_value,
                    "$PERPS_USD_SYMBOL${currentPrice.stripTrailingZeros().toPlainString()}",
                )
            } else null
        }
        isLong && !isTakeProfit -> {
            when {
                price >= currentPrice -> MixinApplicationHolder.getString(
                    R.string.error_price_must_be_less_than_value,
                    "$PERPS_USD_SYMBOL${currentPrice.stripTrailingZeros().toPlainString()}",
                )
                price < liquidationPriceLong -> MixinApplicationHolder.getString(
                    R.string.error_price_must_be_greater_than_value,
                    "$PERPS_USD_SYMBOL${liquidationPriceLong.stripTrailingZeros().toPlainString()}",
                )
                else -> null
            }
        }
        !isLong && isTakeProfit -> {
            when {
                price >= currentPrice -> MixinApplicationHolder.getString(
                    R.string.error_price_must_be_less_than_value,
                    "$PERPS_USD_SYMBOL${currentPrice.stripTrailingZeros().toPlainString()}",
                )
                else -> null
            }
        }
        else -> {
            // !isLong && !isTakeProfit (short stop loss)
            when {
                price <= currentPrice -> MixinApplicationHolder.getString(
                    R.string.error_price_must_be_greater_than_value,
                    "$PERPS_USD_SYMBOL${currentPrice.stripTrailingZeros().toPlainString()}",
                )
                price > liquidationPriceShort -> MixinApplicationHolder.getString(
                    R.string.error_price_must_be_less_than_value,
                    "$PERPS_USD_SYMBOL${liquidationPriceShort.stripTrailingZeros().toPlainString()}",
                )
                else -> null
            }
        }
    }
}

private fun validateTpSlPercent(
    rawValue: String,
    currentPrice: BigDecimal,
    percentBasePrice: BigDecimal,
    liquidationBasePrice: BigDecimal,
    leverage: Int,
    isLong: Boolean,
    mode: PerpsTpSlBottomSheetDialogFragment.Mode,
): String? {
    val trimmed = rawValue.trim()
    if (trimmed.isEmpty()) {
        return null
    }

    val percent = trimmed.toBigDecimalOrNull() ?: return MixinApplicationHolder.getString(R.string.error_invalid_number)
    if (percent <= BigDecimal.ZERO) {
        return MixinApplicationHolder.getString(R.string.error_percentage_must_be_greater_than_value, "0%")
    }
    val derivedPrice = percentToPriceInput(
        percentMagnitudeInput = trimmed,
        percentBasePrice = percentBasePrice,
        leverage = leverage,
        isLong = isLong,
        mode = mode,
    )
    if (derivedPrice.isBlank()) {
        return MixinApplicationHolder.getString(R.string.error_invalid_number)
    }
    return validateTpSlPrice(
        rawValue = derivedPrice,
        currentPrice = currentPrice,
        liquidationBasePrice = liquidationBasePrice,
        leverage = leverage,
        isLong = isLong,
        isTakeProfit = mode == PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT,
    )
}

private object MixinApplicationHolder {
    fun getString(resId: Int, vararg formatArgs: Any): String =
        one.mixin.android.MixinApplication.appContext.getString(resId, *formatArgs)
}
