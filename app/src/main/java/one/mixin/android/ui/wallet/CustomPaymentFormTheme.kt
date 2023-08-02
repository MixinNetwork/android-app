package one.mixin.android.ui.wallet

import com.checkout.frames.style.theme.PaymentFormTheme
import com.checkout.frames.style.theme.PaymentFormThemeColors

object CustomPaymentFormTheme {
    private val paymentFormThemeColors = PaymentFormThemeColors(
        accentColor = 0XFF00CC2D,
        textColor = 0XFFB1B1B1,
        errorColor = 0XFFFF0000,
        backgroundColor = 0xFF17201E,
        fieldBackgroundColor = 0XFF24302D,
        enabledButtonColor = 0xFFFFFFFF,
        disabledButtonColor = 0XFF003300,
    )

    fun providePaymentFormTheme(): PaymentFormTheme {
        return PaymentFormTheme(
            paymentFormThemeColors = paymentFormThemeColors,
        )
    }
}
