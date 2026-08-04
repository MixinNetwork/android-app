package one.mixin.android.ui.wallet.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
internal fun SectionCard(
    title: String,
    showViewAll: Boolean,
    onClick: () -> Unit,
    contentUsesOwnPadding: Boolean = false,
    contentFlush: Boolean = false,
    showBottomSpacer: Boolean = true,
    headerTrailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 20.dp,
                bottom = if (contentFlush) 0.dp else 20.dp,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = MixinAppTheme.colors.textMinor,
            fontSize = 14.sp,
            fontWeight = FontWeight.W400,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = if (headerTrailing == null) Modifier else Modifier.weight(1f),
        )
        if (headerTrailing == null) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_gray_right),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(16.dp).offset(x = 4.dp),
            )
        } else {
            Spacer(modifier = Modifier.width(8.dp))
            headerTrailing()
        }
    }
    if (contentUsesOwnPadding) {
        content()
    } else {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            content()
        }
    }
    if (showViewAll) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(
                    top = if (contentFlush) 0.dp else 20.dp,
                    bottom = 20.dp,
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.view_all),
                color = MixinAppTheme.colors.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
            )
        }
    } else if (showBottomSpacer) {
        Spacer(modifier = Modifier.height(20.dp))
    }
}
