package one.mixin.android.ui.wallet.components

import androidx.compose.runtime.Composable
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.landing.components.MnemonicPhraseInput
import one.mixin.android.ui.landing.components.MnemonicState

@Composable
fun AddWalletPage() {
    MixinAppTheme {
        MnemonicPhraseInput(MnemonicState.Input, onComplete = {

        })
    }
}