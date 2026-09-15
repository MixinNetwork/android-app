package one.mixin.android.ui.home.web3.trade.perps

import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.screenHeight
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment

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
            PerpsAdjustContent(
                onDismiss = { dismiss() },
                onAddMargin = {
                    dismiss()
                    onAddMargin?.invoke()
                },
                onReduceMargin = {
                    dismiss()
                    onReduceMargin?.invoke()
                },
                onAddPosition = {
                    dismiss()
                    onAddPosition?.invoke()
                },
            )
        }
    }

    override fun showError(error: String) = Unit
}

@Composable
private fun PerpsAdjustContent(
    onDismiss: () -> Unit,
    onAddMargin: () -> Unit,
    onReduceMargin: () -> Unit,
    onAddPosition: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MixinAppTheme.colors.backgroundWindow)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 40.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 70.dp).padding(start = 20.dp, end = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.perps_adjust_title),
                fontSize = 18.sp,
                lineHeight = 21.sp,
                letterSpacing = (-0.4).sp,
                fontWeight = FontWeight.W600,
                color = MixinAppTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(R.drawable.ic_circle_close),
                    contentDescription = stringResource(R.string.close),
                    tint = Color.Unspecified,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(10.dp))
            SectionTitle(R.string.Margin)
            Spacer(Modifier.height(11.dp))
            AdjustAction(R.string.perps_add_margin, R.string.perps_add_margin_description, R.drawable.ic_perps_add_margin, onAddMargin)
            Spacer(Modifier.height(8.dp))
            AdjustAction(R.string.perps_reduce_margin, R.string.perps_reduce_margin_description, R.drawable.ic_perps_reduce_margin, onReduceMargin)
            Spacer(Modifier.height(17.dp))
            SectionTitle(R.string.perps_position)
            Spacer(Modifier.height(10.dp))
            AdjustAction(R.string.perps_add_to_position, R.string.perps_add_position_description, R.drawable.ic_perps_add_position, onAddPosition)
            Spacer(Modifier.height(30.dp))
            Column(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.perps_margin_or_position),
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                )
                listOf(R.string.perps_margin_tip, R.string.perps_position_tip, R.string.perps_low_margin_tip).forEach { tip ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", color = MixinAppTheme.colors.textAssist, fontSize = 14.sp, lineHeight = 18.2.sp)
                        Text(stringResource(tip), color = MixinAppTheme.colors.textAssist, fontSize = 14.sp, lineHeight = 18.2.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        fontSize = 14.sp,
        lineHeight = 17.sp,
        color = MixinAppTheme.colors.textMinor,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun AdjustAction(
    @StringRes title: Int,
    @StringRes description: Int,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MixinAppTheme.colors.background)
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 16.dp, top = 13.dp, bottom = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(icon), null, tint = Color.Unspecified, modifier = Modifier.padding(top = 5.dp).size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(title), color = MixinAppTheme.colors.textMinor, fontSize = 16.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(description), color = MixinAppTheme.colors.textAssist, fontSize = 13.sp, lineHeight = 16.sp)
        }
    }
}

@Preview(name = "Adjust menu", widthDp = 375, locale = "en")
@Preview(name = "调整菜单", widthDp = 375, locale = "zh")
@Composable
private fun PerpsAdjustPreview() {
    MixinAppTheme {
        PerpsAdjustContent(onDismiss = {}, onAddMargin = {}, onReduceMargin = {}, onAddPosition = {})
    }
}
