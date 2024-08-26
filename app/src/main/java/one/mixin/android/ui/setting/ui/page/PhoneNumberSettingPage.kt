package one.mixin.android.ui.setting.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.compose.SettingPageScaffold
import one.mixin.android.session.Session
import one.mixin.android.ui.setting.SettingConversationViewModel
import one.mixin.android.vo.SearchSource

@Composable
fun PhoneNumberSettingPage() {
    SettingPageScaffold(title = stringResource(id = R.string.Phone_Number)) {
        val context = LocalContext.current

        MessageSettingTips(stringResource(id = R.string.phone_number_privacy))

        val scope = rememberCoroutineScope()

        val viewModel = hiltViewModel<SettingConversationViewModel>()

        val preferences =
            remember {
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
                            Session.storeAccount(account, 23)
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
            title = stringResource(R.string.Everybody),
            selected = currentSelected == SearchSource.EVERYBODY.name,
            processing = processing,
        ) {
            requestRemoteChange(SearchSource.EVERYBODY)
        }
        MessageSettingItem(
            title = stringResource(R.string.My_Contacts),
            selected = currentSelected == SearchSource.CONTACTS.name,
            processing = processing,
        ) {
            requestRemoteChange(SearchSource.CONTACTS)
        }

        MessageSettingItem(
            title = stringResource(R.string.Nobody),
            selected = currentSelected == SearchSource.NOBODY.name,
            processing = processing,
        ) {
            requestRemoteChange(SearchSource.NOBODY)
        }
    }
}
