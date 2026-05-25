package one.mixin.android.ui.home.web3.trade.perps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsOrder
import one.mixin.android.api.response.perps.PerpsOrderItem
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences

@Composable
fun OpenedOrderItem(
    order: PerpsOrderItem,
    onClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val quoteColorPref = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)

    val quantity = order.quantity
        .toBigDecimalOrNull()
        ?.abs()
        ?.stripTrailingZeros()
        ?.toPlainString()
        ?: order.quantity.removePrefix("-")

    val isLong = order.side.equals("long", ignoreCase = true)
    val sideColor = if (isLong) {
        if (quoteColorPref) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    } else {
        if (quoteColorPref) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    }
    val isIncrease = order.orderType == PerpsOrder.TYPE_INCREASE
    val isFailed = order.status == PerpsOrder.STATUS_REJECTED
    val leverageDimmed = isFailed || order.status == PerpsOrder.STATUS_PROCESSING
    val leverageColor = if (leverageDimmed) MixinAppTheme.colors.textAssist else sideColor
    val leverageBackgroundColor = leverageColor.copy(alpha = 0.1f)
    val title = when {
        isIncrease && isFailed ->
            stringResource(if (isLong) R.string.Add_Long_Failed else R.string.Add_Short_Failed)
        isIncrease ->
            stringResource(if (isLong) R.string.Added_Long else R.string.Added_Short)
        isFailed ->
            stringResource(if (isLong) R.string.Open_Long_Failed else R.string.Open_Short_Failed)
        else ->
            stringResource(if (isLong) R.string.Opened_Long else R.string.Opened_Short)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoilImage(
            model = order.iconUrl,
            placeholder = R.drawable.ic_avatar_place_holder,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = MixinAppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${order.leverage}x",
                    fontSize = 12.sp,
                    color = leverageColor,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(leverageBackgroundColor)
                        .padding(horizontal = 3.dp, vertical = 2.dp),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$quantity ${order.tokenSymbol ?: ""}",
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
