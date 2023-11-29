package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName
import com.google.i18n.phonenumbers.PhoneNumberUtil
import one.mixin.android.util.isValidNumber
import one.mixin.android.util.rxcontact.Contact
import java.util.Locale

data class ContactRequest(
    val phone: String,
    @SerializedName("full_name")
    val fullName: String,
)

fun createContactsRequests(contacts: List<Contact>): List<ContactRequest> {
    val mutableList = mutableListOf<ContactRequest>()
    val phoneNumberUtil = PhoneNumberUtil.getInstance()
    for (item in contacts) {
        for (p in item.phoneNumbers) {
            if (p == null) {
                continue
            }
            try {
                val validationResult = isValidNumber(phoneNumberUtil, p, Locale.getDefault().country)
                val phoneNum = validationResult.second
                if (!validationResult.first) continue
                val phone = phoneNumberUtil.format(phoneNum, PhoneNumberUtil.PhoneNumberFormat.E164)
                if (phone != null) {
                    mutableList.add(ContactRequest(phone, item.displayName))
                }
            } catch (e: Exception) {
            }
        }
    }
    return mutableList
}
