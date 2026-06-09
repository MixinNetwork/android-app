package one.mixin.android.ui.home.web3.trade.perps

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.putInt
import one.mixin.android.extension.screenHeight
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.util.SystemUIManager
import java.math.BigDecimal
import kotlin.math.roundToInt

@AndroidEntryPoint
class LeverageBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {

    companion object {
        const val TAG = "LeverageBottomSheetDialogFragment"
        private const val PREF_LEVERAGE = "pref_perps_leverage"

        fun newInstance(
            currentLeverage: Float,
            maxLeverage: Int,
            amount: String,
            isLong: Boolean
        ): LeverageBottomSheetDialogFragment {
            return LeverageBottomSheetDialogFragment().apply {
                this.currentLeverage = currentLeverage
                this.maxLeverage = maxLeverage
                this.amount = amount
                this.isLong = isLong
            }
        }
    }

    private var currentLeverage: Float = 10f
    private var maxLeverage: Int = 100
    private var amount: String = ""
    private var isLong: Boolean = true
    private var onLeverageSelected: ((Float) -> Unit)? = null

    fun setOnLeverageSelected(callback: (Float) -> Unit): LeverageBottomSheetDialogFragment {
        onLeverageSelected = callback
        return this
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            LeverageContent(
                currentLeverage = currentLeverage,
                maxLeverage = maxLeverage,
                amount = amount,
                isLong = isLong,
                onCancel = { dismiss() },
                onApply = { leverage ->
                    requireContext().defaultSharedPreferences.putInt(PREF_LEVERAGE, leverage.toInt())
                    onLeverageSelected?.invoke(leverage)
                    dismiss()
                }
            )
        }
    }

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop()
    }

    override fun showError(error: String) {
    }

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

    override fun dismiss() {
        dismissAllowingStateLoss()
    }
}

@Composable
private fun LeverageContent(
    currentLeverage: Float,
    maxLeverage: Int,
    amount: String,
    isLong: Boolean,
    onCancel: () -> Unit,
    onApply: (Float) -> Unit
) {
    val boundedMaxLeverage = maxLeverage.coerceAtLeast(1)
    var tempLeverage by remember(currentLeverage, boundedMaxLeverage) {
        mutableIntStateOf(currentLeverage.roundToInt().coerceIn(1, boundedMaxLeverage))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = stringResource(R.string.Leverage),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MixinAppTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LeverageStepperButton(
                    iconRes = R.drawable.ic_perps_minus,
                    enabled = tempLeverage > 1,
                    onClick = { tempLeverage = (tempLeverage - 1).coerceAtLeast(1) },
                )
                Spacer(modifier = Modifier.width(24.dp))
                Text(
                    text = "${tempLeverage}x",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MixinAppTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 112.dp),
                )
                Spacer(modifier = Modifier.width(24.dp))
                LeverageStepperButton(
                    iconRes = R.drawable.ic_perps_add,
                    enabled = tempLeverage < boundedMaxLeverage,
                    onClick = { tempLeverage = (tempLeverage + 1).coerceAtMost(boundedMaxLeverage) },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Slider(
                value = tempLeverage.toFloat(),
                onValueChange = { tempLeverage = it.roundToInt().coerceIn(1, boundedMaxLeverage) },
                valueRange = 1f..boundedMaxLeverage.toFloat(),
                steps = (boundedMaxLeverage - 2).coerceAtLeast(0),
                colors = SliderDefaults.colors(
                    thumbColor = MixinAppTheme.colors.accent,
                    activeTrackColor = MixinAppTheme.colors.accent,
                    inactiveTrackColor = MixinAppTheme.colors.backgroundWindow,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val labels = if (boundedMaxLeverage == 1) {
                    listOf(1)
                } else {
                    List(5) { index ->
                        when (index) {
                            0 -> 1
                            4 -> boundedMaxLeverage
                            else -> 1 + ((boundedMaxLeverage - 1) * index / 4)
                        }
                    }.distinct()
                }
                labels.forEach { value ->
                    Text(
                        text = "${value}x",
                        fontSize = 12.sp,
                        color = MixinAppTheme.colors.textAssist
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ProfitLossInfo(
            amount = amount,
            leverage = tempLeverage,
            isLong = isLong
        )

        Spacer(modifier = Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MixinAppTheme.colors.backgroundWindow)
                    .clickable { onCancel() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.Cancel),
                    fontSize = 16.sp,
                    color = MixinAppTheme.colors.textPrimary
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MixinAppTheme.colors.accent)
                    .clickable { onApply(tempLeverage.toFloat()) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.Apply),
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun LeverageStepperButton(
    iconRes: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MixinAppTheme.colors.backgroundWindow)
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ProfitLossInfo(
    amount: String,
    leverage: Int,
    isLong: Boolean
) {
    val context = LocalContext.current
    val amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val priceChangePercent = 1
    val profitPercent = leverage
    val liquidationPercent = 100.0 / leverage.toDouble()
    
    if (amountValue == BigDecimal.ZERO) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Text(
                text = context.formatPerpsProfitPreview(
                    isLong = isLong,
                    priceChangeText = priceChangePercent.toString(),
                    profitPercentText = profitPercent.toString(),
                    profitAmountText = formatPerpsRawUsdDecimal(BigDecimal.ZERO),
                ),
                fontSize = 13.sp,
                color = MixinAppTheme.colors.textAssist
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isLong) {
                    stringResource(R.string.Price_Down_Loss, String.format("%.2f", liquidationPercent), formatPerpsRawUsdDecimal(BigDecimal.ZERO), "")
                } else {
                    stringResource(R.string.Price_Up_Loss, String.format("%.2f", liquidationPercent), formatPerpsRawUsdDecimal(BigDecimal.ZERO), "")
                },
                fontSize = 13.sp,
                color = MixinAppTheme.colors.textAssist
            )
        }
        return
    }

    val profitAmount = amountValue.multiply(BigDecimal(profitPercent)).divide(BigDecimal(100))
    val lossAmount = amountValue

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = context.formatPerpsProfitPreview(
                isLong = isLong,
                priceChangeText = priceChangePercent.toString(),
                profitPercentText = profitPercent.toString(),
                profitAmountText = formatPerpsRawUsdDecimal(profitAmount),
            ),
            fontSize = 13.sp,
            color = MixinAppTheme.colors.textAssist
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isLong) {
                stringResource(
                    R.string.Price_Down_Loss,
                    String.format("%.2f", liquidationPercent),
                    formatPerpsRawUsdDecimal(lossAmount),
                    ""
                )
            } else {
                stringResource(
                    R.string.Price_Up_Loss,
                    String.format("%.2f", liquidationPercent),
                    formatPerpsRawUsdDecimal(lossAmount),
                    ""
                )
            },
            fontSize = 13.sp,
            color = MixinAppTheme.colors.textAssist
        )
    }
}
