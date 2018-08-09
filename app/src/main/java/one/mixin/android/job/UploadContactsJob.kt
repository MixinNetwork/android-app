package one.mixin.android.job

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import com.birbit.android.jobqueue.Params
import com.google.i18n.phonenumbers.PhoneNumberUtil
import ir.mirrajabi.rxcontacts.Contact
import ir.mirrajabi.rxcontacts.RxContacts
import one.mixin.android.MixinApplication
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.ContactRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class UploadContactsJob : BaseJob(Params(PRIORITY_BACKGROUND).requireNetwork()) {

    @SuppressLint("CheckResult")
    override fun onRun() {
        val ctx = MixinApplication.appContext
        if (ctx.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        RxContacts.fetch(ctx)
            .toSortedList(Contact::compareTo)
            .subscribe({ contacts ->
                val mutableList = mutableListOf<ContactRequest>()
                for (item in contacts) {
                    for (p in item.phoneNumbers) {
                        if (p == null) {
                            continue
                        }
                        try {
                            val phoneNum = PhoneNumberUtil.getInstance().parse(p, Locale.getDefault().country)
                            val phone = PhoneNumberUtil.getInstance().format(phoneNum, PhoneNumberUtil.PhoneNumberFormat.E164)
                            if (phone != null) {
                                mutableList.add(ContactRequest(phone, item.displayName))
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
                val call = contactService.syncContacts(mutableList)
                call.enqueue(object : Callback<MixinResponse<Any>> {
                    override fun onResponse(call: Call<MixinResponse<Any>>, response: Response<MixinResponse<Any>>) {}

                    override fun onFailure(call: Call<MixinResponse<Any>>, t: Throwable) {}
                })
            }, { _ -> })
    }
}