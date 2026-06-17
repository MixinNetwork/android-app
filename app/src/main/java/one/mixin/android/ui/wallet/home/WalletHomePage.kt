package one.mixin.android.ui.wallet.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.home.components.BannerPager
import one.mixin.android.ui.wallet.home.components.ImportSafetyFooter
import one.mixin.android.ui.wallet.home.components.SupportCard
import one.mixin.android.ui.wallet.home.components.WalletHomeCard

@Composable
fun WalletHomePage(
    state: WalletHomeState,
    callbacks: WalletHomeCallbacks,
) {
    MixinAppTheme {
        val scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()
        val scrollingCallbacks = remember(callbacks, scrollState, coroutineScope) {
            object : WalletHomeCallbacks by callbacks {
                override fun onSwapClicked() {
                    coroutineScope.launch {
                        scrollState.scrollTo(0)
                        callbacks.onSwapClicked()
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MixinAppTheme.colors.background)
                .verticalScroll(scrollState),
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            if (state.isLoading && state.cards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(top = 100.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MixinAppTheme.colors.accent,
                        strokeWidth = 3.dp,
                    )
                }
            }

            state.cards.forEachIndexed { index, card ->
                when (card) {
                    WalletHomeCardType.BANNER -> BannerPager(state, scrollingCallbacks)
                    WalletHomeCardType.SUPPORT -> SupportCard(scrollingCallbacks)
                    else -> WalletHomeCard(card, state, scrollingCallbacks)
                }
                if (index != state.cards.lastIndex || !state.showImportSafetyFooter) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
            if (state.showImportSafetyFooter) {
                Spacer(modifier = Modifier.height(36.dp))
                ImportSafetyFooter(state.walletType)
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
