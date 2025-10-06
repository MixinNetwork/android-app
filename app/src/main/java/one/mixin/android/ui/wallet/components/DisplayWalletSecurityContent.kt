package one.mixin.android.ui.wallet.components

import PageScaffold
import androidx.compose.runtime.Composable
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.landing.components.MnemonicPhraseInput
import one.mixin.android.ui.landing.components.MnemonicState
import one.mixin.android.ui.wallet.WalletSecurityActivity.Mode

@Composable
fun DisplayWalletSecurityContent(
    mode: Mode,
    securityContent: String?,
    onQrCode: ((List<String>) -> Unit),
    pop: () -> Unit
) {
    MixinAppTheme {
        PageScaffold(
            title = "",
            verticalScrollable = false,
            backIcon = R.drawable.ic_close_black,
            pop = pop,
        ) {
            if (mode == Mode.VIEW_PRIVATE_KEY) {
                DisplayPrivateKeyContent(securityContent, pop)
            } else {
                val words = securityContent?.split(" ") ?: emptyList()
                MnemonicPhraseInput(
                    MnemonicState.Display,
                    mnemonicList = words,
                    onComplete = { pop.invoke() },
                    onQrCode = onQrCode
                )
            }
        }
    }
}
