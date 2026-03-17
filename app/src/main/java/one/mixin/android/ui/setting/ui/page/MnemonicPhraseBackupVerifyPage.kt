package one.mixin.android.ui.setting.ui.page

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.MixinTopAppBar
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.openUrl
import one.mixin.android.tip.Tip
import one.mixin.android.ui.landing.components.MnemonicPhraseInput
import one.mixin.android.ui.landing.components.MnemonicState

@Composable
fun MnemonicPhraseBackupVerifyPage(mnemonicList: List<String>, pop: () -> Unit, next: (List<String>) -> Unit, tip: Tip, pin: String) {
    val context = LocalContext.current
    MnemonicPhraseInput(
        state = MnemonicState.Verify,
        mnemonicList = mnemonicList,
        onComplete = { next.invoke(mnemonicList) },
        tip = tip,
        pin = pin,
        title = {
            MixinTopAppBar(
                title = {
                    Text(stringResource(R.string.Mnemonic_Phrase))
                },
                actions = {
                    IconButton(onClick = {
                        context.openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_support),
                            contentDescription = null,
                            tint = MixinAppTheme.colors.icon,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { pop() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = null,
                            tint = MixinAppTheme.colors.icon,
                        )
                    }
                }
            )
        }
    )
}