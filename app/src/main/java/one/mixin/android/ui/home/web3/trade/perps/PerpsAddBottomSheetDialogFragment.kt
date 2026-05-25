package one.mixin.android.ui.home.web3.trade.perps

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.findFragmentActivityOrNull
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.putInt
import one.mixin.android.extension.putString
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.components.InputAction
import one.mixin.android.ui.home.web3.trade.InputContent
import one.mixin.android.ui.home.web3.trade.KeyboardAwareBox
import one.mixin.android.ui.home.web3.trade.SwapActivity
import one.mixin.android.ui.home.web3.trade.TradeFragment
import one.mixin.android.ui.home.web3.trade.perps.formatPerpsPrice
import one.mixin.android.ui.home.web3.trade.perps.formatPerpsQuantity
import one.mixin.android.ui.home.web3.trade.perps.formatPerpsUsdDecimal
import one.mixin.android.ui.wallet.AddFeeBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TokenListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.util.SystemUIManager
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.components.MixinButton
import java.math.BigDecimal
import java.math.RoundingMode

@AndroidEntryPoint
class PerpsAddBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    companion object {
        const val TAG = "PerpsAddBottomSheetDialogFragment"
        private const val ARGS_POSITION = "args_position"

        fun newInstance(position: PerpsPositionItem): PerpsAddBottomSheetDialogFragment {
            return PerpsAddBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_POSITION, position)
            }
        }
    }

    private val position: PerpsPositionItem by lazy {
        requireArguments().getParcelableCompat(ARGS_POSITION, PerpsPositionItem::class.java)!!
    }

    private var onAddAction: ((TokenItem, String) -> Unit)? = null

    fun setOnAdd(callback: (TokenItem, String) -> Unit): PerpsAddBottomSheetDialogFragment {
        onAddAction = callback
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
        val context = LocalContext.current
        val viewModel = hiltViewModel<PerpetualViewModel>()
        val acceptedPerpAssetIdsOrdered = remember { readAcceptedPerpAssetIds(context) }
        val acceptedPerpAssetIds = remember(acceptedPerpAssetIdsOrdered) { acceptedPerpAssetIdsOrdered.toSet() }
        var selectedToken by remember { mutableStateOf<TokenItem?>(null) }
        var market by remember { mutableStateOf<PerpsMarket?>(null) }

        LaunchedEffect(acceptedPerpAssetIds) {
            market = viewModel.getMarketFromDb(position.marketId)
            viewModel.loadUsdTokens { tokens ->
                val supportedTokens = if (acceptedPerpAssetIds.isEmpty()) {
                    tokens
                } else {
                    tokens.filter { it.assetId in acceptedPerpAssetIds }
                }
                val orderedSupportedTokens = if (acceptedPerpAssetIdsOrdered.isEmpty()) {
                    supportedTokens
                } else {
                    acceptedPerpAssetIdsOrdered.mapNotNull { assetId ->
                        supportedTokens.firstOrNull { it.assetId == assetId }
                    }
                }

                selectedToken = resolveCurrentToken(
                    selectedToken = selectedToken,
                    availableTokens = orderedSupportedTokens,
                )
            }
        }

        MixinAppTheme {
            PerpsAddContent(
                position = position,
                market = market,
                selectedToken = selectedToken,
                onTokenSelect = {
                    TokenListBottomSheetDialogFragment.newInstance(
                        fromType = TokenListBottomSheetDialogFragment.TYPE_FROM_PERP,
                        currentAssetId = selectedToken?.assetId,
                    ).setOnAssetClick { token ->
                        selectedToken = token
                    }.show(parentFragmentManager, TokenListBottomSheetDialogFragment.TAG)
                },
                onCancel = { dismiss() },
                onAdd = { token, amount ->
                    onAddAction?.let { action ->
                        action(token, amount)
                        dismiss()
                    }
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
private fun PerpsAddContent(
    position: PerpsPositionItem,
    market: PerpsMarket?,
    selectedToken: TokenItem?,
    onTokenSelect: () -> Unit,
    onCancel: () -> Unit,
    onAdd: (TokenItem, String) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var amount by remember(position.positionId) { mutableStateOf("") }
    val tokenBalance = selectedToken?.balance?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val amountValue = amount.toBigDecimalOrNull()
    val hasInputAmount = amountValue != null && amountValue > BigDecimal.ZERO

    val minimumMargin = market?.minAmount?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val maximumMargin = market?.maxAmount?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val belowMinimumMargin = amountValue != null && minimumMargin > BigDecimal.ZERO && amountValue < minimumMargin
    val aboveMaximumMargin = amountValue != null && maximumMargin > BigDecimal.ZERO && amountValue > maximumMargin

    val insufficientBalance = amountValue != null && amountValue > BigDecimal.ZERO && amountValue > tokenBalance
    val canAdd = selectedToken != null && hasInputAmount && !insufficientBalance && !belowMinimumMargin && !aboveMaximumMargin
    val marketSymbol = position.tokenSymbol ?: position.displaySymbol.orEmpty()
    val currentPrice = position.markPrice.orEmpty().ifBlank { position.entryPrice }

    val minimumMarginError = stringResource(
        R.string.perps_minimum_margin,
        minimumMargin.stripTrailingZeros().toPlainString(),
        selectedToken?.symbol.orEmpty(),
    )
    val maximumMarginError = stringResource(
        R.string.perps_maximum_margin,
        maximumMargin.stripTrailingZeros().toPlainString(),
        selectedToken?.symbol.orEmpty(),
    )
    val marginLimitError = when {
        belowMinimumMargin -> minimumMarginError
        aboveMaximumMargin -> maximumMarginError
        else -> null
    }

    val currentPriceText = formatPerpsPrice(currentPrice, position.priceScale)
    val entryPriceText = position.entryPrice
        .takeIf { it.isNotBlank() }
        ?.let { formatPerpsPrice(it, position.priceScale) }
        ?: "--"
    val subtitleRawText = stringResource(R.string.auto_close_subtitle_after_open, entryPriceText, currentPriceText)
    val subtitleLabelColor = MixinAppTheme.colors.textRemarks
    val subtitleValueColor = MixinAppTheme.colors.textAssist
    val subtitleText = remember(subtitleRawText, entryPriceText, currentPriceText, subtitleLabelColor, subtitleValueColor) {
        buildAnnotatedString {
            append(subtitleRawText)
            addStyle(
                style = SpanStyle(color = subtitleLabelColor),
                start = 0,
                end = subtitleRawText.length,
            )
            listOf(entryPriceText, currentPriceText).forEach { value ->
                val start = subtitleRawText.indexOf(value)
                if (start >= 0) {
                    addStyle(
                        style = SpanStyle(color = subtitleValueColor),
                        start = start,
                        end = start + value.length,
                    )
                }
            }
        }
    }
    val tokenNetworkName = selectedToken?.chainName
        ?.takeIf { it.isNotBlank() }
        ?: selectedToken?.chainSymbol
            ?.takeIf { it.isNotBlank() }
            ?: ""
    val navigationBottom = WindowInsets.navigationBars.getBottom(density)
    val priceTabBottomPadding = if (navigationBottom in 1..with(density) { 32.dp.roundToPx() }) 24.dp else 8.dp
    fun showPerpsGuide(tab: Int) {
        val activity = context.findFragmentActivityOrNull() ?: return
        PerpetualGuideBottomSheetDialogFragment.newInstance(tab)
            .show(activity.supportFragmentManager, PerpetualGuideBottomSheetDialogFragment.TAG)
    }

    KeyboardAwareBox(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = MixinAppTheme.colors.background,
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            ),
        content = { availableHeight ->
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoilImage(
                        model = position.iconUrl,
                        placeholder = R.drawable.ic_avatar_place_holder,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val sideText = stringResource(
                            if (position.side.equals("short", ignoreCase = true)) R.string.Short else R.string.Long
                        )
                        Text(
                            text = stringResource(R.string.add_position_title, sideText, marketSymbol),
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.Amount),
                            fontSize = 14.sp,
                            color = MixinAppTheme.colors.textPrimary,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (tokenNetworkName.isNotBlank()) {
                            Text(
                                text = tokenNetworkName,
                                fontSize = 12.sp,
                                color = MixinAppTheme.colors.textAssist,
                                textAlign = TextAlign.End,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    InputContent(
                        token = selectedToken?.toSwapToken(),
                        text = amount,
                        selectClick = onTokenSelect,
                        onInputChanged = { amount = it },
                        tokenIconSize = 25.dp,
                        autoFocus = true,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_web3_wallet),
                            contentDescription = null,
                            tint = MixinAppTheme.colors.textAssist,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedToken?.balance?.numberFormat8() ?: "0",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = MixinAppTheme.colors.textAssist,
                                textAlign = TextAlign.Start,
                            ),
                            modifier = Modifier.clickable {
                                amount = selectedToken?.balance ?: "0"
                            },
                        )
                        if (insufficientBalance || tokenBalance <= BigDecimal.ZERO) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.Add),
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = MixinAppTheme.colors.accent,
                                ),
                                modifier = Modifier.clickable {
                                    val activity = context.findFragmentActivityOrNull() ?: return@clickable
                                    val token = selectedToken
                                    if (token == null) {
                                        onTokenSelect()
                                        return@clickable
                                    }
                                    AddFeeBottomSheetDialogFragment.newInstance(token)
                                        .apply {
                                            onAction = { type, addToken ->
                                                if (type == AddFeeBottomSheetDialogFragment.ActionType.SWAP) {
                                                    val currentWalletId = Session.getAccountId() ?: ""
                                                    val preferenceKey = "${TradeFragment.PREF_TRADE_SELECTED_TAB_PREFIX}$currentWalletId"
                                                    context.defaultSharedPreferences.putInt(preferenceKey, 0)
                                                    SwapActivity.show(
                                                        context = activity,
                                                        input = Constants.AssetId.USDT_ASSET_ETH_ID,
                                                        output = addToken.assetId,
                                                        amount = null,
                                                        referral = null,
                                                    )
                                                } else if (type == AddFeeBottomSheetDialogFragment.ActionType.DEPOSIT) {
                                                    WalletActivity.showDeposit(activity, addToken)
                                                }
                                            }
                                        }
                                        .show(activity.supportFragmentManager, AddFeeBottomSheetDialogFragment.TAG)
                                },
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = selectedToken?.name?.takeIf { it.isNotBlank() } ?: "",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = MixinAppTheme.colors.textAssist,
                                textAlign = TextAlign.End,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    PerpsAddInfoRow(
                        title = stringResource(R.string.add_position_add_size),
                        value = formatAddSizeValue(
                            amount = amount,
                            leverage = position.leverage,
                            price = currentPrice,
                            tokenSymbol = position.tokenSymbol.orEmpty(),
                        ),
                        onTipClick = {
                            showPerpsGuide(PerpetualGuideBottomSheetDialogFragment.TAB_POSITION)
                        },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PerpsAddInfoRow(
                        title = stringResource(R.string.add_position_total_size),
                        value = formatTotalSizeValue(
                            currentQuantity = position.quantity,
                            amount = amount,
                            leverage = position.leverage,
                            price = currentPrice,
                            tokenSymbol = position.tokenSymbol.orEmpty(),
                        ),
                        onTipClick = {
                            showPerpsGuide(PerpetualGuideBottomSheetDialogFragment.TAB_POSITION)
                        },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PerpsAddInfoRow(
                        title = stringResource(R.string.Liquidation_Price),
                        value = calculateEstimatedLiquidationPrice(
                            position = position,
                            amount = amount,
                            currentPrice = currentPrice,
                        ),
                        onTipClick = {
                            showPerpsGuide(PerpetualGuideBottomSheetDialogFragment.TAB_LIQUIDATION)
                        },
                    )
                }
                }
                if (availableHeight == null) {
                    PerpsAddActionFooter(
                        errorText = if (insufficientBalance) {
                            "${selectedToken?.symbol ?: ""} ${stringResource(R.string.insufficient_balance)}"
                        } else {
                            marginLimitError
                        },
                        canAdd = canAdd,
                        onCancel = onCancel,
                        onAdd = {
                            val token = selectedToken ?: return@PerpsAddActionFooter
                            val normalizedAmount = amount.toBigDecimalOrNull()
                                ?.stripTrailingZeros()
                                ?.toPlainString()
                                ?: return@PerpsAddActionFooter
                            onAdd(token, normalizedAmount)
                        },
                    )
                }
            }
        },
        floating = {
            fun applyBalancePercent(percent: BigDecimal) {
                amount = if (tokenBalance > BigDecimal.ZERO) {
                    tokenBalance.multiply(percent).stripTrailingZeros().toPlainString()
                } else {
                    ""
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MixinAppTheme.colors.background),
            ) {
                PerpsAddActionFooter(
                    errorText = if (insufficientBalance) {
                        "${selectedToken?.symbol ?: ""} ${stringResource(R.string.insufficient_balance)}"
                    } else {
                        marginLimitError
                    },
                    canAdd = canAdd,
                    onCancel = onCancel,
                    onAdd = {
                        val token = selectedToken ?: return@PerpsAddActionFooter
                        val normalizedAmount = amount.toBigDecimalOrNull()
                            ?.stripTrailingZeros()
                            ?.toPlainString()
                            ?: return@PerpsAddActionFooter
                        onAdd(token, normalizedAmount)
                    },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MixinAppTheme.colors.backgroundWindow)
                        .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = priceTabBottomPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    InputAction("25%", showBorder = true) {
                        applyBalancePercent(BigDecimal("0.25"))
                    }
                    InputAction("50%", showBorder = true) {
                        applyBalancePercent(BigDecimal("0.5"))
                    }
                    InputAction("100%", showBorder = true) {
                        applyBalancePercent(BigDecimal.ONE)
                    }
                    InputAction(stringResource(R.string.Done), showBorder = false) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                }
            }
        },
    )
}

@Composable
private fun PerpsAddActionFooter(
    errorText: String?,
    canAdd: Boolean,
    onCancel: () -> Unit,
    onAdd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MixinAppTheme.colors.background),
    ) {
        if (errorText != null) {
            Text(
                text = errorText,
                fontSize = 14.sp,
                color = MixinAppTheme.colors.walletRed,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                textAlign = TextAlign.Center,
            )
        }
        BottomActions(
            modifier = Modifier.fillMaxWidth(),
            canAdd = canAdd,
            onCancel = onCancel,
            onAdd = onAdd,
        )
    }
}

@Composable
private fun BottomActions(
    modifier: Modifier = Modifier,
    canAdd: Boolean,
    onCancel: () -> Unit,
    onAdd: () -> Unit,
) {
    Row(
        modifier = modifier
            .background(MixinAppTheme.colors.background)
            .padding(horizontal = 16.dp)
            .padding(bottom = 20.dp, top = 12.dp),
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
            onClick = onAdd,
            enabled = canAdd,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(32.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            backgroundColor = if (canAdd) {
                MixinAppTheme.colors.walletGreen
            } else {
                MixinAppTheme.colors.backgroundGrayLight
            },
        ) {
            Text(
                text = stringResource(R.string.add_position),
                fontSize = 16.sp,
                color = if (canAdd) Color.White else MixinAppTheme.colors.textAssist,
            )
        }
    }
}

@Composable
private fun PerpsAddInfoRow(
    title: String,
    value: String,
    valueColor: Color = MixinAppTheme.colors.textAssist,
    onTipClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(
                enabled = onTipClick != null,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                onTipClick?.invoke()
            },
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist,
            )
            if (onTipClick != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_tip),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.textAssist,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Text(
            text = value,
            fontSize = 14.sp,
            color = valueColor,
            textAlign = TextAlign.End,
        )
    }
}

private fun readAcceptedPerpAssetIds(context: android.content.Context): List<String> {
    return context.defaultSharedPreferences
        .getString(Constants.Account.PREF_PERPS_ACCEPTED_ASSET_IDS_V2, null)
        .orEmpty()
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

private fun resolveCurrentToken(
    selectedToken: TokenItem?,
    availableTokens: List<TokenItem>,
): TokenItem? {
    if (selectedToken == null) {
        return availableTokens.firstOrNull()
    }

    return availableTokens.firstOrNull { it.assetId == selectedToken.assetId } ?: selectedToken
}

private fun calculateAddQuantity(
    amount: String,
    leverage: Int,
    price: String,
): BigDecimal {
    val amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val priceValue = price.toBigDecimalOrNull() ?: BigDecimal.ZERO
    if (priceValue <= BigDecimal.ZERO || leverage <= 0) {
        return BigDecimal.ZERO
    }
    return amountValue
        .multiply(BigDecimal(leverage))
        .divide(priceValue, 8, RoundingMode.HALF_UP)
}

private fun formatAddSizeValue(
    amount: String,
    leverage: Int,
    price: String,
    tokenSymbol: String,
): String {
    val amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val quantityText = formatPerpsQuantity(calculateAddQuantity(amount, leverage, price))
    val usdValue = formatPerpsUsdDecimal(amountValue.multiply(BigDecimal(leverage.coerceAtLeast(0))))
    return listOf(quantityText, tokenSymbol)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .let { "$it ($usdValue)" }
}

private fun formatTotalSizeValue(
    currentQuantity: String,
    amount: String,
    leverage: Int,
    price: String,
    tokenSymbol: String,
): String {
    val priceValue = price.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val current = currentQuantity.toBigDecimalOrNull()?.abs() ?: BigDecimal.ZERO
    val add = calculateAddQuantity(amount, leverage, price)
    val total = current.add(add)
    val usdValue = if (priceValue > BigDecimal.ZERO) {
        total.multiply(priceValue)
    } else {
        BigDecimal.ZERO
    }
    return listOf(formatPerpsQuantity(total), tokenSymbol)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .let { "$it (${formatPerpsUsdDecimal(usdValue)})" }
}

private fun calculateEstimatedLiquidationPrice(
    position: PerpsPositionItem,
    amount: String,
    currentPrice: String,
): String {
    val existingLiquidationPrice = position.liquidationPrice
        ?.takeIf { it.isNotBlank() }
        ?.let { formatPerpsPrice(it, position.priceScale) }
        ?: "--"
    val addMargin = amount.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return existingLiquidationPrice
    val currentQuantity = position.quantity.toBigDecimalOrNull()?.abs() ?: BigDecimal.ZERO
    val addQuantity = calculateAddQuantity(amount, position.leverage, currentPrice)
    val totalQuantity = currentQuantity.add(addQuantity)
    val entryPrice = position.entryPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val markPrice = currentPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
    if (totalQuantity <= BigDecimal.ZERO || position.leverage <= 0 || markPrice <= BigDecimal.ZERO) {
        return existingLiquidationPrice
    }

    val currentNotional = currentQuantity.multiply(entryPrice.takeIf { it > BigDecimal.ZERO } ?: markPrice)
    val addNotional = addMargin.multiply(BigDecimal(position.leverage))
    val averageEntry = currentNotional.add(addNotional).divide(totalQuantity, 8, RoundingMode.HALF_UP)
    val liquidationRatio = BigDecimal.ONE.divide(BigDecimal(position.leverage), 8, RoundingMode.HALF_UP)
    val estimatedPrice = if (position.side.equals("short", ignoreCase = true)) {
        averageEntry.multiply(BigDecimal.ONE.add(liquidationRatio))
    } else {
        averageEntry.multiply(BigDecimal.ONE.subtract(liquidationRatio))
    }
    return formatPerpsPrice(estimatedPrice, position.priceScale)
}
