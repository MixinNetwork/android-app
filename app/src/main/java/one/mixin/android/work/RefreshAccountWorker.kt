package one.mixin.android.work

import android.content.Context
import android.graphics.Point
import androidx.work.Worker
import androidx.work.WorkerParameters
import one.mixin.android.MixinApplication
import one.mixin.android.api.service.AccountService
import one.mixin.android.di.worker.AndroidWorkerInjector
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.putInt
import one.mixin.android.extension.saveQRCode
import one.mixin.android.repository.UserRepository
import one.mixin.android.ui.setting.SettingConversationFragment
import one.mixin.android.util.Session
import one.mixin.android.vo.MessageSource
import one.mixin.android.vo.toUser
import org.jetbrains.anko.windowManager
import javax.inject.Inject

class RefreshAccountWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {

    @Inject
    lateinit var accountService: AccountService
    @Inject
    lateinit var userRepo: UserRepository

    override fun doWork(): Result {
        AndroidWorkerInjector.inject(this)
        return try {
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
                    .getInt(SettingConversationFragment.CONVERSATION_KEY, MessageSource.EVERYBODY.ordinal)
                if (response.data!!.receive_message_source == MessageSource.EVERYBODY.name &&
                    receive != MessageSource.EVERYBODY.ordinal) {
                    MixinApplication.appContext.defaultSharedPreferences
                        .putInt(SettingConversationFragment.CONVERSATION_KEY, MessageSource.EVERYBODY.ordinal)
                } else if (response.data!!.receive_message_source == MessageSource.CONTACTS.name &&
                    receive != MessageSource.CONTACTS.ordinal) {
                    MixinApplication.appContext.defaultSharedPreferences
                        .putInt(SettingConversationFragment.CONVERSATION_KEY, MessageSource.CONTACTS.ordinal)
                }

                val receiveGroup = MixinApplication.appContext.defaultSharedPreferences
                    .getInt(SettingConversationFragment.CONVERSATION_GROUP_KEY, MessageSource.EVERYBODY.ordinal)
                if (response.data!!.accept_conversation_source == MessageSource.EVERYBODY.name &&
                    receiveGroup != MessageSource.EVERYBODY.ordinal) {
                    MixinApplication.appContext.defaultSharedPreferences
                        .putInt(SettingConversationFragment.CONVERSATION_GROUP_KEY, MessageSource.EVERYBODY.ordinal)
                } else if (response.data!!.accept_conversation_source == MessageSource.CONTACTS.name &&
                    receiveGroup != MessageSource.CONTACTS.ordinal) {
                    MixinApplication.appContext.defaultSharedPreferences
                        .putInt(SettingConversationFragment.CONVERSATION_GROUP_KEY, MessageSource.CONTACTS.ordinal)
                }
                Result.SUCCESS
            } else {
                Result.FAILURE
            }
        } catch (e: Exception) {
            Result.FAILURE
        }
    }
}