package one.mixin.android.ui.wallet.alert.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun AlertSelectItem(
    selected: Boolean,
    iconId: Int,
    titleId: Int,
    subTitleId: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Image(
            painter = painterResource(id = iconId),
            contentDescription = null,
            modifier = Modifier
                .padding(top = 5.dp)
                .size(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = titleId),
                style =
                TextStyle(
                    color = MixinAppTheme.colors.textMinor,
                    fontSize = 16.sp,
                ),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = subTitleId),
                style =
                TextStyle(
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 13.sp,
                ),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Image(
            painter = painterResource(id = R.drawable.ic_alert_selected),
            contentDescription = null,
            modifier = Modifier
                .padding(top = 5.dp)
                .alpha(if (selected) 1f else 0f)
                .size(24.dp),
        )
    }
}

