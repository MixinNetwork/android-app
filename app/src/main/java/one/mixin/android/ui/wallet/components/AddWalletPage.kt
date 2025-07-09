package one.mixin.android.ui.wallet.components

import androidx.compose.runtime.Composable
import one.mixin.android.ui.landing.components.MnemonicPhraseInput
import one.mixin.android.ui.landing.components.MnemonicState

@Composable
fun AddWalletPage(
    mnemonicList: List<String> = emptyList(),
    onComplete: (List<String>) -> Unit,
    onScan: () -> Unit
) {
    MnemonicPhraseInput(
        state = MnemonicState.Import,
        mnemonicList = mnemonicList,
        onComplete = onComplete,
        onScan = onScan
    )
}