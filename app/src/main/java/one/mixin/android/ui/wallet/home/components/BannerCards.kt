package one.mixin.android.ui.wallet.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import one.mixin.android.R
import one.mixin.android.api.response.WalletHomeBanner
import one.mixin.android.api.response.WalletHomeBannerAction
import one.mixin.android.compose.CoilImageCompat
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.ui.wallet.home.WalletHomeCallbacks
import one.mixin.android.ui.wallet.home.WalletHomeState
import kotlin.math.absoluteValue

private const val BANNER_AUTO_SWITCH_DELAY_MILLIS = 3_000L

@Composable
internal fun BannerPager(
    state: WalletHomeState,
    callbacks: WalletHomeCallbacks,
) {
    BannerPager(
        banners = state.dynamicBanners,
        showAddWalletBanner = state.showAddWalletBanner,
        onAddWalletClick = callbacks::onAddWalletClicked,
        onAddWalletClose = callbacks::onBannerClosed,
        onBannerClick = callbacks::onDynamicBannerClicked,
        onActionClick = callbacks::onDynamicBannerActionClicked,
        onBannerClose = callbacks::onDynamicBannerClosed,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BannerPager(
    banners: List<WalletHomeBanner>,
    onBannerClick: (WalletHomeBanner) -> Unit,
    onActionClick: (WalletHomeBanner, WalletHomeBannerAction) -> Unit,
    onBannerClose: (WalletHomeBanner) -> Unit,
    showAddWalletBanner: Boolean = false,
    onAddWalletClick: () -> Unit = {},
    onAddWalletClose: () -> Unit = {},
    bordered: Boolean = true,
    horizontalPadding: Dp = 20.dp,
) {
    val pages = remember(showAddWalletBanner, banners) {
        buildList {
            addAll(banners.map(WalletHomeBannerPage::Dynamic))
            if (showAddWalletBanner) add(WalletHomeBannerPage.AddWallet)
        }
    }
    if (pages.isEmpty()) return
    val pagerState = rememberPagerState(initialPage = 0) { pages.size }
    val density = LocalDensity.current
    var bannerHeightPx by remember(pages) { mutableIntStateOf(0) }
    LaunchedEffect(pages) {
        if (pages.size <= 1) return@LaunchedEffect
        while (true) {
            if (pagerState.isScrollInProgress) {
                snapshotFlow { pagerState.isScrollInProgress }.first { !it }
            }
            val interrupted = withTimeoutOrNull(BANNER_AUTO_SWITCH_DELAY_MILLIS) {
                snapshotFlow { pagerState.isScrollInProgress }.first { it }
                true
            } ?: false
            if (!interrupted && !pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % pages.size)
            }
        }
    }
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = horizontalPadding),
        pageSpacing = 8.dp,
        beyondViewportPageCount = minOf(2, pages.lastIndex),
        verticalAlignment = Alignment.Top,
    ) { page ->
        val bannerPage = pages[page]
        val clickModifier = when (bannerPage) {
            WalletHomeBannerPage.AddWallet -> Modifier.clickable(onClick = onAddWalletClick)
            is WalletHomeBannerPage.Dynamic -> if (bannerPage.banner.actionUrl.isNullOrBlank()) {
                Modifier
            } else {
                Modifier.clickable { onBannerClick(bannerPage.banner) }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = with(density) { bannerHeightPx.toDp() })
                .graphicsLayer {
                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                    scaleY = 1f - 0.06f * pageOffset
                }
                .clip(RoundedCornerShape(8.dp))
                .cardBackground(
                    MixinAppTheme.colors.background,
                    if (bordered) MixinAppTheme.colors.borderPrimary else Color.Transparent,
                )
                .then(clickModifier),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { bannerHeightPx = maxOf(bannerHeightPx, it.height) }
                    .padding(horizontal = 16.dp),
            ) {
                when (bannerPage) {
                    WalletHomeBannerPage.AddWallet -> BannerCard(
                        iconRes = R.drawable.ic_wallet_home_add,
                        titleRes = R.string.wallet_home_add_wallet_banner_title,
                        descriptionRes = null,
                        ctaRes = R.string.add_wallet,
                        onClick = onAddWalletClick,
                    )
                    is WalletHomeBannerPage.Dynamic -> DynamicBannerCard(
                        banner = bannerPage.banner,
                        onActionClick = onActionClick,
                    )
                }
            }
            IconButton(
                onClick = {
                    when (bannerPage) {
                        WalletHomeBannerPage.AddWallet -> onAddWalletClose()
                        is WalletHomeBannerPage.Dynamic -> onBannerClose(bannerPage.banner)
                    }
                },
                modifier = Modifier.align(Alignment.TopEnd).size(40.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_wallet_close),
                    contentDescription = stringResource(R.string.Close),
                    tint = Color.Unspecified,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

@Composable
private fun BannerCard(
    iconRes: Int,
    titleRes: Int,
    descriptionRes: Int?,
    ctaRes: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, end = 22.dp, bottom = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(42.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(titleRes),
                modifier = Modifier.fillMaxWidth(),
                color = MixinAppTheme.colors.textMinor,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.W400,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (descriptionRes != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(descriptionRes),
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            BannerAction(text = stringResource(ctaRes), onClick = onClick)
        }
    }
}

@Composable
private fun DynamicBannerCard(
    banner: WalletHomeBanner,
    onActionClick: (WalletHomeBanner, WalletHomeBannerAction) -> Unit,
) {
    val actions = banner.visibleActions
    val description = banner.description.takeIf { it.isNotBlank() }
    val showDescription = actions.isEmpty() && description != null
    val titleOnly = !showDescription
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, end = 22.dp, bottom = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        val iconUrl = banner.iconUrl.takeIf { it.isNotBlank() }
        if (iconUrl != null) {
            CoilImageCompat(
                model = iconUrl,
                placeholder = R.drawable.ic_avatar_place_holder,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(42.dp),
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_avatar_place_holder),
                contentDescription = null,
                modifier = Modifier.size(42.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = banner.title.orEmpty(),
                modifier = Modifier.fillMaxWidth(),
                color = MixinAppTheme.colors.textMinor,
                fontSize = if (titleOnly) 14.sp else 16.sp,
                lineHeight = 20.sp,
                fontWeight = if (titleOnly) FontWeight.W400 else FontWeight.W500,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (showDescription) {
                description.let { description ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        color = MixinAppTheme.colors.textAssist,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.W400,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else if (actions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    actions.forEach { action ->
                        BannerAction(
                            text = action.label.orEmpty(),
                            onClick = { onActionClick(banner, action) },
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ReferralBannerCard(callbacks: WalletHomeCallbacks) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth()
            .height(244.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_wallet_reffal),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .align(Alignment.TopCenter),
        )
        Icon(
            painter = painterResource(R.drawable.ic_wallet_close),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .size(12.dp)
                .clickable { callbacks.onReferralClosed() },
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_wallet_home_referral),
                contentDescription = null,
                modifier = Modifier.size(70.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.wallet_home_referral_banner_title),
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.W500,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            val description = stringResource(R.string.wallet_home_referral_banner_desc)
            val highlight = "60%"
            Text(
                text = buildAnnotatedString {
                    val start = description.indexOf(highlight)
                    if (start >= 0) {
                        append(description.substring(0, start))
                        withStyle(SpanStyle(color = Color(0xFFAA71FA), fontWeight = FontWeight.W500)) {
                            append(highlight)
                        }
                        append(description.substring(start + highlight.length))
                    } else {
                        append(description)
                    }
                },
                color = MixinAppTheme.colors.textAssist,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            BannerAction(
                text = stringResource(R.string.Learn_More),
                primary = true,
                onClick = callbacks::onReferralClicked,
                modifier = Modifier
                    .width(123.dp)
                    .height(32.dp),
                cornerRadius = 42,
                horizontalPadding = 0,
                verticalPadding = 0,
                fontWeight = FontWeight.W400,
                lineHeight = 18,
            )
        }
    }
}

@Composable
private fun BannerAction(
    text: String,
    primary: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 24,
    horizontalPadding: Int = 12,
    verticalPadding: Int = 3,
    fontWeight: FontWeight = FontWeight.W500,
    lineHeight: Int? = null,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(if (primary) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundWindow)
            .then(if (onClick == null) Modifier else Modifier.clickable { onClick() })
            .padding(horizontal = horizontalPadding.dp, vertical = verticalPadding.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (primary) Color.White else MixinAppTheme.colors.accent,
            fontSize = 14.sp,
            fontWeight = fontWeight,
            lineHeight = lineHeight?.sp ?: 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

private sealed class WalletHomeBannerPage {
    data object AddWallet : WalletHomeBannerPage()
    data class Dynamic(val banner: WalletHomeBanner) : WalletHomeBannerPage()
}
