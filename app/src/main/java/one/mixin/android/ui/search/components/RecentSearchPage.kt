package one.mixin.android.ui.search.components

import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.priceFormat2
import one.mixin.android.ui.search.SearchViewModel
import one.mixin.android.vo.Dapp
import one.mixin.android.vo.RecentSearch
import one.mixin.android.vo.RecentSearchType
import timber.log.Timber
import java.math.BigDecimal

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecentSearchPage(dappClick: (Dapp) -> Unit) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<SearchViewModel>()
    val recentSearch = viewModel.getRecentSearch(context.defaultSharedPreferences)
    val dapps = viewModel.getAllDapps()
    MixinAppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (!recentSearch.isNullOrEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.Recent_Searches),
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textAssist
                    )
                    Icon(
                        modifier = Modifier.clickable {

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
                        .padding(horizontal = 16.dp),
                    overflow = FlowRowOverflow.Visible,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    recentSearch.forEach {
                        RecentSearchComponent(it)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .background(color = MixinAppTheme.colors.backgroundWindow)
                )
            }
            if (dapps.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.Trending_Dapps),
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textAssist
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                dapps.forEach {
                    DappComponent(it, dappClick)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun RecentSearchComponent(search: RecentSearch) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<SearchViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val quoteColorPref = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    var priceChangePercentage24H by remember { mutableStateOf<String?>(null) }
    if (search.type == RecentSearchType.MARKET) {
        coroutineScope.launch {
            val marketItem = search.primaryKey?.let { viewModel.findMarketItemByCoinId(coinId = it) }
            Timber.e("market ${marketItem?.coinId} ${marketItem?.priceChange24h}")
            priceChangePercentage24H = marketItem?.priceChangePercentage24H
        }
    }
    Row(
        modifier = Modifier
            .border(
                BorderStroke(1.dp, Color(0x0f000000)),
                shape = RoundedCornerShape(32.dp)
            )
            .padding(start = 6.dp, top = 6.dp, bottom = 6.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        if (search.type == RecentSearchType.LINK) {
            Image(
                painter = painterResource(id = R.drawable.ic_link_place_holder),
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
        } else {
            CoilImage(
                model = search.iconUrl,
                modifier = Modifier.size(36.dp),
                placeholder = R.drawable.ic_avatar_place_holder
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(search.title ?: "", fontSize = 14.sp, lineHeight = 14.sp, color = MixinAppTheme.colors.textPrimary)

            if (search.type == RecentSearchType.MARKET) {
                if (priceChangePercentage24H != null) {
                    val p = BigDecimal(priceChangePercentage24H)
                    Text(
                        "${p.priceFormat2()}%", fontSize = 13.sp, lineHeight = 13.sp, color = if (p >= BigDecimal.ZERO) {
                            if (quoteColorPref) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
                        } else {
                            if (quoteColorPref) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
                        },
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text("", fontSize = 13.sp, lineHeight = 13.sp)
                }
            } else if (search.type == RecentSearchType.DAPP) {
                Text(
                    runCatching {
                        Uri.parse(search.subTitle).host
                    }.getOrNull() ?: search.subTitle ?: "", fontSize = 13.sp, lineHeight = 13.sp, color = MixinAppTheme.colors.textAssist,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    search.subTitle ?: "", fontSize = 13.sp, lineHeight = 13.sp, color = MixinAppTheme.colors.textAssist,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DappComponent(dapp: Dapp, dappClick: (Dapp) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { dappClick.invoke(dapp) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoilImage(
            model = dapp.iconUrl,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape),
            placeholder = R.drawable.ic_avatar_place_holder
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(dapp.name, fontSize = 16.sp, color = MixinAppTheme.colors.textPrimary)
            Text(
                runCatching {
                    Uri.parse(dapp.homeUrl).host
                }.getOrNull() ?: dapp.homeUrl, fontSize = 14.sp, color = MixinAppTheme.colors.textAssist,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}