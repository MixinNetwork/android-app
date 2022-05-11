package one.mixin.android.ui.setting.ui.page

import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.session.Session
import one.mixin.android.ui.setting.SettingConversationViewModel
import one.mixin.android.ui.setting.ui.compose.SettingPageScaffold
import one.mixin.android.vo.SearchSource

@Composable
fun PhoneNumberSettingPage() {
    SettingPageScaffold(title = stringResource(id = R.string.setting_phone_number)) {
        val context = LocalContext.current

        MessageSettingTips(stringResource(id = R.string.setting_phone_number_privacy))

        val scope = rememberCoroutineScope()

        val viewModel = hiltViewModel<SettingConversationViewModel>()

        val preferences = remember {
            viewModel.initSearchPreference(context)
        }

        val selected by preferences.observeAsState()

        var tempSelect by remember {
            mutableStateOf<String?>(null)
        }

        val currentSelected = tempSelect ?: selected

        var processing by remember {
            mutableStateOf(false)
        }

        fun requestRemoteChange(source: SearchSource) {
            tempSelect = source.name
            processing = true
            scope.launch {
                handleMixinResponse(
                    invokeNetwork = {
                        viewModel.savePreferences(AccountUpdateRequest(acceptSearchSource = source.name))
                    },
                    successBlock = {
                        it.data?.let { account ->
                            Session.storeAccount(account)
                        }
                        when (source) {
                            SearchSource.EVERYBODY -> preferences.setEveryBody()
                            SearchSource.CONTACTS -> preferences.setContacts()
                            SearchSource.NOBODY -> preferences.setNobody()
                        }
                        tempSelect = null
                    },
                    failureBlock = {
                        processing = false
                        tempSelect = null
                        return@handleMixinResponse false
                    },
                    exceptionBlock = {
                        processing = false
                        tempSelect = null
                        return@handleMixinResponse false
                    },
                    doAfterNetworkSuccess = {
                        processing = false
                    },
                )
            }
        }

        MessageSettingItem(
            title = stringResource(R.string.setting_conversation_everybody),
            selected = currentSelected == SearchSource.EVERYBODY.name,
            processing = processing,
        ) {
            requestRemoteChange(SearchSource.EVERYBODY)
        }
        MessageSettingItem(
            title = stringResource(R.string.setting_my_contacts),
            selected = currentSelected == SearchSource.CONTACTS.name,
            processing = processing,
        ) {
            requestRemoteChange(SearchSource.CONTACTS)
        }

        MessageSettingItem(
            title = stringResource(R.string.nobody),
            selected = currentSelected == SearchSource.NOBODY.name,
            processing = processing,
        ) {
            requestRemoteChange(SearchSource.NOBODY)
        }

    }
}