package one.mixin.android.ui.wallet.alert.components
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.vo.CoinItem

@Composable
fun AssetFilter(coins: Set<CoinItem>?, open: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .background(MixinAppTheme.colors.background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                open.invoke()
            }
            .border(1.dp, shape = RoundedCornerShape(24.dp), color = MixinAppTheme.colors.borderColor)
    ) {
        if (coins.isNullOrEmpty()) {
            Row(
                modifier = Modifier.padding(vertical = 9.dp, horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.Assets), fontSize = 14.sp, color = MixinAppTheme.colors.textPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    modifier = Modifier.wrapContentSize(),
                    painter = painterResource(id = R.drawable.ic_sort_arrow_down),
                    contentDescription = null,
                    tint = Color.Unspecified,
                )
            }
        } else if (coins.size == 1) {
            Row(
                modifier = Modifier.padding(vertical = 9.dp, horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoilImage(
                    coins.first().iconUrl,
                    modifier =
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape),
                    placeholder = R.drawable.ic_avatar_place_holder,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(coins.first().symbol, fontSize = 14.sp, color = MixinAppTheme.colors.textPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    modifier = Modifier.wrapContentSize(),
                    painter = painterResource(id = R.drawable.ic_sort_arrow_down),
                    contentDescription = null,
                    tint = Color.Unspecified,
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(vertical = 9.dp, horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OverlappingLayout {
                    coins.forEachIndexed { _, item ->
                        CoilImage(
                            item.iconUrl,
                            modifier =
                            Modifier
                                .size(20.dp)
                                .clip(CircleShape),
                            placeholder = R.drawable.ic_avatar_place_holder,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.number_of_assets, coins.size), fontSize = 14.sp, color = MixinAppTheme.colors.textPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    modifier = Modifier.wrapContentSize(),
                    painter = painterResource(id = R.drawable.ic_sort_arrow_down),
                    contentDescription = null,
                    tint = Color.Unspecified,
                )
            }
        }
    }
}