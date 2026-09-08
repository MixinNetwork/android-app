package one.mixin.android.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import one.mixin.android.Constants
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences

internal enum class BalanceChangeTone {
    POSITIVE,
    NEGATIVE,
    PLAIN,
}

internal data class BalanceChangePresentation(
    val amount: String,
    val tone: BalanceChangeTone,
)

internal fun balanceChangePresentation(
    amount: String,
    isReceive: Boolean? = null,
): BalanceChangePresentation {
    val displayAmount = if (isReceive == null) {
        amount.withBalanceChangeSign()
    } else {
        val magnitude = amount.toBigDecimalOrNull()?.let { it.abs().toPlainString() }
            ?: amount.removePrefix("+").removePrefix("-")
        if (isReceive) "+$magnitude" else "-$magnitude"
    }
    val tone = when (displayAmount.toBigDecimalOrNull()?.signum()) {
        1 -> BalanceChangeTone.POSITIVE
        -1 -> BalanceChangeTone.NEGATIVE
        else -> BalanceChangeTone.PLAIN
    }
    return BalanceChangePresentation(displayAmount, tone)
}

@Composable
internal fun BalanceChangeTone.toColor(): Color {
    val quoteColorReversed = LocalContext.current.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    return when (this) {
        BalanceChangeTone.POSITIVE -> if (quoteColorReversed) {
            MixinAppTheme.colors.walletRed
        } else {
            MixinAppTheme.colors.walletGreen
        }
        BalanceChangeTone.NEGATIVE -> if (quoteColorReversed) {
            MixinAppTheme.colors.walletGreen
        } else {
            MixinAppTheme.colors.walletRed
        }
        BalanceChangeTone.PLAIN -> MixinAppTheme.colors.textPrimary
    }
}

private fun String.withBalanceChangeSign(): String {
    val value = toBigDecimalOrNull() ?: return this
    return when {
        value.signum() > 0 && !startsWith("+") -> "+$this"
        value.signum() == 0 -> removePrefix("+")
        else -> this
    }
}
