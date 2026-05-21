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
import one.mixin.android.api.response.perps.PerpsOrderItem
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

@Composable
fun OpenedOrderItem(
    order: PerpsOrderItem,
    onClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val quoteColorPref = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val dateFormat = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault())

    val displaySymbol = order.displaySymbol ?: order.tokenSymbol ?: "Unknown"
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
    val leverageBackgroundColor = sideColor.copy(alpha = 0.1f)
    val isIncreaseOrder = order.orderType == "increase_position"

    val openedTime = try {
        val instant = Instant.parse(order.createdAt)
        instant.atZone(ZoneId.systemDefault()).format(dateFormat)
    } catch (e: Exception) {
        ""
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
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val sideText = if (isLong) {
                        stringResource(R.string.Long)
                    } else {
                        stringResource(R.string.Short)
                    }
                    Text(
                        text = sideText,
                        fontSize = 16.sp,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = displaySymbol,
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
                        color = sideColor,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(leverageBackgroundColor)
                            .padding(horizontal = 3.dp, vertical = 2.dp),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isIncreaseOrder) stringResource(R.string.perps_added) else stringResource(R.string.perps_opened),
                    fontSize = 14.sp,
                    color = sideColor,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$quantity ${order.tokenSymbol ?: ""}",
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = openedTime,
                    fontSize = 12.sp,
                    color = MixinAppTheme.colors.textAssist,
                )
            }
        }
    }
}