package one.mixin.android.ui.setting.ui.page

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.compose.IndeterminateProgressDialog
import one.mixin.android.compose.MixinBackButton
import one.mixin.android.compose.MixinBottomSheetDialog
import one.mixin.android.compose.MixinTopAppBar
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.extension.openNotificationSetting
import one.mixin.android.extension.supportsOreo
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.setting.SettingViewModel
import one.mixin.android.util.ChannelManager
import one.mixin.android.vo.Fiats
import timber.log.Timber

@Composable
fun NotificationsPage() {
    Scaffold(
        topBar = {
            MixinTopAppBar(
                title = {
                    Text(text = stringResource(R.string.Notification_and_Confirmation))
                },
                navigationIcon = {
                    MixinBackButton()
                },
            )
        },
        backgroundColor = MixinAppTheme.colors.backgroundWindow,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(it)
                    .verticalScroll(rememberScrollState()),
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
                        PropertyHelper.findValueByKey(Constants.Account.PREF_DUPLICATE_TRANSFER, true)
                    strangerTransferChecked =
                        PropertyHelper.findValueByKey(Constants.Account.PREF_STRANGER_TRANSFER, true)
                }
            }

            NotificationItem(
                trailing = {
                    Switch(
                        checked = duplicateTransferSelected,
                        colors =
                            SwitchDefaults.colors(
                                checkedThumbColor = MixinAppTheme.colors.accent,
                                uncheckedThumbColor = MixinAppTheme.colors.unchecked,
                                checkedTrackColor = MixinAppTheme.colors.accent,
                                uncheckedTrackColor = MixinAppTheme.colors.unchecked,
                            ),
                        onCheckedChange = null,
                    )
                },
                onClick = {
                    scope.launch {
                        duplicateTransferSelected = !duplicateTransferSelected
                        PropertyHelper.updateKeyValue(
                            Constants.Account.PREF_DUPLICATE_TRANSFER,
                            duplicateTransferSelected,
                        )
                    }
                },
                title = stringResource(id = R.string.Duplicate_Transfer_Confirmation),
                description = stringResource(id = R.string.setting_duplicate_transfer_desc),
            )

            NotificationItem(
                trailing = {
                    Switch(
                        checked = strangerTransferChecked,
                        colors =
                            SwitchDefaults.colors(
                                checkedThumbColor = MixinAppTheme.colors.accent,
                                uncheckedThumbColor = MixinAppTheme.colors.unchecked,
                                checkedTrackColor = MixinAppTheme.colors.accent,
                                uncheckedTrackColor = MixinAppTheme.colors.unchecked,
                            ),
                        onCheckedChange = null,
                    )
                },
                onClick = {
                    scope.launch {
                        strangerTransferChecked = !strangerTransferChecked
                        PropertyHelper.updateKeyValue(
                            Constants.Account.PREF_STRANGER_TRANSFER,
                            strangerTransferChecked.toString(),
                        )
                    }
                },
                title = stringResource(id = R.string.Stranger_Transfer_Confirmation),
                description = stringResource(id = R.string.setting_stranger_transfer_desc),
            )

            val context = LocalContext.current

            NotificationItem(
                onClick = {
                    context.openNotificationSetting()
                },
                title = stringResource(id = R.string.System_options),
            )

            supportsOreo {
                NotificationItem(
                    onClick = {
                        ChannelManager.resetChannelSound(context)
                        toast(R.string.Successful)
                    },
                    title = stringResource(id = R.string.Reset_notifications),
                )
            }
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
            modifier =
                Modifier
                    .clickable {
                        onClick()
                    }
                    .height(60.dp)
                    .background(color = MixinAppTheme.colors.background)
                    .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp,
            )
            Spacer(Modifier.weight(1f))
            ProvideTextStyle(
                value =
                    TextStyle(
                        color = MixinAppTheme.colors.textSubtitle,
                        fontSize = 13.sp,
                    ),
            ) {
                trailing()
            }
        }
        if (description != null) {
            Text(
                modifier =
                    Modifier
                        .background(color = MixinAppTheme.colors.backgroundWindow)
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 8.dp, bottom = 16.dp, end = 16.dp),
                text = description,
                fontSize = 13.sp,
                color = MixinAppTheme.colors.textSubtitle,
            )
        }
    }
}

@Composable
private fun TransferNotificationItem() {
    val accountSymbol =
        remember {
            Fiats.getSymbol()
        }
    val threshold =
        remember {
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
        title = stringResource(R.string.Large_Amount_Confirmation),
        description = stringResource(R.string.setting_transfer_large_summary, "$accountSymbol${threshold.value}"),
    )

    if (showEditDialog) {
        MixinBottomSheetDialog({ showEditDialog = false }) {
            EditDialog(
                title = stringResource(R.string.Transfer_Amount_count_down, accountSymbol),
                hint = stringResource(R.string.Transfer_Amount),
                onClose = {
                    showEditDialog = false
                },
                text = threshold.value.toString(),
                onConfirm = {
                    Timber.d("onConfirm $it")
                    val result = it.toDoubleOrNull()
                    if (result == null) {
                        toast(R.string.Data_error)
                    } else {
                        showProgressDialog = true
                        scope.launch {
                            handleMixinResponse(
                                invokeNetwork = {
                                    viewModel.preferences(
                                        AccountUpdateRequest(
                                            fiatCurrency = Session.getFiatCurrency(),
                                            transferNotificationThreshold = result,
                                        ),
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
                                },
                            )
                        }
                    }
                },
            )
        }
    }

    if (showProgressDialog) {
        IndeterminateProgressDialog(
            title = stringResource(R.string.Large_Amount_Confirmation),
            message = stringResource(R.string.Please_wait_a_bit),
        )
    }
}

@Composable
private fun TransferLargeAmountItem() {
    val accountSymbol =
        remember {
            Fiats.getSymbol()
        }
    val threshold =
        remember {
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
        title = stringResource(R.string.Transfer_Notifications),
        description =
            if (threshold.value <= 0.0) {
                stringResource(
                    R.string.setting_transfer_large_summary_greater,
                    "${accountSymbol}0",
                )
            } else {
                stringResource(
                    R.string.setting_transfer_large_summary,
                    "$accountSymbol${threshold.value}",
                )
            },
    )

    if (showEditDialog) {
        MixinBottomSheetDialog({ showEditDialog = false }) {
            EditDialog(
                title = stringResource(R.string.Transfer_Amount_count_down, accountSymbol),
                hint = stringResource(R.string.Transfer_Amount),
                onClose = {
                    showEditDialog = false
                },
                text = threshold.value.toString(),
                onConfirm = {
                    Timber.d("onConfirm $it")
                    val result = it.toDoubleOrNull()
                    if (result == null) {
                        toast(R.string.Data_error)
                    } else {
                        showProgressDialog = true
                        scope.launch {
                            handleMixinResponse(
                                invokeNetwork = {
                                    viewModel.preferences(
                                        AccountUpdateRequest(
                                            fiatCurrency = Session.getFiatCurrency(),
                                            transferConfirmationThreshold = result,
                                        ),
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
                                },
                            )
                        }
                    }
                },
            )
        }
    }

    if (showProgressDialog) {
        IndeterminateProgressDialog(
            title = stringResource(R.string.Transfer_Notifications),
            message = stringResource(R.string.Please_wait_a_bit),
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
    val inputText =
        remember {
            mutableStateOf(
                TextFieldValue(
                    text = text,
                    selection = TextRange(text.length),
                ),
            )
        }
    Column(
        modifier =
            Modifier
                .background(
                    color = MixinAppTheme.colors.background,
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                )
                .padding(24.dp),
    ) {
        Text(
            text = title,
            color = MixinAppTheme.colors.textPrimary,
        )

        val focusRequester =
            remember {
                FocusRequester()
            }
        val keyboardController = LocalSoftwareKeyboardController.current

        val interactionSource = remember { MutableInteractionSource() }
        val hasFocus by remember {
            mutableStateOf(false)
        }

        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            scope.launch {
                // delay to request focus. make sure the compose is ready.
                delay(50)
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }

        BasicTextField(
            value = inputText.value,
            onValueChange = { inputText.value = it },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .focusRequester(focusRequester),
            interactionSource = interactionSource,
            keyboardOptions =
                KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Decimal,
                ),
            textStyle =
                TextStyle(
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textPrimary,
                ),
            cursorBrush = SolidColor(MixinAppTheme.colors.accent),
        ) { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                innerTextField()
                if (text.isEmpty()) {
                    Text(
                        text = hint,
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textSubtitle,
                    )
                }
            }
            Box(contentAlignment = Alignment.BottomCenter) {
                Box(
                    modifier =
                        Modifier
                            .height(1.5.dp)
                            .fillMaxWidth()
                            .background(color = if (hasFocus) MixinAppTheme.colors.accent else MixinAppTheme.colors.textSubtitle),
                )
            }
        }

        Row {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                keyboardController?.hide()
                onClose()
            }) {
                Text(
                    text = stringResource(R.string.Cancel),
                    style =
                        TextStyle(
                            fontSize = 14.sp,
                            color = MixinAppTheme.colors.accent,
                        ),
                )
            }
            TextButton(onClick = {
                keyboardController?.hide()
                onClose()
                onConfirm(inputText.value.text)
            }) {
                Text(
                    text = stringResource(R.string.Save),
                    style =
                        TextStyle(
                            fontSize = 14.sp,
                            color = MixinAppTheme.colors.accent,
                        ),
                )
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
            description = "Description",
        )
    }
}

@Composable
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
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
