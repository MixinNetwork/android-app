package one.mixin.android.ui.wallet.components

import android.content.Context
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.openUrl
import one.mixin.android.job.RefreshWeb3Job
import one.mixin.android.ui.wallet.alert.components.cardBackground

const val PREF_NAME = "wallet_info_card"
private const val KEY_HIDE_PRIVACY_WALLET_INFO = "hide_privacy_wallet_info"
private const val KEY_HIDE_COMMON_WALLET_INFO = "hide_common_wallet_info"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AssetDashboardScreen(
    onWalletCardClick: (destination: WalletDestination) -> Unit,
    onAddWalletClick: () -> Unit,
    viewModel: AssetDistributionViewModel = hiltViewModel()
) {
    val tokenDistribution by viewModel.tokenDistribution.collectAsState(initial = emptyList())
    val wallets by viewModel.wallets.collectAsState(initial = emptyList())

    val tokenTotalBalance by viewModel.tokenTotalBalance.collectAsState()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) }
    val hidePrivacyWalletInfo = remember { mutableStateOf(prefs.getBoolean(KEY_HIDE_PRIVACY_WALLET_INFO, false)) }
    val hideCommonWalletInfo = remember { mutableStateOf(prefs.getBoolean(KEY_HIDE_COMMON_WALLET_INFO, false)) }

    MixinAppTheme(skip = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.Wallets),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MixinAppTheme.colors.textPrimary,
                    modifier = Modifier
                        .height(56.dp)
                        .wrapContentHeight(Alignment.CenterVertically)
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    painter = painterResource(R.drawable.ic_add_black_24dp),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.icon,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            onAddWalletClick()
                        }
                )
            }
            TotalAssetsCard()
            Spacer(modifier = Modifier.height(20.dp))
            
            WalletCard(
                balance = tokenTotalBalance,
                assets = tokenDistribution,
                destination = WalletDestination.Privacy,
                onClick = { onWalletCardClick.invoke(WalletDestination.Privacy) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            wallets.forEach { wallet ->
                if (wallet.category == RefreshWeb3Job.WALLET_CATEGORY_PRIVATE) {
                    val web3TokenTotalBalance by viewModel.web3TokenTotalBalanceFlow(wallet.id).collectAsState()
                    val web3TokenDistribution by viewModel.web3TokenDistributionFlow(wallet.id).collectAsState(initial = emptyList())
                    WalletCard(
                        name = wallet.name,
                        balance = web3TokenTotalBalance,
                        assets = web3TokenDistribution,
                        destination = WalletDestination.Import(wallet.id),
                        onClick = { onWalletCardClick.invoke(WalletDestination.Import(wallet.id)) }
                    )
                } else {
                    val web3TokenTotalBalance by viewModel.web3TokenTotalBalanceFlow(wallet.id).collectAsState()
                    val web3TokenDistribution by viewModel.web3TokenDistributionFlow(wallet.id).collectAsState(initial = emptyList())
                    WalletCard(
                        balance = web3TokenTotalBalance,
                        assets = web3TokenDistribution,
                        destination = WalletDestination.Classic(wallet.id),
                        onClick = { onWalletCardClick.invoke(WalletDestination.Classic(wallet.id)) }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (!hidePrivacyWalletInfo.value || !hideCommonWalletInfo.value) {
                Spacer(modifier = Modifier.weight(1f))
                WalletInfoCard(
                    hidePrivacyWalletInfo = hidePrivacyWalletInfo.value,
                    hideCommonWalletInfo = hideCommonWalletInfo.value,
                    onPrivacyClose = {
                        hidePrivacyWalletInfo.value = true
                        prefs.edit { putBoolean(KEY_HIDE_PRIVACY_WALLET_INFO, true) }
                    },
                    onCommonClose = {
                        hideCommonWalletInfo.value = true
                        prefs.edit { putBoolean(KEY_HIDE_COMMON_WALLET_INFO, true) }
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WalletInfoCard(
    hidePrivacyWalletInfo: Boolean,
    hideCommonWalletInfo: Boolean,
    onPrivacyClose: () -> Unit,
    onCommonClose: () -> Unit
) {
    val initialPage = if (hidePrivacyWalletInfo && !hideCommonWalletInfo) 0 else 0
    val pageCount = if (!hidePrivacyWalletInfo && !hideCommonWalletInfo) 2 else 1
    
    val pagerState = rememberPagerState(initialPage = initialPage) { pageCount }
    val context = LocalContext.current
    
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                if (!hidePrivacyWalletInfo && !hideCommonWalletInfo) {
                    when (page) {
                        0 -> PrivacyWalletInfo(
                            onLearnMoreClick = {
                                context.openUrl(context.getString(R.string.url_privacy_wallet))
                            },
                            onClose = onPrivacyClose
                        )
                        1 -> CommonWalletInfo(
                            onLearnMoreClick = {
                                context.openUrl(context.getString(R.string.url_classic_wallet))
                            },
                            onClose = onCommonClose
                        )
                    }
                } else if (!hidePrivacyWalletInfo) {
                    PrivacyWalletInfo(
                        onLearnMoreClick = {
                            context.openUrl("https://support.mixin.one/zh/article/5lua5lmi5piv6zqq56eb6zkx5yyf77yf-1s7o0e2/")
                        },
                        onClose = onPrivacyClose
                    )
                } else if (!hideCommonWalletInfo) {
                    CommonWalletInfo(
                        onLearnMoreClick = {
                            context.openUrl("https://support.mixin.one/zh/article/5lua5lmi5piv5pmu6yca6zkx5yyf77yf-8308b1/")
                        },
                        onClose = onCommonClose
                    )
                }
            }
            
            if (!hidePrivacyWalletInfo && !hideCommonWalletInfo) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    repeat(2) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MixinAppTheme.colors.accent else MixinAppTheme.colors.borderColor
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacyWalletInfo(
    onLearnMoreClick: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
        .padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.Whats_Privacy_Wallet),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MixinAppTheme.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            
            Image(
                painter = painterResource(id = R.drawable.ic_close_grey),
                contentDescription = stringResource(R.string.Close),
                modifier = Modifier
                    .size(12.dp)
                    .clickable { onClose() }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.Privacy_Wallet_Description),
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textMinor,
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Image(
                painter = painterResource(id = R.drawable.ic_privacy),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onLearnMoreClick() }
        ) {
            Text(
                text = stringResource(R.string.Learn_More),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MixinAppTheme.colors.accent
            )
        }
    }
}

@Composable
fun CommonWalletInfo(
    onLearnMoreClick: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
        .padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.Whats_Common_Wallet),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MixinAppTheme.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            
            Image(
                painter = painterResource(id = R.drawable.ic_close_grey),
                contentDescription = stringResource(R.string.Close),
                modifier = Modifier
                    .size(12.dp)
                    .clickable { onClose() }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.Common_Wallet_Description),
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textMinor,
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Image(
                painter = painterResource(id = R.drawable.ic_common),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onLearnMoreClick() }
        ) {
            Text(
                text = stringResource(R.string.Learn_More),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MixinAppTheme.colors.accent
            )
        }
    }
}
