package one.mixin.android.ui.wallet.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun AssetDashboardScreen(
    onWalletCardClick: (destination: WalletDestination, walletId: String?) -> Unit,
    viewModel: AssetDistributionViewModel = hiltViewModel()
) {
    val top3TokenDistribution by viewModel.top3TokenDistribution.collectAsState(initial = emptyList())
    val top3Web3TokenDistribution by viewModel.top3Web3TokenDistribution.collectAsState(initial = emptyList())
    val wallets by viewModel.wallets.collectAsState(initial = emptyList())

    val tokenTotalBalance by viewModel.tokenTotalBalance.collectAsState()
    val web3TokenTotalBalance by viewModel.web3TokenTotalBalance.collectAsState()

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
                assets = top3TokenDistribution,
                destination = WalletDestination.Privacy,
                onClick = { onWalletCardClick.invoke(WalletDestination.Privacy, WalletDestination.Privacy.name) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            wallets.forEach { wallet ->
                WalletCard(
                    balance = web3TokenTotalBalance,
                    assets = top3Web3TokenDistribution,
                    destination = WalletDestination.Classic,
                    onClick = { onWalletCardClick.invoke(WalletDestination.Classic, wallet.id) }
                )
            }
        }
    }
}

