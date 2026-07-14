package one.mixin.android.ui.home.web3.trade.perps

import android.content.Context
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsOrderItem
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.timeAgoDay

fun PerpsOrderItem.createdAtDateLabel(context: Context): String {
    return runCatching {
        createdAt.timeAgoDay(context.getString(R.string.date_format_date))
    }.getOrDefault(createdAt).ifBlank { createdAt }
}

@Composable
fun PerpsActivityDateHeader(
    date: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = date,
        modifier = modifier,
        fontSize = 14.sp,
        fontWeight = FontWeight.W400,
        color = MixinAppTheme.colors.textAssist,
    )
}
