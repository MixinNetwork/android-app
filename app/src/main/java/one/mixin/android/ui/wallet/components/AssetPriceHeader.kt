package one.mixin.android.ui.wallet.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
internal fun AssetPriceHeader(iconUrl: String, title: String, price: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CoilImage(
            model = iconUrl,
            placeholder = R.drawable.ic_avatar_place_holder,
            modifier = Modifier.size(42.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Column {
            Text(title, fontSize = 16.sp, color = MixinAppTheme.colors.textPrimary)
            Text(stringResource(R.string.Current_price, price), fontSize = 13.sp, color = MixinAppTheme.colors.textAssist)
        }
    }
}
