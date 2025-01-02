package one.mixin.android.ui.setting.ui.page

import PageScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.landing.components.MnemonicPhraseInput
import one.mixin.android.ui.landing.components.MnemonicState

@Composable
fun MnemonicPhraseBackupShownPage(mnemonicList: List<String>, pop: () -> Unit, next: (List<String>) -> Unit, onQrCode: (List<String>) -> Unit) {

    MixinAppTheme {
        PageScaffold(
            title = stringResource(R.string.Mnemonic_Phrase),
            verticalScrollable = false,
            pop = pop,
        ) {
            MnemonicPhraseInput(MnemonicState.Display, mnemonicList = mnemonicList, onComplete = { next.invoke(mnemonicList) }, onQrCode = onQrCode)
        }
    }
}