package one.mixin.android.job

import android.graphics.Point
import com.birbit.android.jobqueue.Params
import one.mixin.android.MixinApplication
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.putInt
import one.mixin.android.extension.saveQRCode
import one.mixin.android.ui.setting.SettingConversationFragment.Companion.CONVERSATION_KEY
import one.mixin.android.util.Session
import one.mixin.android.vo.MessageSource
import one.mixin.android.vo.toUser
import org.jetbrains.anko.windowManager

class RefreshAccountJob : BaseJob(Params(PRIORITY_UI_HIGH).addTags(RefreshAccountJob.GROUP).requireNetwork()) {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshAccountJob"
    }

    override fun onRun() {
        val response = accountService.getMe().execute().body()
        if (response != null && response.isSuccess && response.data != null) {
            val account = response.data
            userRepo.upsert(account!!.toUser())
            Session.storeAccount(account)
            if (account.code_id.isNotEmpty()) {
                val p = Point()
                MixinApplication.appContext.windowManager.defaultDisplay?.getSize(p)
                val size = minOf(p.x, p.y)
                val b = account.code_url.generateQRCode(size)
                b?.saveQRCode(MixinApplication.appContext, account.userId)
            }

            val receive = MixinApplication.appContext.defaultSharedPreferences
                .getInt(CONVERSATION_KEY, MessageSource.EVERYBODY.ordinal)
            if (response.data!!.receive_message_source == MessageSource.EVERYBODY.name &&
                receive != MessageSource.EVERYBODY.ordinal) {
                MixinApplication.appContext.defaultSharedPreferences
                    .putInt(CONVERSATION_KEY, MessageSource.EVERYBODY.ordinal)
            } else if (response.data!!.receive_message_source == MessageSource.CONTACTS.name &&
                receive != MessageSource.CONTACTS.ordinal) {
                MixinApplication.appContext.defaultSharedPreferences
                    .putInt(CONVERSATION_KEY, MessageSource.CONTACTS.ordinal)
            }
        }
    }
}