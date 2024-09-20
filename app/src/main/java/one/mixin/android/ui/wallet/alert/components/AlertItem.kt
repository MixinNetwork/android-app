package one.mixin.android.ui.wallet.alert.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.priceFormat
import one.mixin.android.ui.wallet.alert.vo.Alert
import java.math.BigDecimal

@Composable
fun AlertItem(alert: Alert) {
    ConstraintLayout(modifier = Modifier.fillMaxWidth()) {
        val (starIcon, endIcon, title, subtitle) = createRefs()
        Icon(
            modifier = Modifier
                .constrainAs(starIcon) {
                    top.linkTo(endIcon.top)
                    start.linkTo(parent.start)
                    bottom.linkTo(endIcon.bottom)
                }
                .wrapContentSize(),
            painter = painterResource(id = alert.type.getIconResId()),
            contentDescription = null,
            tint = Color.Unspecified,
        )
        Text(
            "${BigDecimal(alert.value).priceFormat()} USD", modifier = Modifier.constrainAs(title) {
                top.linkTo(endIcon.top)
                bottom.linkTo(endIcon.bottom)
                linkTo(starIcon.end, endIcon.start, startMargin = 10.dp, endMargin = 10.dp, bias = 0f)
            }, style = TextStyle(
                fontSize = 14.sp,
                lineHeight = 14.sp
            ), color = MixinAppTheme.colors.textPrimary
        )
        Icon(
            modifier = Modifier
                .constrainAs(endIcon) {
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                }
                .wrapContentSize(),
            painter = painterResource(id = R.drawable.ic_more_horiz_black_24dp),
            contentDescription = null,
            tint = Color.Unspecified,
        )

        Text(
            stringResource(alert.frequency.getStringResId()), modifier = Modifier.constrainAs(subtitle) {
                top.linkTo(title.bottom, margin = 4.dp)
                start.linkTo(title.start)
            }, fontSize = 12.sp, color = MixinAppTheme.colors.textAssist
        )
    }
}