package one.mixin.android.ui.wallet.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.ui.wallet.home.WalletHomeCallbacks
import one.mixin.android.ui.wallet.home.WalletHomeState

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BannerPager(
    state: WalletHomeState,
    callbacks: WalletHomeCallbacks,
) {
    val pages = remember(state.showAddWalletBanner) {
        buildList {
            if (state.showAddWalletBanner) add(WalletHomeBannerPage.ADD_WALLET)
        }
    }
    if (pages.isEmpty()) return
    val pagerState = rememberPagerState(initialPage = 0) { pages.size }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .wrapContentHeight()
                .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                ) { page ->
                    when (pages[page]) {
                        WalletHomeBannerPage.ADD_WALLET -> BannerCard(
                            iconRes = R.drawable.ic_wallet_home_add,
                            titleRes = R.string.wallet_home_add_wallet_banner_title,
                            descriptionRes = null,
                            ctaRes = R.string.add_wallet,
                            onClick = callbacks::onAddWalletClicked,
                        )
                    }
                }
            }
            Icon(
                painter = painterResource(R.drawable.ic_wallet_close),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .size(12.dp)
                    .clickable {
                        when (pages.getOrNull(pagerState.currentPage)) {
                            WalletHomeBannerPage.ADD_WALLET -> callbacks.onBannerClosed()
                            null -> Unit
                        }
                    },
            )
        }
        if (pages.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pages.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index) {
                                    MixinAppTheme.colors.accent
                                } else {
                                    MixinAppTheme.colors.borderColor
                                },
                            ),
                    )
                }
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
            .padding(end = 22.dp),
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
                color = MixinAppTheme.colors.textMinor,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.W400,
            )
            if (descriptionRes != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(descriptionRes),
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 12.sp,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            BannerAction(textRes = ctaRes, onClick = onClick)
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
                .padding(horizontal = 16.dp)
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
                textRes = R.string.Learn_More,
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
    textRes: Int,
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
            text = stringResource(textRes),
            color = if (primary) Color.White else MixinAppTheme.colors.accent,
            fontSize = 14.sp,
            fontWeight = fontWeight,
            lineHeight = lineHeight?.sp ?: 18.sp,
            textAlign = TextAlign.Center,
        )
    }
}

private enum class WalletHomeBannerPage {
    ADD_WALLET,
}
