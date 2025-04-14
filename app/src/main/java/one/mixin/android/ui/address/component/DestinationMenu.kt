package one.mixin.android.ui.address.component

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.components.cardBackground

@Composable
fun DestinationMenu(
    icon: Int,
    title: Int,
    subTile: Int,
    onClick: () -> Unit = {},
    free: Boolean = false
) {
    DestinationMenu(icon, stringResource(title), stringResource(subTile), onClick, free = free)
}

@Composable
fun DestinationMenu(
    icon: Int,
    title: String,
    subTile: Int,
    onClick: () -> Unit = {},
    free: Boolean = false
) {
    DestinationMenu(icon, title, stringResource(subTile), onClick, free = free)
}

@Composable
fun DestinationMenu(
    icon: Int,
    title: Int,
    subTile: String,
    onClick: () -> Unit = {},
    free: Boolean = false,
    isPrivacy: Boolean = false,
) {
    DestinationMenu(icon, stringResource(title), subTile, onClick, free = free, isPrivacy = isPrivacy)
}

@Composable
fun DestinationMenu(
    icon: Int,
    title: String,
    subTile: String,
    onClick: () -> Unit = {},
    free: Boolean = false,
    isPrivacy: Boolean = false,
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .cardBackground(
                Color.Transparent, MixinAppTheme.colors.borderColor, cornerRadius = 8.dp
            )
            .padding(vertical = 13.dp, horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.padding(8.dp),
            painter = painterResource(icon),
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    fontSize = 16.sp,
                    lineHeight = 19.sp,
                    color = MixinAppTheme.colors.textPrimary
                )
                if (free) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.FREE),
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier
                            .background(
                                color = MixinAppTheme.colors.accent,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                } else if (isPrivacy) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(id = R.drawable.ic_wallet_privacy),
                        tint = Color.Unspecified,
                        contentDescription = null,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                subTile,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = MixinAppTheme.colors.textAssist
            )
        }
    }
}
