package one.mixin.android.ui.setting.ui.page

import PageScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.tip.Tip
import one.mixin.android.ui.landing.components.MnemonicPhraseInput
import one.mixin.android.ui.landing.components.MnemonicState

@Composable
fun MnemonicPhraseBackupVerifyPage(mnemonicList: List<String>, pop: () -> Unit, next: (List<String>) -> Unit, tip: Tip, pin: String) {
    MixinAppTheme {
        PageScaffold(
            title = stringResource(R.string.Mnemonic_Phrase),
            verticalScrollable = false,
            pop = pop,
        ) {
            MnemonicPhraseInput(MnemonicState.Verify, mnemonicList = mnemonicList, onComplete = { next.invoke(mnemonicList) }, tip, pin)
        }
    }
}