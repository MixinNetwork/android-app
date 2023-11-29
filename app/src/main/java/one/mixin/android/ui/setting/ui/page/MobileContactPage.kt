package one.mixin.android.ui.setting.ui.page

import android.Manifest
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.createContactsRequests
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.findFragmentActivityOrNull
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.toast
import one.mixin.android.ui.setting.SettingViewModel
import one.mixin.android.ui.setting.ui.compose.MixinAlertDialog
import one.mixin.android.ui.setting.ui.compose.SettingPageScaffold
import one.mixin.android.ui.setting.ui.compose.SettingTile
import one.mixin.android.ui.setting.ui.compose.rememberComposeScope
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import one.mixin.android.util.rxcontact.Contact
import one.mixin.android.util.rxcontact.RxContacts
import timber.log.Timber

@Composable
fun MobileContactPage() {
    SettingPageScaffold(title = stringResource(R.string.Phone_Contact)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(modifier = Modifier.height(32.dp))
            Image(
                painter = painterResource(id = R.drawable.ic_setting_mobile_contact),
                contentDescription = null,
            )
            Box(modifier = Modifier.height(32.dp))
            Text(
                modifier = Modifier.padding(horizontal = 20.dp),
                text = stringResource(R.string.syncs_contact_hint),
                color = MixinAppTheme.colors.textSubtitle,
                fontSize = 14.sp,
            )
            Box(modifier = Modifier.height(32.dp))
        }

        var showUploadButton by remember {
            mutableStateOf(true)
        }

        var loading by remember {
            mutableStateOf(false)
        }

        val coroutineScope = rememberCoroutineScope()

        val viewModel = hiltViewModel<SettingViewModel>()

        LaunchedEffect(Unit) {
            coroutineScope.launch {
                loading = true
                handleMixinResponse(
                    invokeNetwork = { viewModel.getContacts() },
                    successBlock = { response ->
                        showUploadButton = response.data.isNullOrEmpty()
                    },
                )
                loading = false
            }
        }

        if (loading) {
            SettingTile(title = "", trailing = {
                CircularProgressIndicator(
                    modifier =
                        Modifier
                            .size(24.dp)
                            .padding(horizontal = 4.dp),
                    color = MixinAppTheme.colors.accent,
                    strokeWidth = 2.dp,
                )
            }) {
            }
        } else if (showUploadButton) {
            UploadButton(viewModel) {
                showUploadButton = false
            }
        } else {
            DeleteButton(viewModel) {
                showUploadButton = true
            }
        }
    }
}

@Composable
private fun UploadButton(
    viewModel: SettingViewModel,
    onUploaded: () -> Unit,
) {
    val context = LocalContext.current

    val scope = rememberComposeScope()
    val coroutineScope = rememberCoroutineScope()

    var processing by remember {
        mutableStateOf(false)
    }

    fun uploadContacts(contacts: List<Contact>) =
        coroutineScope.launch {
            val mutableList = createContactsRequests(contacts)

            if (mutableList.isEmpty()) {
                processing = false
                toast(R.string.Empty_address_book)
                return@launch
            }

            handleMixinResponse(
                invokeNetwork = { viewModel.syncContacts(mutableList) },
                successBlock = {
                    context.defaultSharedPreferences.putBoolean(Constants.Account.PREF_DELETE_MOBILE_CONTACTS, false)
                    onUploaded()
                },
            )
            processing = false
        }

    SettingTile(
        title = stringResource(R.string.Upload_Mobile_Contacts),
        titleColor = MixinAppTheme.colors.accent,
        trailing = {
            if (processing) {
                CircularProgressIndicator(
                    modifier =
                        Modifier
                            .size(24.dp)
                            .padding(4.dp),
                    color = MixinAppTheme.colors.accent,
                    strokeWidth = 2.dp,
                )
            }
        },
    ) {
        val activity = context.findFragmentActivityOrNull()
        if (activity == null) {
            Timber.e("MobileContactPage: activity is null")
            return@SettingTile
        }
        RxPermissions(activity)
            .request(Manifest.permission.READ_CONTACTS)
            .autoDispose(scope)
            .subscribe { granted ->
                if (!granted) {
                    context.openPermissionSetting()
                    return@subscribe
                }
                processing = true
                RxContacts.fetch(context)
                    .toSortedList(Contact::compareTo)
                    .autoDispose(scope)
                    .subscribe(
                        { contacts ->
                            uploadContacts(contacts)
                        },
                        {
                            processing = false
                        },
                    )
            }
    }
}

@Composable
private fun DeleteButton(
    viewModel: SettingViewModel,
    onDeleted: () -> Unit,
) {
    var showAlert by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var deleting by remember {
        mutableStateOf(false)
    }

    SettingTile(
        title = stringResource(R.string.Delete_Synced_Contact),
        titleColor = MixinAppTheme.colors.red,
        trailing = {
            if (deleting) {
                CircularProgressIndicator(
                    modifier =
                        Modifier
                            .size(24.dp)
                            .padding(4.dp),
                    color = MixinAppTheme.colors.accent,
                    strokeWidth = 2.dp,
                )
            }
        },
    ) {
        showAlert = true
    }

    fun deleteContacts() =
        scope.launch {
            deleting = true
            handleMixinResponse(
                invokeNetwork = { viewModel.deleteContacts() },
                successBlock = {
                    context.defaultSharedPreferences.putBoolean(Constants.Account.PREF_DELETE_MOBILE_CONTACTS, true)
                    onDeleted()
                },
                failureBlock = {
                    return@handleMixinResponse false
                },
                exceptionBlock = {
                    return@handleMixinResponse false
                },
                doAfterNetworkSuccess = {
                },
            )
            deleting = false
        }

    if (showAlert) {
        MixinAlertDialog(
            onDismissRequest = { showAlert = false },
            text = {
                Text(text = stringResource(id = R.string.setting_mobile_contact_warning))
            },
            confirmText = stringResource(id = R.string.Delete),
            onConfirmClick = {
                showAlert = false
                deleteContacts()
            },
            dismissText = stringResource(id = R.string.Cancel),
        )
    }
}
