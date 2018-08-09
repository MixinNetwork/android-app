package one.mixin.android.ui.setting

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.api.service.AccountService
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putInt
import one.mixin.android.vo.Account
import one.mixin.android.vo.MessageSource
import javax.inject.Inject

class SettingConversationViewModel @Inject
internal constructor(private val userService: AccountService) : ViewModel() {

    fun savePreferences(request: AccountUpdateRequest): Observable<MixinResponse<Account>> =
        userService.preferences(request).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())

    fun initPreferences(context: Context): MessageSourcePreferences {
        preferences = MessageSourcePreferences(context)
        return preferences
    }

    fun initGroupPreferences(context: Context): MessageGroupSourcePreferences {
        groupPreferences = MessageGroupSourcePreferences(context)
        return groupPreferences
    }

    lateinit var preferences: MessageSourcePreferences
    lateinit var groupPreferences: MessageGroupSourcePreferences

    class MessageSourcePreferences(val context: Context) : LiveData<Int>() {
        init {
            value = context.defaultSharedPreferences.getInt(SettingConversationFragment.CONVERSATION_KEY,
                MessageSource.EVERYBODY.ordinal)
        }

        fun setEveryBody() {
            value = MessageSource.EVERYBODY.ordinal
            context.defaultSharedPreferences.putInt(SettingConversationFragment.CONVERSATION_KEY,
                MessageSource.EVERYBODY.ordinal)
        }

        fun setContacts() {
            value = MessageSource.CONTACTS.ordinal
            context.defaultSharedPreferences.putInt(SettingConversationFragment.CONVERSATION_KEY,
                MessageSource.CONTACTS.ordinal)
        }
    }

    class MessageGroupSourcePreferences(val context: Context) : LiveData<Int>() {
        init {
            value = context.defaultSharedPreferences.getInt(SettingConversationFragment.CONVERSATION_GROUP_KEY,
                MessageSource.EVERYBODY.ordinal)
        }

        fun setEveryBody() {
            value = MessageSource.EVERYBODY.ordinal
            context.defaultSharedPreferences.putInt(SettingConversationFragment.CONVERSATION_GROUP_KEY,
                MessageSource.EVERYBODY.ordinal)
        }

        fun setContacts() {
            value = MessageSource.CONTACTS.ordinal
            context.defaultSharedPreferences.putInt(SettingConversationFragment.CONVERSATION_GROUP_KEY,
                MessageSource.CONTACTS.ordinal)
        }
    }
}