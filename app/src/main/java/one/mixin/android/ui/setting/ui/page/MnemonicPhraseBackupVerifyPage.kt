package one.mixin.android.ui.setting.ui.page

import PageScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.landing.MnemonicPhrases
import one.mixin.android.ui.landing.components.MnemonicPhraseInput
import one.mixin.android.ui.landing.components.MnemonicState
import kotlin.random.Random

@Composable
fun MnemonicPhraseBackupVerifyPage(pop: () -> Unit, next: (List<String>) -> Unit) {
    var mnemonicList by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(Unit) {
        mnemonicList = MnemonicPhrases.shuffled(Random).take(13)
    }

    MixinAppTheme {
        PageScaffold(
            title = stringResource(R.string.Mnemonic_Phrase),
            verticalScrollable = false,
            pop = pop,
        ) {
            MnemonicPhraseInput(MnemonicState.Verify, mnemonicList = mnemonicList, onComplete = { next.invoke(mnemonicList) })
        }
    }
}