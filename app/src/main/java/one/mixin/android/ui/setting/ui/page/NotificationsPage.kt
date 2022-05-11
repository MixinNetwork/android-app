package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.extension.openNotificationSetting
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.setting.SettingViewModel
import one.mixin.android.ui.setting.ui.compose.IndeterminateProgressDialog
import one.mixin.android.ui.setting.ui.compose.MixinBackButton
import one.mixin.android.ui.setting.ui.compose.MixinBottomSheetDialog
import one.mixin.android.ui.setting.ui.compose.MixinTopAppBar
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import one.mixin.android.util.PropertyHelper
import one.mixin.android.vo.Fiats
import timber.log.Timber

@Composable
fun NotificationsPage() {
    Scaffold(
        topBar = {
            MixinTopAppBar(
                title = {
                    Text(text = stringResource(R.string.setting_notification_confirmation))
                }, navigationIcon = {
                    MixinBackButton()
                }
            )
        },
        backgroundColor = MixinAppTheme.colors.backgroundWindow,
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
        ) {
            TransferNotificationItem()
            TransferLargeAmountItem()

            val scope = rememberCoroutineScope()

            var duplicateTransferSelected by remember {
                mutableStateOf(true)
            }
            var strangerTransferChecked by remember {
                mutableStateOf(true)
            }

            LaunchedEffect(Unit) {
                scope.launch {
                    duplicateTransferSelected =
                        PropertyHelper.findValueByKey(Constants.Account.PREF_DUPLICATE_TRANSFER)?.toBoolean() ?: true
                    strangerTransferChecked =
                        PropertyHelper.findValueByKey(Constants.Account.PREF_STRANGER_TRANSFER)?.toBoolean() ?: true
                }
            }

            NotificationItem(
                trailing = {
                    Switch(
                        checked = duplicateTransferSelected,
                        onCheckedChange = null
                    )
                },
                onClick = {
                    scope.launch {
                        duplicateTransferSelected = !duplicateTransferSelected
                        PropertyHelper.updateKeyValue(
                            Constants.Account.PREF_DUPLICATE_TRANSFER,
                            duplicateTransferSelected.toString()
                        )
                    }
                },
                title = stringResource(id = R.string.transfer_duplicate_title),
                description = stringResource(id = R.string.setting_duplicate_transfer_desc)
            )

            NotificationItem(
                trailing = {
                    Switch(
                        checked = strangerTransferChecked,
                        onCheckedChange = null
                    )
                },
                onClick = {
                    scope.launch {
                        strangerTransferChecked = !strangerTransferChecked
                        PropertyHelper.updateKeyValue(
                            Constants.Account.PREF_STRANGER_TRANSFER,
                            strangerTransferChecked.toString()
                        )
                    }
                },
                title = stringResource(id = R.string.transfer_stranger_title),
                description = stringResource(id = R.string.setting_stranger_transfer_desc)
            )

            val context = LocalContext.current

            NotificationItem(
                onClick = {
                    context.openNotificationSetting()
                },
                title = stringResource(id = R.string.setting_notification_system),
            )
        }
    }
}

@Composable
private fun NotificationItem(
    trailing: @Composable () -> Unit = {},
    onClick: () -> Unit,
    title: String,
    description: String? = null,
) {
    Column {
        Row(
            modifier = Modifier
                .clickable {
                    onClick()
                }
                .height(60.dp)
                .background(color = MixinAppTheme.colors.background)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp
            )
            Spacer(Modifier.weight(1f))
            ProvideTextStyle(
                value = TextStyle(
                    color = MixinAppTheme.colors.textSubtitle,
                    fontSize = 13.sp,
                )
            ) {
                trailing()
            }
        }
        if (description != null) {
            Text(
                modifier = Modifier
                    .background(color = MixinAppTheme.colors.backgroundWindow)
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, bottom = 16.dp, end = 16.dp),
                text = description,
                fontSize = 13.sp,
                color = MixinAppTheme.colors.textSubtitle
            )
        }
    }
}

@Composable
private fun TransferNotificationItem() {
    val accountSymbol = remember {
        Fiats.getSymbol()
    }
    val threshold = remember {
        mutableStateOf(Session.getAccount()!!.transferNotificationThreshold)
    }

    var showEditDialog by remember {
        mutableStateOf(false)
    }

    var showProgressDialog by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()
    val viewModel = hiltViewModel<SettingViewModel>()

    NotificationItem(
        trailing = {
            Text("$accountSymbol${threshold.value}")
        },
        onClick = {
            showEditDialog = true
        },
        title = stringResource(R.string.setting_notification_transfer),
        description = stringResource(R.string.setting_notification_transfer_desc, "$accountSymbol${threshold.value}")
    )

    if (showEditDialog) {
        MixinBottomSheetDialog({ showEditDialog = false }) {
            EditDialog(
                title = stringResource(R.string.setting_notification_transfer_amount, accountSymbol),
                hint = stringResource(R.string.wallet_transfer_amount),
                onClose = {
                    showEditDialog = false
                },
                text = threshold.value.toString(),
                onConfirm = {
                    Timber.d("onConfirm $it")
                    val result = it.toDoubleOrNull()
                    if (result == null) {
                        toast(R.string.error_data)
                    } else {
                        showProgressDialog = true
                        scope.launch {
                            handleMixinResponse(
                                invokeNetwork = {
                                    viewModel.preferences(
                                        AccountUpdateRequest(
                                            fiatCurrency = Session.getFiatCurrency(),
                                            transferNotificationThreshold = result,
                                        )
                                    )
                                },
                                successBlock = { response ->
                                    response.data?.let { account ->
                                        Session.storeAccount(account)
                                        threshold.value = account.transferNotificationThreshold
                                    }
                                },
                                doAfterNetworkSuccess = {
                                    showProgressDialog = false
                                },
                                exceptionBlock = {
                                    showProgressDialog = false
                                    return@handleMixinResponse false
                                }
                            )
                        }
                    }
                }
            )
        }
    }

    if (showProgressDialog) {
        IndeterminateProgressDialog(
            title = stringResource(R.string.setting_notification_transfer),
            message = stringResource(R.string.pb_dialog_message),
        )
    }
}

@Composable
private fun TransferLargeAmountItem() {
    val accountSymbol = remember {
        Fiats.getSymbol()
    }
    val threshold = remember {
        mutableStateOf(Session.getAccount()!!.transferConfirmationThreshold)
    }

    var showEditDialog by remember {
        mutableStateOf(false)
    }

    var showProgressDialog by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()
    val viewModel = hiltViewModel<SettingViewModel>()

    NotificationItem(
        trailing = {
            Text("$accountSymbol${threshold.value}")
        },
        onClick = {
            showEditDialog = true
        },
        title = stringResource(R.string.setting_notification_transfer),
        description = if (threshold.value <= 0.0)
            stringResource(R.string.setting_transfer_large_summary_greater)
        else stringResource(
            R.string.setting_transfer_large_summary,
            "$accountSymbol${threshold.value}"
        )
    )

    if (showEditDialog) {
        MixinBottomSheetDialog({ showEditDialog = false }) {
            EditDialog(
                title = stringResource(R.string.wallet_transaction_tip_title_with_symbol, accountSymbol),
                hint = stringResource(R.string.wallet_transaction_tip_title),
                onClose = {
                    showEditDialog = false
                },
                text = threshold.value.toString(),
                onConfirm = {
                    Timber.d("onConfirm $it")
                    val result = it.toDoubleOrNull()
                    if (result == null) {
                        toast(R.string.error_data)
                    } else {
                        showProgressDialog = true
                        scope.launch {
                            handleMixinResponse(
                                invokeNetwork = {
                                    viewModel.preferences(
                                        AccountUpdateRequest(
                                            fiatCurrency = Session.getFiatCurrency(),
                                            transferConfirmationThreshold = result
                                        )
                                    )
                                },
                                successBlock = { response ->
                                    response.data?.let { account ->
                                        Session.storeAccount(account)
                                        threshold.value = account.transferConfirmationThreshold
                                    }
                                },
                                doAfterNetworkSuccess = {
                                    showProgressDialog = false
                                },
                                exceptionBlock = {
                                    showProgressDialog = false
                                    return@handleMixinResponse false
                                }
                            )
                        }
                    }
                }
            )
        }
    }

    if (showProgressDialog) {
        IndeterminateProgressDialog(
            title = stringResource(R.string.wallet_transaction_tip_title),
            message = stringResource(R.string.pb_dialog_message),
        )
    }
}

@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun EditDialog(
    onClose: () -> Unit,
    title: String,
    hint: String,
    text: String = "",
    onConfirm: (String) -> Unit = {},
) {
    val inputText = remember {
        mutableStateOf(text)
    }
    Column(
        modifier = Modifier
            .background(
                color = MixinAppTheme.colors.background,
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
            )
            .padding(24.dp)
    ) {
        Text(text = title)

        val focusRequester = remember {
            FocusRequester()
        }
        val keyboardController = LocalSoftwareKeyboardController.current

        val interactionSource = remember { MutableInteractionSource() }
        var hasFocus by remember {
            mutableStateOf(false)
        }
        BasicTextField(
            value = inputText.value,
            onValueChange = { inputText.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (it.isFocused) {
                        keyboardController?.show()
                    }
                    hasFocus = it.isFocused
                },
            interactionSource = interactionSource,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Decimal,
            ),
            textStyle = TextStyle(
                fontSize = 14.sp,
            ),
            cursorBrush = SolidColor(MixinAppTheme.colors.accent),
        ) { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                innerTextField()
                if (text.isEmpty())
                    Text(
                        text = hint,
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textSubtitle,
                    )
            }
            Box(contentAlignment = Alignment.BottomCenter) {
                Box(
                    modifier = Modifier
                        .height(1.5.dp)
                        .fillMaxWidth()
                        .background(color = if (hasFocus) MixinAppTheme.colors.accent else MixinAppTheme.colors.textSubtitle)
                )
            }
        }

        DisposableEffect(Unit) {
            focusRequester.requestFocus()
            onDispose { }
        }

        Row {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                onClose()
            }) {
                Text(text = stringResource(R.string.action_cancel))
            }
            TextButton(onClick = {
                onClose()
                onConfirm(inputText.value)
            }) {
                Text(text = stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
@Preview
fun NotificationItemPreview() {
    MixinAppTheme {
        NotificationItem(
            trailing = {
                Text("Preview")
            },
            onClick = {},
            title = "Title",
            description = "Description"
        )
    }
}

@Composable
@Preview
fun EditDialogPreview() {
    MixinAppTheme {
        EditDialog(
            onClose = {},
            title = "Title",
            hint = "Hint",
            text = "Text",
        )
    }
}

@Composable
@Preview
fun EditDialogEmptyPreview() {
    MixinAppTheme {
        EditDialog(
            onClose = {},
            title = "Title",
            hint = "Hint",
            text = "",
        )
    }
}

@Composable
@Preview
fun EditDialogLongTextPreview() {
    MixinAppTheme {
        EditDialog(
            onClose = {},
            title = "Title",
            hint = "Hint",
            text = "TextTextTextTextTextTextTextTextTextTextTextTextTextTextText",
        )
    }
}
