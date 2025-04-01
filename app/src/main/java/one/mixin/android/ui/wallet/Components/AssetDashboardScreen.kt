package one.mixin.android.ui.wallet.components

import android.content.Context
import android.content.SharedPreferences
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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.wallet.alert.components.cardBackground
import androidx.core.content.edit

private const val PREF_NAME = "wallet_info_card"
private const val KEY_HIDE_WALLET_INFO = "hide_wallet_info"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AssetDashboardScreen(
    onWalletCardClick: (destination: WalletDestination, walletId: String?) -> Unit,
    viewModel: AssetDistributionViewModel = hiltViewModel()
) {
    val tokenDistribution by viewModel.tokenDistribution.collectAsState(initial = emptyList())
    val web3TokenDistribution by viewModel.web3TokenDistribution.collectAsState(initial = emptyList())
    val wallets by viewModel.wallets.collectAsState(initial = emptyList())

    val tokenTotalBalance by viewModel.tokenTotalBalance.collectAsState()
    val web3TokenTotalBalance by viewModel.web3TokenTotalBalance.collectAsState()
    
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) }
    val hideWalletInfo = remember { mutableStateOf(prefs.getBoolean(KEY_HIDE_WALLET_INFO, false)) }

    MixinAppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.Wallet),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MixinAppTheme.colors.textPrimary,
                modifier = Modifier
                    .height(56.dp)
                    .wrapContentHeight(Alignment.CenterVertically)
            )
            TotalAssetsCard()
            Spacer(modifier = Modifier.height(20.dp))
            
            WalletCard(
                balance = tokenTotalBalance,
                assets = tokenDistribution,
                destination = WalletDestination.Privacy,
                onClick = { onWalletCardClick.invoke(WalletDestination.Privacy, WalletDestination.Privacy.name) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            wallets.forEach { wallet ->
                WalletCard(
                    balance = web3TokenTotalBalance,
                    assets = web3TokenDistribution,
                    destination = WalletDestination.Classic,
                    onClick = { onWalletCardClick.invoke(WalletDestination.Classic, wallet.id) }
                )
            }


            if (!hideWalletInfo.value) {
                Spacer(modifier = Modifier.weight(1f))
                WalletInfoCard(
                    onClose = {
                        hideWalletInfo.value = true
                        prefs.edit { putBoolean(KEY_HIDE_WALLET_INFO, true) }
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
    onClose: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
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
                when (page) {
                    0 -> PrivacyWalletInfo(
                        onLearnMoreClick = {
                            context.openUrl("https://support.mixin.one/zh/article/5lua5lmi5piv6zqq56eb6zkx5yyf77yf-1s7o0e2/")
                        },
                        onClose = onClose
                    )
                    1 -> CommonWalletInfo(
                        onLearnMoreClick = {
                            context.openUrl("https://support.mixin.one/zh/article/5lua5lmi5piv5pmu6yca6zkx5yyf77yf-8308b1/")
                        },
                        onClose = onClose
                    )
                }
            }
            
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

@Composable
fun PrivacyWalletInfo(
    onLearnMoreClick: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()
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
    Column(modifier = Modifier.fillMaxWidth()
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
