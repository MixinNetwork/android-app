package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.compose.SettingPageScaffold
import one.mixin.android.compose.SettingTile
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.session.Session
import one.mixin.android.ui.setting.SettingConversationViewModel
import one.mixin.android.vo.Account
import one.mixin.android.vo.MessageSource

@Composable
fun ConversationSettingPage() {
    val viewModel = hiltViewModel<SettingConversationViewModel>()

    SettingPageScaffold(title = stringResource(id = R.string.Conversation)) {
        val context = LocalContext.current

        MessageSettingTips(stringResource(id = R.string.setting_conversation_tip))
        SettingGroup(
            initMessageSourcePreferences = { viewModel.initPreferences(context) },
            doUpdate = {
                viewModel.savePreferences(AccountUpdateRequest(receiveMessageSource = it.name))
            },
        )

        MessageSettingTips(stringResource(id = R.string.setting_conversation_group_tip))
        SettingGroup(
            initMessageSourcePreferences = { viewModel.initGroupPreferences(context) },
            doUpdate = {
                viewModel.savePreferences(AccountUpdateRequest(acceptConversationSource = it.name))
            },
        )
    }
}

@Composable
private fun SettingGroup(
    initMessageSourcePreferences: () -> SettingConversationViewModel.BaseMessageSourcePreferences,
    doUpdate: suspend (source: MessageSource) -> MixinResponse<Account>,
) {
    val scope = rememberCoroutineScope()

    val preferences =
        remember {
            initMessageSourcePreferences()
        }

    val selected by preferences.observeAsState()

    var tempSelect by remember {
        mutableStateOf<Int?>(null)
    }

    val currentSelected = tempSelect ?: selected

    var processing by remember {
        mutableStateOf(false)
    }

    fun requestRemoteChange(source: MessageSource) {
        tempSelect = source.ordinal
        processing = true
        scope.launch {
            handleMixinResponse(
                invokeNetwork = {
                    doUpdate(source)
                },
                successBlock = {
                    it.data?.let { account ->
                        Session.storeAccount(account, 19)
                    }
                    when (source) {
                        MessageSource.EVERYBODY -> preferences.setEveryBody()
                        MessageSource.CONTACTS -> preferences.setContacts()
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
        selected = currentSelected == MessageSource.EVERYBODY.ordinal,
        processing = processing,
    ) {
        requestRemoteChange(MessageSource.EVERYBODY)
    }
    MessageSettingItem(
        title = stringResource(R.string.My_Contacts),
        selected = currentSelected == MessageSource.CONTACTS.ordinal,
        processing = processing,
    ) {
        requestRemoteChange(MessageSource.CONTACTS)
    }
}

@Composable
fun MessageSettingItem(
    title: String,
    selected: Boolean,
    processing: Boolean,
    onSelect: () -> Unit,
) {
    SettingTile(
        title = title,
        trailing = {
            if (selected) {
                if (!processing) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check_black_24dp),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.icon,
                    )
                } else {
                    CircularProgressIndicator(
                        modifier =
                            Modifier
                                .size(24.dp)
                                .padding(4.dp),
                        color = MixinAppTheme.colors.accent,
                        strokeWidth = 2.dp,
                    )
                }
            }
        },
        onClick = {
            if (!selected) {
                onSelect()
            }
        },
    )
}

@Composable
fun MessageSettingTips(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        color = MixinAppTheme.colors.textAssist,
        modifier =
            Modifier.padding(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 8.dp,
            ),
    )
}

@Composable
@Preview
fun PreviewConversationSettingPage() {
    MixinAppTheme {
        SettingPageScaffold(title = stringResource(id = R.string.Conversation)) {
            MessageSettingTips(stringResource(id = R.string.setting_conversation_tip))
            MessageSettingItem(
                title = stringResource(R.string.Everybody),
                selected = false,
                processing = false,
            ) {
            }
            MessageSettingItem(
                title = stringResource(R.string.Everybody),
                selected = true,
                processing = true,
            ) {
            }
            MessageSettingItem(
                title = stringResource(R.string.Everybody),
                selected = true,
                processing = false,
            ) {
            }
        }
    }
}
