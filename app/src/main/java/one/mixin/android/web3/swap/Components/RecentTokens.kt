package one.mixin.android.web3.swap.Components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowOverflow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getList
import one.mixin.android.extension.priceFormat2
import one.mixin.android.ui.search.components.RecentSearchComponent
import one.mixin.android.vo.RecentSearch
import one.mixin.android.vo.RecentSearchType
import java.math.BigDecimal

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecentTokens(key: String) {
    val context = LocalContext.current
    val list = context.defaultSharedPreferences.getList(key, SwapToken::class.java)
    if (list.isEmpty()) return
    MixinAppTheme {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MixinAppTheme.colors.background)
                .padding(horizontal = 16.dp),
            overflow = FlowRowOverflow.Visible,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            list.forEach {
                RecentToken(it) {

                }
            }
        }
    }
}

@Composable
fun RecentToken(search: SwapToken, swapTokenClick: (SwapToken) -> Unit) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val itemWidth = ((screenWidthDp - 32) / 3).dp
    Row(
        modifier = Modifier
            .widthIn(max = itemWidth)
            .border(
                BorderStroke(1.dp, Color(0x0f000000)),
                shape = RoundedCornerShape(32.dp)
            )
            .clip(RoundedCornerShape(21.dp))
            .clickable {
                swapTokenClick.invoke(search)
            }
            .padding(start = 6.dp, top = 6.dp, bottom = 6.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            CoilImage(
                model = search.icon,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                placeholder = R.drawable.ic_avatar_place_holder
            )
            CoilImage(
                model = search.chain.icon,
                modifier = Modifier
                    .size(13.dp)
                    .border(1.dp, MixinAppTheme.colors.background, CircleShape)
                    .offset(x = (-7).dp, y = (-7).dp),
                placeholder = R.drawable.ic_avatar_place_holder
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(search.symbol, fontSize = 14.sp, lineHeight = 14.sp, color = MixinAppTheme.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(search.name, fontSize = 12.sp, lineHeight = 12.sp, color = MixinAppTheme.colors.textAssist)
        }
    }
}