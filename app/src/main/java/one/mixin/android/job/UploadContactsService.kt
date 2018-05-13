package one.mixin.android.job

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.support.v4.app.JobIntentService
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import dagger.android.AndroidInjection
import ir.mirrajabi.rxcontacts.Contact
import ir.mirrajabi.rxcontacts.RxContacts
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.ContactRequest
import one.mixin.android.api.service.ContactService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class UploadContactsService : JobIntentService() {

    @Inject
    lateinit var mContactsApi: ContactService
    private val contentObserver = object : ContentObserver(Handler()) {

        override fun deliverSelfNotifications(): Boolean {
            return true
        }

        override fun onChange(selfChange: Boolean) {
            onChange(selfChange, null)
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            uploadContacts()
        }
    }

    private fun uploadContacts() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        RxContacts.fetch(this)
            .toSortedList(Contact::compareTo)
            .subscribe({ contacts ->
                val mutableList = mutableListOf<ContactRequest>()
                for (item in contacts) {
                    for (p in item.phoneNumbers) {
                        if (p == null) {
                            continue
                        }
                        try {
                            val phone = PhoneNumberUtils.formatNumberToE164(p,
                                telephonyManager.simCountryIso.toUpperCase())
                            if (phone != null) {
                                mutableList.add(ContactRequest(phone, item.displayName))
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
                val call = mContactsApi.syncContacts(mutableList)
                call.enqueue(object : Callback<MixinResponse<Any>> {
                    override fun onResponse(call: Call<MixinResponse<Any>>, response: Response<MixinResponse<Any>>) {}

                    override fun onFailure(call: Call<MixinResponse<Any>>, t: Throwable) {}
                })
            }, { _ -> })
    }

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
        contentResolver.registerContentObserver(android.provider.ContactsContract.Contacts.CONTENT_URI,
            true, contentObserver)
    }

    override fun onDestroy() {
        contentResolver.unregisterContentObserver(contentObserver)
        super.onDestroy()
    }

    override fun onHandleWork(intent: Intent) {
        val type = intent.getIntExtra(TYPE, -1)
        if (type == TYPE_START_UPLOAD) {
            uploadContacts()
        }
    }

    companion object {
        val TAG = UploadContactsService::class.java.simpleName!!
        const val TYPE = "type"
        const val TYPE_START_UPLOAD = 1
        private const val TYPE_LISTEN = 2

        fun listenContacts(context: Context) {
            val intent = Intent(context, UploadContactsService::class.java)
            intent.putExtra(TYPE, TYPE_LISTEN)
            context.startService(intent)
        }

        fun startUploadContacts(context: Context) {
            val intent = Intent(context, UploadContactsService::class.java)
            intent.putExtra(TYPE, TYPE_START_UPLOAD)
            context.startService(intent)
        }
    }
}
