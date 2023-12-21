package one.mixin.android.ui.wallet

import com.checkout.frames.model.CornerRadius
import com.checkout.frames.model.Shape
import com.checkout.frames.style.theme.DefaultPaymentFormTheme
import com.checkout.frames.style.theme.PaymentFormComponentBuilder
import com.checkout.frames.style.theme.PaymentFormComponentField
import com.checkout.frames.style.theme.PaymentFormCornerRadius
import com.checkout.frames.style.theme.PaymentFormShape
import com.checkout.frames.style.theme.PaymentFormTheme
import com.checkout.frames.style.theme.PaymentFormThemeColors
import one.mixin.android.R

object CustomPaymentFormTheme {
    private val paymentFormThemeColors =
        PaymentFormThemeColors(
            accentColor = 0XFF333333,
            textColor = 0XFFFFFFFF,
            errorColor = 0XFFE55541,
            backgroundColor = 0xFFFFFFFF,
            fieldBackgroundColor = 0XFFF6F7FA,
            enabledButtonColor = 0xFF3D75E3,
            disabledButtonColor = 0XFFE5E5E5,
        )

    private val paymentNightFormThemeColors =
        PaymentFormThemeColors(
            accentColor = 0XFFFFFFFF,
            textColor = 0XFFFFFFFF,
            errorColor = 0XFFE55541,
            backgroundColor = 0xFF2C3136,
            fieldBackgroundColor = 0XFF23272B,
            enabledButtonColor = 0xFF3D75E3,
            disabledButtonColor = 0XFF3B3F44,
        )

    fun providePaymentFormTheme(isNightMode: Boolean): PaymentFormTheme {
        return PaymentFormTheme(
            paymentFormThemeColors = if (isNightMode) paymentNightFormThemeColors else paymentFormThemeColors,
            paymentFormComponents =
                DefaultPaymentFormTheme.providePaymentFormComponents(
                    paymentHeaderTitle =
                        PaymentFormComponentBuilder().setPaymentFormField(
                            PaymentFormComponentField.PaymentHeaderTitle,
                        ).setTitleTextId(R.string.Add_New_Card).setBackIconImage(R.drawable.ic_back_route)
                            .build(),
                    cardScheme =
                        PaymentFormComponentBuilder().setPaymentFormField(
                            PaymentFormComponentField.CardScheme,
                        ).setTitleTextId(
                            R.string.Accepted_Cards,
                        ).build(),
                    cardNumber =
                        PaymentFormComponentBuilder().setPaymentFormField(
                            PaymentFormComponentField.CardNumber,
                        ).setTitleTextId(R.string.Card_Number).build(),
                    expiryDate =
                        PaymentFormComponentBuilder().setPaymentFormField(
                            PaymentFormComponentField.ExpiryDate,
                        ).setTitleTextId(R.string.Expiry_Date)
                            .setSubTitleTextId(R.string.Expiry_date_sub_title).build(),
                    cvv =
                        PaymentFormComponentBuilder().setPaymentFormField(PaymentFormComponentField.CVV)
                            .setTitleTextId(
                                R.string.Security_Code,
                            )
                            .setSubTitleTextId(R.string.Security_code_sub_title).build(),
                    payButton =
                        PaymentFormComponentBuilder().setPaymentFormField(
                            PaymentFormComponentField.PaymentDetailsButton,
                        ).setTitleTextId(
                            R.string.Add_New_Card,
                        ).build(),
                    addBillingSummaryButton =
                        PaymentFormComponentBuilder()
                            .setIsFieldOptional(true)
                            .setPaymentFormField(PaymentFormComponentField.AddBillingSummaryButton)
                            .build(),
                    editBillingSummaryButton =
                        PaymentFormComponentBuilder()
                            .setIsFieldOptional(true)
                            .setPaymentFormField(PaymentFormComponentField.AddBillingSummaryButton)
                            .build(),
                    cardHolderName =
                        PaymentFormComponentBuilder()
                            .setPaymentFormField(PaymentFormComponentField.CardHolderName)
                            .setTitleTextId(R.string.Cardholder_Name)
                            .setIsFieldOptional(false)
                            .setIsFieldHidden(false).build(),
                ),
            paymentFormShape =
                PaymentFormShape(
                    inputFieldShape = Shape.RoundCorner,
                    buttonShape = Shape.RoundCorner,
                ),
            paymentFormCornerRadius =
                PaymentFormCornerRadius(
                    inputFieldCornerRadius = CornerRadius(8),
                    buttonCornerRadius = CornerRadius(8),
                    addressSummaryCornerRadius = CornerRadius(8),
                ),
        )
    }
}
