package one.mixin.android.ui.wallet.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.ui.wallet.home.WalletHomeCallbacks
import one.mixin.android.ui.wallet.home.WalletHomeCashAccount
import one.mixin.android.ui.wallet.home.WalletHomeState
import one.mixin.android.ui.wallet.home.WalletHomeWealthAccount
import one.mixin.android.ui.wallet.home.WalletAssetIcon
import java.math.BigDecimal

private val AccountCardShape = RoundedCornerShape(8.dp)
private val AccountAmountFont = FontFamily(Font(R.font.mixin_font))
private val AccountTextStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

@Composable
internal fun WalletHomeAccountCards(
    state: WalletHomeState,
    callbacks: WalletHomeCallbacks,
) {
    if (state.wealthAccounts.isEmpty()) return

    if (state.cashAccount == null) {
        WealthAccountCard(
            accounts = state.wealthAccounts,
            callbacks = callbacks,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CompactCashAccountCard(
                account = state.cashAccount,
                quoteColorReversed = state.quoteColorReversed,
                callbacks = callbacks,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            CompactWealthAccountCard(
                accounts = state.wealthAccounts,
                callbacks = callbacks,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun CompactCashAccountCard(
    account: WalletHomeCashAccount,
    quoteColorReversed: Boolean,
    callbacks: WalletHomeCallbacks,
    modifier: Modifier,
) {
    val apyColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    AccountCardSurface(
        modifier = modifier
            .clickable { callbacks.onCashClicked() },
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.wallet_home_fiat_account),
            color = MixinAppTheme.colors.textAssist,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.W400,
            style = AccountTextStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = buildCompactUsdAmount(account.balanceAmountText),
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 16.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.W600,
            style = AccountTextStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            account.apyText?.let { apyText ->
                Text(
                    text = stringResource(R.string.cash_account_apy, apyText),
                    color = apyColor,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.W500,
                    style = AccountTextStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .background(apyColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 3.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            AccountArrow()
        }
    }
}

@Composable
private fun CompactWealthAccountCard(
    accounts: List<WalletHomeWealthAccount>,
    callbacks: WalletHomeCallbacks,
    modifier: Modifier,
) {
    val account = accounts.summary()
    AccountCardSurface(
        modifier = modifier
            .clickable { callbacks.onWealthAccountClicked() },
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.wallet_home_wealth_account),
            color = MixinAppTheme.colors.textAssist,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.W400,
            style = AccountTextStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = buildCompactUsdAmount(account.balanceAmountText),
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 16.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.W600,
            style = AccountTextStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            WealthTokenIcons(accounts)
            Spacer(modifier = Modifier.weight(1f))
            AccountArrow()
        }
    }
}

@Composable
private fun WealthAccountCard(
    accounts: List<WalletHomeWealthAccount>,
    callbacks: WalletHomeCallbacks,
    modifier: Modifier,
) {
    val account = accounts.summary()
    AccountCardSurface(
        modifier = modifier.clickable { callbacks.onWealthAccountClicked() },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_earn),
                contentDescription = null,
                modifier = Modifier.size(42.dp),
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.wealth_balance),
                        color = MixinAppTheme.colors.textMinor,
                        fontSize = 14.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.W400,
                        style = AccountTextStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    AccountArrow()
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = account.balanceAmountText,
                        color = MixinAppTheme.colors.textPrimary,
                        fontSize = 18.sp,
                        lineHeight = 21.sp,
                        fontFamily = AccountAmountFont,
                        fontWeight = FontWeight.W600,
                        style = AccountTextStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "USD",
                        color = MixinAppTheme.colors.textAssist,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.W500,
                        style = AccountTextStyle,
                        maxLines = 1,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    WealthTokenIcons(accounts)
                }
            }
        }
    }
}

@Composable
private fun AccountCardSurface(
    modifier: Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AccountCardShape)
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
private fun AccountArrow() {
    Icon(
        painter = painterResource(R.drawable.ic_earn_link),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = Modifier.size(16.dp),
    )
}

@Composable
private fun buildCompactUsdAmount(amount: String) = buildAnnotatedString {
    append('$')
    withStyle(
        SpanStyle(
            fontFamily = AccountAmountFont,
            fontWeight = FontWeight.W600,
        ),
    ) {
        append(amount)
    }
}

@Composable
private fun WealthTokenIcons(accounts: List<WalletHomeWealthAccount>) {
    Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
        accounts.distinctBy { it.assetId }.take(2).forEach { account ->
            WalletAssetIcon(
                iconUrl = account.iconUrl,
                chainIconUrl = null,
                collectionHash = null,
                size = 18.dp,
                chainBadgeSize = 6.dp,
            )
        }
    }
}

private fun List<WalletHomeWealthAccount>.summary(): WalletHomeWealthAccount {
    val first = first()
    return WalletHomeWealthAccount(
        assetId = first.assetId,
        assetSymbol = first.assetSymbol,
        iconUrl = first.iconUrl,
        balanceUsd = fold(BigDecimal.ZERO) { total, account -> total + account.balanceUsd },
        earningsUsd = fold(BigDecimal.ZERO) { total, account -> total + account.earningsUsd },
        apyText = mapNotNull { it.apyText }.distinct().joinToString(" / ").takeIf { it.isNotBlank() },
    )
}
