package one.mixin.android.ui.wallet.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.ui.wallet.home.PrivacyTokenRecycler
import one.mixin.android.ui.wallet.home.WalletHomeCallbacks
import one.mixin.android.ui.wallet.home.WalletHomeCardType
import one.mixin.android.ui.wallet.home.WalletHomeState
import one.mixin.android.ui.wallet.home.WalletHomeType
import one.mixin.android.ui.wallet.home.Web3TokenRecycler

@Composable
fun WalletHomeAllTokensPage(
    state: WalletHomeState,
    callbacks: WalletHomeCallbacks,
) {
    MixinAppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MixinAppTheme.colors.backgroundWindow)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            WalletHomeCard(WalletHomeCardType.BALANCE, state, callbacks)
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor),
            ) {
                val tokensEmpty = if (state.walletType == WalletHomeType.PRIVACY) {
                    state.privacyTokens.isEmpty()
                } else {
                    state.web3Tokens.isEmpty()
                }
                if (tokensEmpty) {
                    EmptyTokens()
                } else if (state.walletType == WalletHomeType.PRIVACY) {
                    PrivacyTokenRecycler(
                        tokens = state.privacyTokens,
                        onClick = callbacks::onTokenClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                    )
                } else {
                    Web3TokenRecycler(
                        tokens = state.web3Tokens,
                        onClick = callbacks::onTokenClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun EmptyTokens() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_empty_file),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.No_asset),
            color = MixinAppTheme.colors.textRemarks,
            fontSize = 14.sp,
        )
    }
}
