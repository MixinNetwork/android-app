package one.mixin.android.ui.home.web3.trade.perps

import androidx.compose.material.LocalTextStyle
import androidx.compose.material.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.em
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
internal fun PerpsConfirmationTheme(content: @Composable () -> Unit) {
    MixinAppTheme {
        ProvideTextStyle(LocalTextStyle.current.copy(lineHeight = 1.25.em), content)
    }
}
