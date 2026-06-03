package one.mixin.android.ui.wallet.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    val pages = remember(state.showAddWalletBanner, state.showCashbackBanner) {
        buildList {
            if (state.showAddWalletBanner) add(WalletHomeBannerPage.ADD_WALLET)
            if (state.showCashbackBanner) add(WalletHomeBannerPage.CASHBACK)
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
                .padding(horizontal = 16.dp)
                .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) { page ->
                    when (pages[page]) {
                        WalletHomeBannerPage.ADD_WALLET -> BannerCard(
                            iconRes = R.drawable.ic_wallet_home_add,
                            titleRes = R.string.wallet_home_add_wallet_banner_title,
                            descriptionRes = null,
                            ctaRes = R.string.wallet_home_add_wallet_banner_cta,
                            onClick = callbacks::onAddWalletClicked,
                        )
                        WalletHomeBannerPage.CASHBACK -> BannerCard(
                            iconRes = R.drawable.ic_wallet_home_buy,
                            titleRes = R.string.wallet_home_cashback_banner_title,
                            descriptionRes = null,
                            ctaRes = R.string.wallet_home_cashback_banner_cta,
                            onClick = callbacks::onBuyClicked,
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
                            WalletHomeBannerPage.CASHBACK -> callbacks.onCashbackBannerClosed()
                            null -> Unit
                        }
                    },
            )
        }
        if (pages.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(pages.size) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
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
            .padding(end = 16.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(titleRes),
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp,
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
            Spacer(modifier = Modifier.height(6.dp))
            BannerAction(textRes = ctaRes, onClick = onClick)
        }
    }
}

@Composable
internal fun ReferralBannerCard(callbacks: WalletHomeCallbacks) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(),
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_wallet_reffal),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
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
                .align(Alignment.Center)
                .padding(horizontal = 16.dp)
                .padding(top = 30.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_wallet_home_referral),
                contentDescription = null,
                modifier = Modifier.size(70.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.Referral),
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 16.sp,
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
                        withStyle(SpanStyle(color = Color(0xFF7B61FF), fontWeight = FontWeight.W600)) {
                            append(highlight)
                        }
                        append(description.substring(start + highlight.length))
                    } else {
                        append(description)
                    }
                },
                color = MixinAppTheme.colors.textAssist,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            BannerAction(textRes = R.string.Learn_More, primary = true, onClick = callbacks::onReferralClicked)
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun BannerAction(
    textRes: Int,
    primary: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (primary) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundWindow)
            .then(if (onClick == null) Modifier else Modifier.clickable { onClick() })
            .padding(horizontal = 12.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(textRes),
            color = if (primary) Color.White else MixinAppTheme.colors.accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.W500,
            textAlign = TextAlign.Center,
        )
    }
}

private enum class WalletHomeBannerPage {
    ADD_WALLET,
    CASHBACK,
}
