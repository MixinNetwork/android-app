package one.mixin.android.ui.setting.ui.page

import android.Manifest
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
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
import ir.mirrajabi.rxcontacts.Contact
import ir.mirrajabi.rxcontacts.RxContacts
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.createContactsRequests
import one.mixin.android.extension.*
import one.mixin.android.ui.setting.SettingViewModel
import one.mixin.android.ui.setting.ui.compose.SettingPageScaffold
import one.mixin.android.ui.setting.ui.compose.SettingTile
import one.mixin.android.ui.setting.ui.compose.rememberComposeScope
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import timber.log.Timber

@Composable
fun MobileContactPage() {
    SettingPageScaffold(title = stringResource(R.string.setting_mobile_contact)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.height(32.dp))
            Image(
                painter = painterResource(id = R.drawable.ic_setting_mobile_contact),
                contentDescription = null
            )
            Box(modifier = Modifier.height(32.dp))
            Text(
                modifier = Modifier.padding(horizontal = 20.dp),
                text = stringResource(R.string.setting_mobile_contact_desc),
                color = MixinAppTheme.colors.textSubtitle,
                fontSize = 14.sp
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
                    modifier = Modifier
                        .size(24.dp)
                        .padding(horizontal = 4.dp),
                    color = MixinAppTheme.colors.accent,
                    strokeWidth = 2.dp
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
    onUploaded: () -> Unit
) {

    val context = LocalContext.current

    val scope = rememberComposeScope()
    val coroutineScope = rememberCoroutineScope()

    var processing by remember {
        mutableStateOf(false)
    }


    fun uploadContacts(contacts: List<Contact>) = coroutineScope.launch {

        val mutableList = createContactsRequests(contacts)

        if (mutableList.isEmpty()) {
            processing = false
            toast(R.string.setting_mobile_contact_empty)
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
        title = stringResource(R.string.setting_mobile_contact_upload),
        titleColor = MixinAppTheme.colors.accent,
        trailing = {
            if (processing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(4.dp),
                    color = MixinAppTheme.colors.accent,
                    strokeWidth = 2.dp,
                )
            }
        }
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
                        }, {
                            processing = false
                        }
                    )
            }
    }

}

@Composable
private fun DeleteButton(
    viewModel: SettingViewModel,
    onDeleted: () -> Unit
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
        title = stringResource(R.string.setting_mobile_contact_delete),
        titleColor = MixinAppTheme.colors.red,
        trailing = {
            if (deleting) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(4.dp),
                    color = MixinAppTheme.colors.accent,
                    strokeWidth = 2.dp,
                )
            }
        }
    ) {
        showAlert = true
    }

    fun deleteContacts() = scope.launch {
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
            }
        )
        deleting = false
    }


    if (showAlert) {
        AlertDialog(
            onDismissRequest = { showAlert = false },
            text = {
                Text(text = stringResource(id = R.string.setting_mobile_contact_warning))
            },
            confirmButton = {
                TextButton(onClick = {
                    showAlert = false
                    deleteContacts()
                }) {
                    Text(text = stringResource(id = R.string.conversation_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAlert = false
                }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }
}