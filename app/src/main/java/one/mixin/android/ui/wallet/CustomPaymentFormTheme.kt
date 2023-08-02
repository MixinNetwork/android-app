package one.mixin.android.ui.wallet

import com.checkout.frames.style.theme.PaymentFormTheme
import com.checkout.frames.style.theme.PaymentFormThemeColors

object CustomPaymentFormTheme {
    private val paymentFormThemeColors = PaymentFormThemeColors(
        accentColor = 0XFF333333,
        textColor = 0XFF333333,
        errorColor = 0XFFE55541,
        backgroundColor = 0xFFFFFFFF,
        fieldBackgroundColor = 0XFFF6F7FA,
        enabledButtonColor = 0xFF3D75E3,
        disabledButtonColor = 0XFF9B9B9B,
    )

    fun providePaymentFormTheme(): PaymentFormTheme {
        return PaymentFormTheme(
            paymentFormThemeColors = paymentFormThemeColors,
        )
    }
}
