package one.mixin.android.ui.setting.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun MemberUpgradeTopBar(
    onClose: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(id = R.string.mixin_one),
            fontSize = 18.sp,
            fontWeight = FontWeight.W700,
            color = MixinAppTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onClose) {
            Icon(
                painter = painterResource(id = R.drawable.ic_circle_close),
                tint = Color.Unspecified,
                contentDescription = stringResource(id = R.string.close)
            )
        }
    }
}
