package one.mixin.android.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import one.mixin.android.compose.theme.MixinAppTheme

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
            ?: amount.trimStart('+', '-')
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
    return when (this) {
        BalanceChangeTone.POSITIVE -> MixinAppTheme.colors.walletGreen
        BalanceChangeTone.NEGATIVE -> MixinAppTheme.colors.walletRed
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
