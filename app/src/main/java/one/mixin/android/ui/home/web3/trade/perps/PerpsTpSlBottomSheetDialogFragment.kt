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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
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
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.putBoolean
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
private const val PREF_TPSL_INFO_TP_DISMISSED = "pref_perps_tpsl_info_tp_dismissed"
private const val PREF_TPSL_INFO_SL_DISMISSED = "pref_perps_tpsl_info_sl_dismissed"
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
    onCancel: () -> Unit,
    onApply: (String?) -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember(context) { context.defaultSharedPreferences }
    val currentPriceValue = remember(currentPrice) { currentPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO }
    val basePrice = remember(entryPrice) {
        entryPrice.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ZERO
    }
    val leverageValue = leverage.coerceAtLeast(1)
    val storedInputType = remember(preferences) {
        preferences.getString(PREF_TPSL_INPUT_TYPE, null)
            ?.let { stored -> InputType.values().firstOrNull { it.name == stored } }
    }
    val defaultInputType = remember(initialPrice, storedInputType) {
        storedInputType ?: if (initialPrice.isBlank()) InputType.PNL else InputType.PRICE
    }
    val infoDismissedPrefKey = remember(mode) {
        if (mode == PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT) {
            PREF_TPSL_INFO_TP_DISMISSED
        } else {
            PREF_TPSL_INFO_SL_DISMISSED
        }
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
                derivePercentMagnitudeInput(
                    priceInput = initialPrice,
                    basePrice = basePrice,
                    isLong = isLong,
                    mode = mode,
                )
            )
        )
    }

    val priceInput = priceFieldValue.text
    val signedPercent = signedPercentFromPrice(
        priceInput = priceInput,
        basePrice = basePrice,
        isLong = isLong,
        mode = mode,
    )
    val percentMagnitudeInput = percentFieldValue.text
    val isTakeProfit = mode == PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT
    val priceErrorText = validateTpSlPrice(
        rawValue = priceInput,
        basePrice = basePrice,
        leverage = leverageValue,
        isLong = isLong,
        isTakeProfit = isTakeProfit,
    )
    val percentErrorText = validateTpSlPercent(
        rawValue = percentMagnitudeInput,
        leverage = leverageValue,
        isLong = isLong,
        isTakeProfit = isTakeProfit,
    )
    val errorText = if (inputType == InputType.PNL) percentErrorText else priceErrorText
    val currentPriceText = "$PERPS_USD_SYMBOL${currentPriceValue.priceFormat()}"
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
        generateStopLossQuickOptions(leverageValue)
    }.filter { option ->
        validateTpSlPercent(
            rawValue = option,
            leverage = leverageValue,
            isLong = isLong,
            isTakeProfit = isTakeProfit,
        ) == null
    }
    var showInfoCard by rememberSaveable(mode.name) {
        mutableStateOf(!preferences.getBoolean(infoDismissedPrefKey, false))
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
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(12.dp))
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
                        fontWeight = FontWeight.W600,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$marketSymbol · ${stringResource(R.string.Current_price, currentPriceText)}",
                        fontSize = 12.sp,
                        color = MixinAppTheme.colors.textAssist,
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
                                R.string.perps_tpsl_take_profit_when
                            } else {
                                R.string.perps_tpsl_stop_loss_when
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
                            val normalized = normalizeDecimalInput(fieldValue.text)
                            percentFieldValue = fieldValue.copy(
                                text = normalized,
                                selection = TextRange(normalized.length),
                            )
                            priceFieldValue = textFieldValueAtEnd(
                                percentToPriceInput(
                                    percentMagnitudeInput = normalized,
                                    basePrice = basePrice,
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
                                derivePercentMagnitudeInput(
                                    priceInput = normalized,
                                    basePrice = basePrice,
                                    isLong = isLong,
                                    mode = mode,
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
                                        basePrice = basePrice,
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
                        .multiply(BigDecimal(leverageValue))
                        .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                    val roePercent = pnlPercent.multiply(BigDecimal(leverageValue))
                        .stripTrailingZeros().toPlainString()
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
                        text = stringResource(R.string.perps_tpsl_auto_close_hint),
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
                        preferences.putBoolean(infoDismissedPrefKey, true)
                    },
                    actionText = stringResource(R.string.Learn_More),
                    onActionClick = {
                        (context as? FragmentActivity)?.let { activity ->
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
                        onApply(priceInput.trim().takeIf { it.isNotEmpty() })
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
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = percentFieldValue,
                        onValueChange = onPercentFieldValueChange,
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .weight(1f, fill = false),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        cursorBrush = SolidColor(MixinAppTheme.colors.accent),
                        textStyle = TextStyle(
                            color = MixinAppTheme.colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.W600,
                        ),
                        decorationBox = { innerTextField ->
                            if (percentFieldValue.text.isBlank()) {
                                Text(
                                    text = stringResource(
                                        if (mode == PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT) {
                                            R.string.perps_tpsl_profit_reaches_percent
                                        } else {
                                            R.string.perps_tpsl_loss_reaches_percent
                                        }
                                    ),
                                    fontSize = 18.sp,
                                    color = MixinAppTheme.colors.textAssist,
                                )
                            }
                            innerTextField()
                        },
                    )
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
                                text = "${stringResource(R.string.perps_tpsl_price_reaches)} $PERPS_USD_SYMBOL",
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

private fun percentToPriceInput(
    percentMagnitudeInput: String,
    basePrice: BigDecimal,
    isLong: Boolean,
    mode: PerpsTpSlBottomSheetDialogFragment.Mode,
): String {
    val magnitude = percentMagnitudeInput.toBigDecimalOrNull() ?: return ""
    if (basePrice <= BigDecimal.ZERO) {
        return ""
    }
    val signedPercent = if (mode == PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT) {
        magnitude
    } else {
        magnitude.negate()
    }
    val marketDeltaPercent = if (isLong) signedPercent else signedPercent.negate()
    val multiplier = BigDecimal.ONE + marketDeltaPercent.divide(BigDecimal(100), 8, RoundingMode.HALF_UP)
    if (multiplier <= BigDecimal.ZERO) {
        return ""
    }
    return basePrice.multiply(multiplier).stripTrailingZeros().toPlainString()
}

private fun derivePercentMagnitudeInput(
    priceInput: String,
    basePrice: BigDecimal,
    isLong: Boolean,
    mode: PerpsTpSlBottomSheetDialogFragment.Mode,
): String {
    val signedPercent = signedPercentFromPrice(
        priceInput = priceInput,
        basePrice = basePrice,
        isLong = isLong,
        mode = mode,
    ) ?: return ""
    return signedPercent.abs().stripTrailingZeros().toPlainString()
}

private fun signedPercentFromPrice(
    priceInput: String,
    basePrice: BigDecimal,
    isLong: Boolean,
    mode: PerpsTpSlBottomSheetDialogFragment.Mode,
): BigDecimal? {
    val targetPrice = priceInput.toBigDecimalOrNull() ?: return null
    if (targetPrice <= BigDecimal.ZERO || basePrice <= BigDecimal.ZERO) {
        return null
    }
    val marketDeltaPercent = targetPrice
        .subtract(basePrice)
        .multiply(BigDecimal(100))
        .divide(basePrice, 8, RoundingMode.HALF_UP)
    val signedPercent = if (isLong) marketDeltaPercent else marketDeltaPercent.negate()
    return when {
        mode == PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT && signedPercent > BigDecimal.ZERO -> signedPercent
        mode == PerpsTpSlBottomSheetDialogFragment.Mode.STOP_LOSS && signedPercent < BigDecimal.ZERO -> signedPercent
        else -> null
    }
}

private fun validateTpSlPrice(
    rawValue: String,
    basePrice: BigDecimal,
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
    if (basePrice <= BigDecimal.ZERO) {
        return null
    }
    if (leverage <= 0) {
        return null
    }

    val liquidationOffset = BigDecimal.ONE.divide(BigDecimal(leverage), 8, RoundingMode.HALF_UP)
    val liquidationPriceLong = basePrice.multiply(BigDecimal.ONE.subtract(liquidationOffset))
    val liquidationPriceShort = basePrice.multiply(BigDecimal.ONE.add(liquidationOffset))

    return when {
        isLong && isTakeProfit -> {
            if (price <= basePrice) {
                MixinApplicationHolder.getString(
                    R.string.error_price_must_be_greater_than_value,
                    "$PERPS_USD_SYMBOL${basePrice.stripTrailingZeros().toPlainString()}",
                )
            } else null
        }
        isLong && !isTakeProfit -> {
            when {
                price >= basePrice -> MixinApplicationHolder.getString(
                    R.string.error_price_must_be_less_than_value,
                    "$PERPS_USD_SYMBOL${basePrice.stripTrailingZeros().toPlainString()}",
                )
                price <= liquidationPriceLong -> MixinApplicationHolder.getString(
                    R.string.error_price_must_be_greater_than_value,
                    "$PERPS_USD_SYMBOL${liquidationPriceLong.stripTrailingZeros().toPlainString()}",
                )
                else -> null
            }
        }
        !isLong && isTakeProfit -> {
            when {
                price >= basePrice -> MixinApplicationHolder.getString(
                    R.string.error_price_must_be_less_than_value,
                    "$PERPS_USD_SYMBOL${basePrice.stripTrailingZeros().toPlainString()}",
                )
                else -> null
            }
        }
        else -> {
            // !isLong && !isTakeProfit (short stop loss)
            when {
                price <= basePrice -> MixinApplicationHolder.getString(
                    R.string.error_price_must_be_greater_than_value,
                    "$PERPS_USD_SYMBOL${basePrice.stripTrailingZeros().toPlainString()}",
                )
                price >= liquidationPriceShort -> MixinApplicationHolder.getString(
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
    leverage: Int,
    isLong: Boolean,
    isTakeProfit: Boolean,
): String? {
    val trimmed = rawValue.trim()
    if (trimmed.isEmpty()) {
        return null
    }

    val percent = trimmed.toBigDecimalOrNull() ?: return MixinApplicationHolder.getString(R.string.error_invalid_number)
    if (percent <= BigDecimal.ZERO) {
        return MixinApplicationHolder.getString(R.string.error_percentage_must_be_greater_than_value, "0%")
    }

    val maxPercentExclusive = when {
        isTakeProfit && isLong -> null
        isTakeProfit && !isLong -> BigDecimal(100)
        leverage <= 0 -> null
        else -> BigDecimal(100).divide(BigDecimal(leverage), 8, RoundingMode.HALF_UP)
    } ?: return null

    return if (percent >= maxPercentExclusive) {
        MixinApplicationHolder.getString(
            R.string.error_percentage_must_be_less_than_value,
            "${maxPercentExclusive.stripTrailingZeros().toPlainString()}%",
        )
    } else {
        null
    }
}

private fun generateStopLossQuickOptions(leverage: Int): List<String> {
    val maxPercent = BigDecimal(100).divide(BigDecimal(leverage), 4, RoundingMode.HALF_UP)
    return (1..4).map { i ->
        val value = maxPercent.multiply(BigDecimal(i * 20))
            .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
        value
    }
}

private object MixinApplicationHolder {
    fun getString(resId: Int, vararg formatArgs: Any): String =
        one.mixin.android.MixinApplication.appContext.getString(resId, *formatArgs)
}
