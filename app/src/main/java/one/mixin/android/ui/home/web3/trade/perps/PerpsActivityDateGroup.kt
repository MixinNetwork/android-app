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
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

fun PerpsOrderItem.createdAtDateLabel(context: Context): String {
    return runCatching {
        val zone = ZoneId.systemDefault()
        val date = ZonedDateTime.parse(createdAt).withZoneSameInstant(zone)
        val pattern = context.getString(
            if (date.year == ZonedDateTime.now(zone).year) {
                R.string.date_format_month_day
            } else {
                R.string.date_format_date
            },
        )
        date.format(DateTimeFormatter.ofPattern(pattern).withZone(zone))
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
