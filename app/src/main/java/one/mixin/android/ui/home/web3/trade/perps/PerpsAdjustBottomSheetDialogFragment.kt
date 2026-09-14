package one.mixin.android.ui.home.web3.trade.perps

import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.screenHeight
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.wallet.alert.components.cardBackground

@AndroidEntryPoint
class PerpsAdjustBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    companion object {
        const val TAG = "PerpsAdjustBottomSheetDialogFragment"
    }

    var onAddMargin: (() -> Unit)? = null
    var onReduceMargin: (() -> Unit)? = null
    var onAddPosition: (() -> Unit)? = null

    override fun getTheme() = R.style.AppTheme_Dialog

    override fun getBottomSheetHeight(view: View): Int =
        requireContext().screenHeight() - view.getSafeAreaInsetsTop()

    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MixinAppTheme.colors.background)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 40.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.perps_adjust_title),
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.W600,
                        color = MixinAppTheme.colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { dismiss() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_circle_close),
                            contentDescription = stringResource(R.string.close),
                            tint = Color.Unspecified,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                SectionTitle(R.string.Margin)
                AdjustAction(R.string.perps_add_margin, R.string.perps_add_margin_description, R.drawable.ic_perps_margin, true) {
                    dismiss()
                    onAddMargin?.invoke()
                }
                Spacer(Modifier.height(8.dp))
                AdjustAction(R.string.perps_reduce_margin, R.string.perps_reduce_margin_description, R.drawable.ic_perps_margin, false) {
                    dismiss()
                    onReduceMargin?.invoke()
                }
                Spacer(Modifier.height(20.dp))
                SectionTitle(R.string.perps_position)
                AdjustAction(R.string.perps_add_to_position, R.string.perps_add_position_description, R.drawable.ic_perps_position, true) {
                    dismiss()
                    onAddPosition?.invoke()
                }
                Spacer(Modifier.height(32.dp))
                Text(
                    text = stringResource(R.string.perps_margin_or_position),
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Spacer(Modifier.height(12.dp))
                listOf(R.string.perps_margin_tip, R.string.perps_position_tip, R.string.perps_low_margin_tip).forEach { tip ->
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", color = MixinAppTheme.colors.textAssist, fontSize = 14.sp)
                        Text(stringResource(tip), color = MixinAppTheme.colors.textAssist, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }
        }
    }

    override fun showError(error: String) = Unit
}

@Composable
private fun SectionTitle(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = MixinAppTheme.colors.textPrimary,
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
    )
}

@Composable
private fun AdjustAction(
    @StringRes title: Int,
    @StringRes description: Int,
    @DrawableRes icon: Int,
    increase: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(24.dp)) {
            Icon(painterResource(icon), null, tint = MixinAppTheme.colors.textRemarks, modifier = Modifier.size(24.dp))
            Icon(
                painterResource(if (increase) R.drawable.ic_perps_add else R.drawable.ic_perps_minus),
                null,
                tint = Color.Unspecified,
                modifier = Modifier.size(13.dp).align(Alignment.BottomEnd),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(title), color = MixinAppTheme.colors.textPrimary, fontSize = 16.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(description), color = MixinAppTheme.colors.textAssist, fontSize = 14.sp, lineHeight = 18.sp)
        }
    }
}
