package one.mixin.android.ui.setting

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.api.service.AccountService
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putInt
import one.mixin.android.extension.putString
import one.mixin.android.ui.setting.PhoneNumberSettingFragment.Companion.ACCEPT_SEARCH_KEY
import one.mixin.android.vo.MessageSource
import one.mixin.android.vo.SearchSource
import javax.inject.Inject

@HiltViewModel
class SettingConversationViewModel
@Inject
internal constructor(private val userService: AccountService) : ViewModel() {

    suspend fun savePreferences(request: AccountUpdateRequest) = withContext(Dispatchers.IO) {
        userService.preferences(request)
    }

    fun initPreferences(context: Context): MessageSourcePreferences {
        preferences = MessageSourcePreferences(context)
        return preferences
    }

    fun initGroupPreferences(context: Context): MessageGroupSourcePreferences {
        groupPreferences = MessageGroupSourcePreferences(context)
        return groupPreferences
    }

    fun initSearchPreference(context: Context): SearchSourcePreferences {
        searchPreference = SearchSourcePreferences(context)
        return searchPreference
    }

    lateinit var preferences: MessageSourcePreferences
    lateinit var groupPreferences: MessageGroupSourcePreferences

    abstract class BaseMessageSourcePreferences : LiveData<Int>() {
        abstract fun setEveryBody()
        abstract fun setContacts()
    }

    class MessageSourcePreferences(val context: Context) : BaseMessageSourcePreferences() {
        init {
            value = context.defaultSharedPreferences.getInt(
                SettingConversationFragment.CONVERSATION_KEY,
                MessageSource.EVERYBODY.ordinal,
            )
        }

        override fun setEveryBody() {
            value = MessageSource.EVERYBODY.ordinal
            context.defaultSharedPreferences.putInt(
                SettingConversationFragment.CONVERSATION_KEY,
                MessageSource.EVERYBODY.ordinal,
            )
        }

        override fun setContacts() {
            value = MessageSource.CONTACTS.ordinal
            context.defaultSharedPreferences.putInt(
                SettingConversationFragment.CONVERSATION_KEY,
                MessageSource.CONTACTS.ordinal,
            )
        }
    }

    class MessageGroupSourcePreferences(val context: Context) : BaseMessageSourcePreferences() {
        init {
            value = context.defaultSharedPreferences.getInt(
                SettingConversationFragment.CONVERSATION_GROUP_KEY,
                MessageSource.EVERYBODY.ordinal,
            )
        }

        override fun setEveryBody() {
            value = MessageSource.EVERYBODY.ordinal
            context.defaultSharedPreferences.putInt(
                SettingConversationFragment.CONVERSATION_GROUP_KEY,
                MessageSource.EVERYBODY.ordinal,
            )
        }

        override fun setContacts() {
            value = MessageSource.CONTACTS.ordinal
            context.defaultSharedPreferences.putInt(
                SettingConversationFragment.CONVERSATION_GROUP_KEY,
                MessageSource.CONTACTS.ordinal,
            )
        }
    }

    lateinit var searchPreference: SearchSourcePreferences

    class SearchSourcePreferences(val context: Context) : LiveData<String>() {
        init {
            value = context.defaultSharedPreferences.getString(
                ACCEPT_SEARCH_KEY,
                SearchSource.EVERYBODY.name,
            )
        }

        fun setEveryBody() {
            value = SearchSource.EVERYBODY.name
            context.defaultSharedPreferences.putString(
                ACCEPT_SEARCH_KEY,
                SearchSource.EVERYBODY.name,
            )
        }

        fun setContacts() {
            value = SearchSource.CONTACTS.name
            context.defaultSharedPreferences.putString(
                ACCEPT_SEARCH_KEY,
                SearchSource.CONTACTS.name,
            )
        }

        fun setNobody() {
            value = SearchSource.NOBODY.name
            context.defaultSharedPreferences.putString(
                ACCEPT_SEARCH_KEY,
                SearchSource.NOBODY.name,
            )
        }
    }
}
