package one.mixin.android.ui.wallet

import com.checkout.frames.style.theme.DefaultPaymentFormTheme
import com.checkout.frames.style.theme.PaymentFormComponentBuilder
import com.checkout.frames.style.theme.PaymentFormComponentField
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

    private val paymentNightFormThemeColors = PaymentFormThemeColors(
        accentColor = 0XFFFFFFFF,
        textColor = 0XFFFFFFFF,
        errorColor = 0XFFE55541,
        backgroundColor = 0xFF2C3136,
        fieldBackgroundColor = 0XFF23272B,
        enabledButtonColor = 0xFF4191FF,
        disabledButtonColor = 0XFF808691,
    )

    fun providePaymentFormTheme(isNightMode: Boolean): PaymentFormTheme {
        return PaymentFormTheme(
            paymentFormThemeColors = if (isNightMode) paymentNightFormThemeColors else paymentFormThemeColors,
            paymentFormComponents = DefaultPaymentFormTheme.providePaymentFormComponents(
                addBillingSummaryButton = PaymentFormComponentBuilder()
                    .setIsFieldOptional(true)
                    .setPaymentFormField(PaymentFormComponentField.AddBillingSummaryButton)
                    .build(),
                editBillingSummaryButton = PaymentFormComponentBuilder()
                    .setIsFieldOptional(true)
                    .setPaymentFormField(PaymentFormComponentField.AddBillingSummaryButton)
                    .build(),
            ),
        )
    }
}
