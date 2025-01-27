package one.mixin.android.web3.swap.Components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.priceFormat2
import one.mixin.android.ui.search.SearchViewModel
import java.math.BigDecimal

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecentTokens(key: String, callback: (SwapToken) -> Unit) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<SearchViewModel>()
    val recentToken by viewModel.recentSwapTokens.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.getRecentSwapTokens(context.defaultSharedPreferences, key)
    }
    if (recentToken.isEmpty()) return
    MixinAppTheme {
        Column(
            modifier = Modifier
                .background(MixinAppTheme.colors.background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.Recent),
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist
                )
                Icon(
                    modifier = Modifier.clickable {
                        viewModel.removeRecentSwapTokens(context.defaultSharedPreferences, key)
                    },
                    painter = painterResource(id = R.drawable.ic_action_delete),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.textAssist,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                overflow = FlowRowOverflow.Visible,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                recentToken.forEach {
                    RecentToken(it) {
                        callback.invoke(it)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun RecentToken(search: SwapToken, swapTokenClick: (SwapToken) -> Unit) {
    val context = LocalContext.current
    val quoteColorPref = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val itemWidth = ((screenWidthDp - 72) / 3).dp
    Row(
        modifier = Modifier
            .widthIn(max = itemWidth)
            .border(
                BorderStroke(1.dp, MixinAppTheme.colors.textPrimary.copy(alpha = 0.06f)), shape = RoundedCornerShape(32.dp)
            )
            .clip(RoundedCornerShape(21.dp))
            .clickable {
                swapTokenClick.invoke(search)
            }
            .padding(start = 6.dp, top = 5.dp, bottom = 5.dp, end = 10.dp), verticalAlignment = Alignment.CenterVertically
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
                    .offset(x = 0.dp, y = (19).dp)
                    .border(1.dp, MixinAppTheme.colors.background, CircleShape),
                placeholder = R.drawable.ic_avatar_place_holder
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(search.symbol, fontSize = 14.sp, lineHeight = 14.sp, color = MixinAppTheme.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (search.changeUsd != null) {
                val p = runCatching { BigDecimal(search.changeUsd).multiply(BigDecimal(100)) }.getOrDefault(BigDecimal.ZERO)
                Text(
                    "${if (p >= BigDecimal.ZERO) "+" else ""}${p.priceFormat2()}%", fontSize = 12.sp, lineHeight = 12.sp, color = if (p >= BigDecimal.ZERO) {
                        if (quoteColorPref) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
                    } else {
                        if (quoteColorPref) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
                    },
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(search.name, fontSize = 12.sp, lineHeight = 12.sp, color = MixinAppTheme.colors.textAssist, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}