package one.mixin.android.job

import android.graphics.Point
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.MixinApplication
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.putInt
import one.mixin.android.extension.saveQRCode
import one.mixin.android.session.Session
import one.mixin.android.ui.setting.SettingConversationFragment
import one.mixin.android.vo.MessageSource
import one.mixin.android.vo.toUser
import org.jetbrains.anko.windowManager

class RefreshAccountJob : BaseJob(Params(PRIORITY_UI_HIGH).addTags(GROUP).requireNetwork().persist()) {

    companion object {
        private const val serialVersionUID = 1L
        private const val GROUP = "RefreshAccountJob"
    }

    override fun onRun() = runBlocking {
        val response = accountService.getMe().execute().body()
        if (response != null && response.isSuccess && response.data != null) {
            val account = response.data
            val u = account!!.toUser()
            userRepo.upsert(u)
            Session.storeAccount(account)
            if (account.codeId.isNotEmpty()) {
                val p = Point()
                MixinApplication.appContext.windowManager.defaultDisplay?.getSize(p)
                val size = minOf(p.x, p.y)
                val b = account.codeUrl.generateQRCode(size, MixinApplication.get().isNightMode())
                b?.saveQRCode(MixinApplication.appContext, account.userId)
            }

            val receive = MixinApplication.appContext.defaultSharedPreferences
                .getInt(SettingConversationFragment.CONVERSATION_KEY, MessageSource.EVERYBODY.ordinal)
            if (response.data!!.receiveMessageSource == MessageSource.EVERYBODY.name &&
                receive != MessageSource.EVERYBODY.ordinal
            ) {
                MixinApplication.appContext.defaultSharedPreferences
                    .putInt(SettingConversationFragment.CONVERSATION_KEY, MessageSource.EVERYBODY.ordinal)
            } else if (response.data!!.receiveMessageSource == MessageSource.CONTACTS.name &&
                receive != MessageSource.CONTACTS.ordinal
            ) {
                MixinApplication.appContext.defaultSharedPreferences
                    .putInt(SettingConversationFragment.CONVERSATION_KEY, MessageSource.CONTACTS.ordinal)
            }

            val receiveGroup = MixinApplication.appContext.defaultSharedPreferences
                .getInt(SettingConversationFragment.CONVERSATION_GROUP_KEY, MessageSource.EVERYBODY.ordinal)
            if (response.data!!.acceptConversationSource == MessageSource.EVERYBODY.name &&
                receiveGroup != MessageSource.EVERYBODY.ordinal
            ) {
                MixinApplication.appContext.defaultSharedPreferences
                    .putInt(SettingConversationFragment.CONVERSATION_GROUP_KEY, MessageSource.EVERYBODY.ordinal)
            } else if (response.data!!.acceptConversationSource == MessageSource.CONTACTS.name &&
                receiveGroup != MessageSource.CONTACTS.ordinal
            ) {
                MixinApplication.appContext.defaultSharedPreferences
                    .putInt(SettingConversationFragment.CONVERSATION_GROUP_KEY, MessageSource.CONTACTS.ordinal)
            }
        }
    }
}
