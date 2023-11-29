package one.mixin.android.job

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.MixinApplication
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.createContactsRequests
import one.mixin.android.util.rxcontact.Contact
import one.mixin.android.util.rxcontact.RxContacts

class UploadContactsJob : BaseJob(Params(PRIORITY_BACKGROUND).requireNetwork()) {
    @SuppressLint("CheckResult")
    override fun onRun() {
        val ctx = MixinApplication.appContext
        if (ctx.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        RxContacts.fetch(ctx)
            .toSortedList(Contact::compareTo)
            .subscribe(
                { contacts ->
                    val mutableList = createContactsRequests(contacts)
                    if (mutableList.isEmpty()) return@subscribe
                    runBlocking {
                        handleMixinResponse(
                            invokeNetwork = { contactService.syncContacts(mutableList) },
                            successBlock = {},
                        )
                    }
                },
                { },
            )
    }

    companion object {
        private var serialVersionUID: Long = 1L
    }
}
