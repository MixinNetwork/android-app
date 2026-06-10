package one.mixin.android.widget

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.autofill.AutofillValue
import androidx.appcompat.widget.AppCompatEditText
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

class AutoFillPhoneText(
    context: Context,
    attributeSet: AttributeSet,
) : AppCompatEditText(context, attributeSet) {
    @TargetApi(Build.VERSION_CODES.O)
    override fun autofill(value: AutofillValue) {
        val autoFilledText = value.textValue
        val phoneNum =
            try {
                PhoneNumberUtil.getInstance().parse(autoFilledText, Locale.getDefault().country).nationalNumber.toString()
            } catch (e: NumberParseException) {
                ""
            }
        super.autofill(AutofillValue.forText(phoneNum))
    }
}
