package one.mixin.android.job

import androidx.lifecycle.LiveData
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.db.MixinDatabase

import one.mixin.android.event.TipEvent
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putInt
import one.mixin.android.extension.putString
import one.mixin.android.session.Session
import one.mixin.android.ui.setting.PhoneNumberSettingFragment
import one.mixin.android.ui.setting.SettingConversationFragment
import one.mixin.android.vo.Account
import one.mixin.android.vo.MessageSource
import one.mixin.android.vo.SearchSource
import one.mixin.android.vo.toUser

class RefreshAccountJob(
    private val checkTip: Boolean = false,
) : BaseJob(Params(PRIORITY_UI_HIGH).addTags(GROUP).requireNetwork().persist()) {
    companion object {
        private const val serialVersionUID = 1L
        private const val GROUP = "RefreshAccountJob"
    }

    override fun onRun() =
        runBlocking {
            val response = accountService.getMe().execute().body()
            if (response != null && response.isSuccess && response.data != null) {
                val account = response.data ?: return@runBlocking
                updateAccount(account)

                if (checkTip && !tipCounterSynced.synced) {
                    tip.checkCounter(
                        account.tipCounter,
                        onNodeCounterNotEqualServer = { nodeMaxCounter, failedSigners ->
                            RxBus.publish(TipEvent(nodeMaxCounter, failedSigners))
                        },
                        onNodeCounterInconsistency = { nodeMaxCounter, failedSigners ->
                            RxBus.publish(TipEvent(nodeMaxCounter, failedSigners))
                        },
                    ).onSuccess {
                        tipCounterSynced.synced = true
                    }
                }
            }
        }
}

fun updateAccount(account: Account) {
    val db = MixinDatabase.getDatabase(MixinApplication.appContext)
    val u = account.toUser()
    db.userDao().insertUpdate(u, db.appDao())
    Session.storeAccount(account)
    val receive =
        MixinApplication.appContext.defaultSharedPreferences
            .getInt(SettingConversationFragment.CONVERSATION_KEY, MessageSource.EVERYBODY.ordinal)
    if (account.receiveMessageSource == MessageSource.EVERYBODY.name &&
        receive != MessageSource.EVERYBODY.ordinal
    ) {
        MixinApplication.appContext.defaultSharedPreferences
            .putInt(SettingConversationFragment.CONVERSATION_KEY, MessageSource.EVERYBODY.ordinal)
    } else if (account.receiveMessageSource == MessageSource.CONTACTS.name &&
        receive != MessageSource.CONTACTS.ordinal
    ) {
        MixinApplication.appContext.defaultSharedPreferences
            .putInt(SettingConversationFragment.CONVERSATION_KEY, MessageSource.CONTACTS.ordinal)
    }

    val receiveGroup =
        MixinApplication.appContext.defaultSharedPreferences
            .getInt(SettingConversationFragment.CONVERSATION_GROUP_KEY, MessageSource.EVERYBODY.ordinal)
    if (account.acceptConversationSource == MessageSource.EVERYBODY.name &&
        receiveGroup != MessageSource.EVERYBODY.ordinal
    ) {
        MixinApplication.appContext.defaultSharedPreferences
            .putInt(SettingConversationFragment.CONVERSATION_GROUP_KEY, MessageSource.EVERYBODY.ordinal)
    } else if (account.acceptConversationSource == MessageSource.CONTACTS.name &&
        receiveGroup != MessageSource.CONTACTS.ordinal
    ) {
        MixinApplication.appContext.defaultSharedPreferences
            .putInt(SettingConversationFragment.CONVERSATION_GROUP_KEY, MessageSource.CONTACTS.ordinal)
    }

    val searchSource =
        MixinApplication.appContext.defaultSharedPreferences.getString(
            PhoneNumberSettingFragment.ACCEPT_SEARCH_KEY,
            SearchSource.EVERYBODY.name,
        )
    if (account.acceptSearchSource != searchSource) {
        if (SearchSource.EVERYBODY.name == account.acceptSearchSource) {
            MixinApplication.appContext.defaultSharedPreferences.putString(
                PhoneNumberSettingFragment.ACCEPT_SEARCH_KEY,
                SearchSource.EVERYBODY.name,
            )
        } else if (SearchSource.CONTACTS.name == account.acceptSearchSource) {
            MixinApplication.appContext.defaultSharedPreferences.putString(
                PhoneNumberSettingFragment.ACCEPT_SEARCH_KEY,
                SearchSource.CONTACTS.name,
            )
        } else if (SearchSource.NOBODY.name == account.acceptSearchSource) {
            MixinApplication.appContext.defaultSharedPreferences.putString(
                PhoneNumberSettingFragment.ACCEPT_SEARCH_KEY,
                SearchSource.NOBODY.name,
            )
        }
    }
}

// global LiveData to observe if Tip.checkCounter inside RefreshAccountJob is success
class TipCounterSyncedLiveData : LiveData<Boolean>() {
    var synced: Boolean = false
        set(value) {
            if (value == field) return

            field = value
            postValue(value)
        }
}
