package one.mixin.android.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import one.mixin.android.MixinApplication
import one.mixin.android.api.service.AccountService
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putInt
import one.mixin.android.extension.putString
import one.mixin.android.repository.UserRepository
import one.mixin.android.session.Session
import one.mixin.android.ui.setting.PhoneNumberSettingFragment
import one.mixin.android.ui.setting.SettingConversationFragment
import one.mixin.android.vo.MessageSource
import one.mixin.android.vo.SearchSource
import one.mixin.android.vo.toUser

@HiltWorker
class RefreshAccountWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted parameters: WorkerParameters,
        private val accountService: AccountService,
        private val userRepo: UserRepository,
    ) : BaseWork(context, parameters) {
        override suspend fun onRun(): Result {
            val response = accountService.getMe().execute().body()
            if (response != null && response.isSuccess && response.data != null) {
                val account = response.data ?: return Result.failure()
                val u = account.toUser()
                userRepo.upsert(u)
                Session.storeAccount(account, 29)

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
                return Result.success()
            } else {
                return Result.failure()
            }
        }
    }
