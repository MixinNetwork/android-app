package one.mixin.android.ui.wallet.components

import androidx.compose.runtime.Composable
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.landing.components.MnemonicPhraseInput
import one.mixin.android.ui.landing.components.MnemonicState

@Composable
fun AddWalletPage() {
    MixinAppTheme {
        // Test code
    }
    MnemonicPhraseInput(MnemonicState.Import, mnemonicList = "reason bubble doctor wolf ocean victory visual final employ lizard junior cancel benefit copper observe spider labor service odor dragon coconut twin hard sail".split(" "), onComplete = {

    })
}