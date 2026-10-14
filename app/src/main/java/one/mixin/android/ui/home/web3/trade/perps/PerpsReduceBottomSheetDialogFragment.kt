package one.mixin.android.ui.home.web3.trade.perps

import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.api.response.perps.PerpsPosition
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.api.response.perps.toPosition
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.widget.components.MixinButton
import java.math.BigDecimal
import java.math.RoundingMode

@AndroidEntryPoint
class PerpsReduceBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    companion object {
        const val TAG = "PerpsReduceBottomSheetDialogFragment"
        private const val ARGS_POSITION = "args_position"

        fun newInstance(position: PerpsPositionItem) = PerpsReduceBottomSheetDialogFragment().withArgs {
            putParcelable(ARGS_POSITION, position)
        }
    }

    private val viewModel by viewModels<PerpetualViewModel>()
    private val initialPosition by lazy {
        requireNotNull(requireArguments().getParcelableCompat(ARGS_POSITION, PerpsPositionItem::class.java))
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    override fun getBottomSheetHeight(view: View): Int = requireContext().screenHeight() - view.getSafeAreaInsetsTop()

    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    @Composable
    override fun ComposeContent() {
        val observedPosition by remember(initialPosition.positionId) {
            viewModel.observePosition(initialPosition.positionId)
        }.collectAsStateWithLifecycle(initialValue = initialPosition)
        val position = observedPosition ?: initialPosition
        var market by remember(position.marketId) { mutableStateOf<PerpsMarket?>(null) }
        var enteredInput by rememberSaveable { mutableStateOf("100") }
        var enteredAsPercentage by rememberSaveable { mutableStateOf(true) }
        var isPercentage by rememberSaveable { mutableStateOf(true) }
        val keyboard = LocalSoftwareKeyboardController.current
        LaunchedEffect(initialPosition.positionId) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    viewModel.refreshSinglePosition(initialPosition.positionId, initialPosition.walletId)
                    viewModel.loadMarketDetail(initialPosition.marketId, onSuccess = { market = it }, onError = {})
                    delay(10_000)
                }
            }
        }
        val current = position.quantity.toBigDecimalOrNull()?.abs() ?: BigDecimal.ZERO
        val price = position.markPrice?.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }
        val size = price?.multiply(current)
        val inputValue = enteredInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val quantity = perpsReductionQuantity(position.quantity, position.markPrice, enteredInput, enteredAsPercentage, market?.quantityScale ?: -1)
        val amount = if (enteredAsPercentage) size?.multiply(inputValue)?.movePointLeft(2) else inputValue
        val percentage = if (enteredAsPercentage) inputValue else size?.takeIf { it > BigDecimal.ZERO }?.let {
            inputValue.multiply(BigDecimal(100)).divide(it, 8, RoundingMode.DOWN)
        }
        val input = if (isPercentage == enteredAsPercentage) enteredInput else {
            (if (isPercentage) percentage else amount)?.stripTrailingZeros()?.toPlainString().orEmpty()
        }
        val minimum = market?.minOrderValue?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val error = when {
            observedPosition?.state != PerpsPosition.STATE_OPEN -> stringResource(R.string.perps_reduce_quantity_changed)
            (enteredAsPercentage && inputValue > BigDecimal(100)) || (!enteredAsPercentage && size != null && inputValue > size) ->
                stringResource(R.string.perps_reduce_invalid_quantity)
            market != null && inputValue > BigDecimal.ZERO && quantity == null -> stringResource(R.string.perps_reduce_invalid_quantity)
            quantity != null && !perpsReductionMeetsMinimum(quantity, current, price, minimum) ->
                stringResource(R.string.error_perps_position_size_too_small_value, "\$${minimum.stripTrailingZeros().toPlainString()}")
            else -> null
        }
        val canSubmit = quantity != null && error == null
        val previewQuantity = quantity?.takeIf { canSubmit }?.toPlainString().orEmpty()
        val pnl = perpsReductionValue(position.unrealizedPnl, previewQuantity, position.quantity)
        val margin = perpsReductionValue(position.margin, previewQuantity, position.quantity)
        val receive = perpsReductionValue(estimatedPerpsCloseReturn(position.margin, position.unrealizedPnl, position.estimatedCloseFee)?.toPlainString(), previewQuantity, position.quantity)
        val fee = perpsReductionValue(position.estimatedCloseFee, previewQuantity, position.quantity)

        MixinAppTheme {
            Column(Modifier.fillMaxSize().background(MixinAppTheme.colors.background).imePadding()) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CoilImage(position.iconUrl, modifier = Modifier.size(30.dp).clip(CircleShape), placeholder = R.drawable.ic_avatar_place_holder)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.perps_close_position_title, stringResource(if (position.side.equals("long", true)) R.string.Long else R.string.Short), position.tokenSymbol.orEmpty()),
                                color = MixinAppTheme.colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.W600,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                stringResource(R.string.auto_close_subtitle_after_open, formatPerpsPrice(position.entryPrice, position.priceScale), formatPerpsPrice(position.markPrice ?: position.entryPrice, position.priceScale)),
                                color = MixinAppTheme.colors.textAssist,
                                fontSize = 12.sp,
                            )
                        }
                        IconButton(onClick = { dismiss() }) {
                            Icon(painterResource(R.drawable.ic_circle_close), stringResource(R.string.close), tint = Color.Unspecified, modifier = Modifier.size(26.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    PerpsReductionInput(
                        input = input,
                        isPercentage = isPercentage,
                        amount = amount,
                        percentage = percentage,
                        margin = size,
                        errorText = error,
                        enabled = observedPosition?.state == PerpsPosition.STATE_OPEN,
                        onInputChanged = { enteredInput = it; enteredAsPercentage = isPercentage },
                        decimalPlaces = 8,
                        percentageLabels = true,
                        autoFocus = false,
                        onToggleMode = {
                            isPercentage = !isPercentage
                        },
                    )
                    Spacer(Modifier.height(18.dp))
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PerpsAddInfoRow(
                            title = stringResource(R.string.Estimated_Receive),
                            value = receive?.let(::formatPerpsRawUsdDecimal) ?: "--",
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            PerpsAddInfoRow(
                                title = stringResource(R.string.Margin),
                                value = margin?.let { if (it.signum() < 0) formatPerpsSignedRawUsdDecimal(it) else formatPerpsRawUsdDecimal(it) } ?: "--",
                            )
                            Spacer(Modifier.height(4.dp))
                            PerpsIncludedPnl(pnl)
                        }
                        PerpsAddInfoRow(
                            title = stringResource(R.string.Fee),
                            value = fee?.let(::formatPerpsRawUsdDecimal) ?: "--",
                            onTipClick = {
                                PerpetualGuideBottomSheetDialogFragment.newInstance(
                                    PerpetualGuideBottomSheetDialogFragment.TAB_TRADING_FEE,
                                ).show(parentFragmentManager, PerpetualGuideBottomSheetDialogFragment.TAG)
                            },
                        )
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MixinButton(
                        onClick = { dismiss() },
                        modifier = Modifier.weight(1f).height(48.dp),
                        backgroundColor = MixinAppTheme.colors.backgroundGrayLight,
                        shape = RoundedCornerShape(32.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        Text(stringResource(R.string.Cancel), color = MixinAppTheme.colors.textPrimary, fontSize = 16.sp)
                    }
                    MixinButton(
                        onClick = {
                            if (quantity != null && canSubmit && parentFragmentManager.findFragmentByTag(PerpsCloseBottomSheetDialogFragment.TAG) == null) {
                                keyboard?.hide()
                                PerpsCloseBottomSheetDialogFragment.newInstance(position.toPosition(), reduceQuantity = quantity.stripTrailingZeros().toPlainString())
                                    .show(parentFragmentManager, PerpsCloseBottomSheetDialogFragment.TAG)
                                dismiss()
                            }
                        },
                        enabled = canSubmit,
                        modifier = Modifier.weight(1f).height(48.dp),
                        backgroundColor = if (canSubmit) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGrayLight,
                        shape = RoundedCornerShape(32.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        Text(stringResource(R.string.Confirm), color = if (canSubmit) Color.White else MixinAppTheme.colors.textAssist, fontSize = 16.sp)
                    }
                }
            }
        }
    }

    override fun showError(error: String) = Unit
}
