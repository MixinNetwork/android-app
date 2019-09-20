package one.mixin.android.ui.setting

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import javax.inject.Inject
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.api.service.AccountService
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putInt
import one.mixin.android.vo.MessageSource

class SettingConversationViewModel @Inject
internal constructor(private val userService: AccountService) : ViewModel() {

    suspend fun savePreferences(request: AccountUpdateRequest) = userService.preferences(request)

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
