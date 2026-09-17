package one.mixin.android.ui.wallet.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.ui.wallet.home.WalletHomeCallbacks
import one.mixin.android.ui.wallet.home.WalletHomeCashAccount
import one.mixin.android.ui.wallet.home.WalletHomeState
import one.mixin.android.ui.wallet.home.WalletHomeEarnAccount
import one.mixin.android.ui.wallet.home.WalletAssetIcon
import one.mixin.android.ui.wallet.home.maxApyText
import java.math.BigDecimal

private val AccountCardShape = RoundedCornerShape(8.dp)
private val AccountTextStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

@Composable
internal fun WalletHomeAccountCards(
    state: WalletHomeState,
    callbacks: WalletHomeCallbacks,
) {
    if (state.earnAccounts.isEmpty()) return

    if (state.cashAccount == null) {
        EarnAccountCard(
            accounts = state.earnAccounts,
            callbacks = callbacks,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            CompactCashAccountCard(
                account = state.cashAccount,
                quoteColorReversed = state.quoteColorReversed,
                callbacks = callbacks,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            CompactEarnAccountCard(
                accounts = state.earnAccounts,
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
        contentPadding = PaddingValues(16.dp),
    ) {
        Text(
            text = stringResource(R.string.cash_balance),
            color = MixinAppTheme.colors.textAssist,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.W400,
            style = AccountTextStyle,
            modifier = Modifier.heightIn(min = 16.dp).wrapContentHeight(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        WalletHomeAccountBalance(
            balanceAmountText = account.balanceAmountText,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            account.apyText?.let { apyText ->
                AccountApyBadge(
                    text = stringResource(R.string.cash_account_apy, apyText),
                    color = apyColor,
                    modifier = Modifier.weight(1f, fill = false),
                )
            } ?: Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(4.dp))
            AccountArrow()
        }
    }
}

@Composable
private fun CompactEarnAccountCard(
    accounts: List<WalletHomeEarnAccount>,
    callbacks: WalletHomeCallbacks,
    modifier: Modifier,
) {
    val account = accounts.summary()
    AccountCardSurface(
        modifier = modifier
            .clickable { callbacks.onEarnAccountClicked() },
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.earn_balance),
                color = MixinAppTheme.colors.textAssist,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.W400,
                style = AccountTextStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(4.dp))
            EarnTokenIcons(accounts, modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        WalletHomeAccountBalance(
            balanceAmountText = account.balanceAmountText,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            account.apyText?.let { apyText ->
                AccountApyBadge(
                    text = stringResource(R.string.earn_account_max_apy, apyText),
                    modifier = Modifier.weight(1f, fill = false),
                )
            } ?: Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(4.dp))
            AccountArrow()
        }
    }
}

@Composable
private fun EarnAccountCard(
    accounts: List<WalletHomeEarnAccount>,
    callbacks: WalletHomeCallbacks,
    modifier: Modifier,
) {
    val account = accounts.summary()
    AccountCardSurface(
        modifier = modifier.clickable { callbacks.onEarnAccountClicked() },
        contentPadding = PaddingValues(start = 16.dp, top = 20.dp, end = 20.dp, bottom = 20.dp),
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
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.earn_balance),
                        color = MixinAppTheme.colors.textAssist,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.W400,
                        style = AccountTextStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    EarnTokenIcons(accounts)
                    Spacer(modifier = Modifier.width(4.dp))
                    AccountArrow()
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WalletHomeAccountBalance(
                        balanceAmountText = account.balanceAmountText,
                        modifier = Modifier.weight(1f),
                    )
                    account.apyText?.let { apyText ->
                        Spacer(modifier = Modifier.width(8.dp))
                        AccountApyBadge(text = stringResource(R.string.earn_account_max_apy, apyText))
                    }
                }
            }
        }
    }
}

@Composable
internal fun WalletHomeAccountBalance(
    balanceAmountText: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.heightIn(min = 22.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        BasicText(
            text = balanceAmountText,
            modifier = Modifier
                .weight(1f, fill = false)
                .alignByBaseline(),
            style = AccountTextStyle.copy(
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.W600,
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 10.sp,
                maxFontSize = 18.sp,
                stepSize = 0.5.sp,
            ),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "USD",
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.W400,
            style = AccountTextStyle,
            maxLines = 1,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

@Composable
internal fun AccountApyBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MixinAppTheme.colors.walletGreen,
) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.W400,
        style = AccountTextStyle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 3.dp, vertical = 1.dp),
    )
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
internal fun AccountArrow() {
    Icon(
        painter = painterResource(R.drawable.ic_earn_link),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = Modifier.size(16.dp),
    )
}

@Composable
private fun EarnTokenIcons(
    accounts: List<WalletHomeEarnAccount>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.requiredHeight(18.dp),
        horizontalArrangement = Arrangement.spacedBy((-6).dp),
    ) {
        accounts.distinctBy { it.assetId }.take(2).forEach { account ->
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(MixinAppTheme.colors.background, CircleShape)
                    .padding(1.dp),
            ) {
                WalletAssetIcon(
                    iconUrl = account.iconUrl,
                    chainIconUrl = null,
                    collectionHash = null,
                    size = 16.dp,
                )
            }
        }
    }
}

private fun List<WalletHomeEarnAccount>.summary(): WalletHomeEarnAccount {
    val first = first()
    return WalletHomeEarnAccount(
        assetId = first.assetId,
        assetSymbol = first.assetSymbol,
        iconUrl = first.iconUrl,
        balanceUsd = fold(BigDecimal.ZERO) { total, account -> total + account.balanceUsd },
        earningsUsd = fold(BigDecimal.ZERO) { total, account -> total + account.earningsUsd },
        apyText = maxApyText(),
    )
}
