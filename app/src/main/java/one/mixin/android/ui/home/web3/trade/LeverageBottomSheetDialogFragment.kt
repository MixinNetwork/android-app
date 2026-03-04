package one.mixin.android.ui.home.web3.trade

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import kotlin.math.abs

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
    var tempLeverage by remember { mutableFloatStateOf(currentLeverage) }

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
            Text(
                text = "${tempLeverage.toInt()}x",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MixinAppTheme.colors.textPrimary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Slider(
                value = tempLeverage,
                onValueChange = { tempLeverage = it },
                valueRange = 1f..maxLeverage.toFloat(),
                steps = maxLeverage - 2,
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
                val steps = 5
                val stepValue = maxLeverage / (steps - 1)
                for (i in 0 until steps) {
                    val value = if (i == steps - 1) maxLeverage else (i * stepValue)
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
                    fontWeight = FontWeight.Bold,
                    color = MixinAppTheme.colors.textPrimary
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MixinAppTheme.colors.accent)
                    .clickable { onApply(tempLeverage) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.Apply),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ProfitLossInfo(
    amount: String,
    leverage: Float,
    isLong: Boolean
) {
    val amountValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    
    if (amountValue == BigDecimal.ZERO) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Text(
                text = stringResource(R.string.Price_Up_Profit, "1", "0.0", "$0.00"),
                fontSize = 13.sp,
                color = MixinAppTheme.colors.walletGreen
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.Price_Down_Loss, String.format("%.2f", 100.0 / leverage), "$0.00", ""),
                fontSize = 13.sp,
                color = MixinAppTheme.colors.walletRed
            )
        }
        return
    }

    val priceUpPercent = 1.0
    val profitPercent = priceUpPercent * leverage
    val profitAmount = amountValue * BigDecimal(profitPercent / 100)

    val liquidationPercent = 100.0 / leverage
    val lossAmount = amountValue

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = if (isLong) {
                stringResource(
                    R.string.Price_Up_Profit,
                    String.format("%.0f", abs(priceUpPercent)),
                    String.format("%.0f", profitPercent),
                    String.format("$%.2f", profitAmount)
                )
            } else {
                stringResource(
                    R.string.Price_Down_Profit,
                    String.format("%.0f", abs(priceUpPercent)),
                    String.format("%.0f", profitPercent),
                    String.format("$%.2f", profitAmount)
                )
            },
            fontSize = 13.sp,
            color = MixinAppTheme.colors.textAssist
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isLong) {
                stringResource(
                    R.string.Price_Down_Loss,
                    String.format("%.2f", liquidationPercent),
                    String.format("$%.2f", lossAmount),
                    ""
                )
            } else {
                stringResource(
                    R.string.Price_Up_Loss,
                    String.format("%.2f", liquidationPercent),
                    String.format("$%.2f", lossAmount),
                    ""
                )
            },
            fontSize = 13.sp,
            color = MixinAppTheme.colors.textAssist
        )
    }
}