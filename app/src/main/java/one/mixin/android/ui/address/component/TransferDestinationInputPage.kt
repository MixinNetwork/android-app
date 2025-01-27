package one.mixin.android.ui.address.component

import PageScaffold
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.Web3Token
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.address.AddressViewModel
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.safe.TokenItem

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TransferDestinationInputPage(
    token: TokenItem?,
    web3Token: Web3Token?,
    web3Chain: Web3Token?,
    pop: (() -> Unit)?,
    onScan: (() -> Unit)? = null,
    contentText: String = "",
    onContentTextChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: AddressViewModel = hiltViewModel()
    val addresses by viewModel.addressesFlow(token?.assetId ?: "")
        .collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )

    MixinAppTheme {
        BackHandler(
            enabled = modalSheetState.isVisible
        ) {
            scope.launch {
                modalSheetState.hide()
            }
        }

        ModalBottomSheetLayout(
            sheetState = modalSheetState,
            scrimColor = Color.Black.copy(alpha = 0.3f),
            sheetBackgroundColor = Color.Transparent,
            sheetContent = {
                Spacer(modifier = Modifier.height(56.dp))
                AddressSearchBottomSheet(addresses = addresses, modalSheetState = modalSheetState)
            }
        ) {
            PageScaffold(
                title = stringResource(R.string.Send),
                verticalScrollable = false,
                pop = pop,
                actions = {
                    IconButton(onClick = {
                        context.openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_support),
                            contentDescription = null,
                            tint = MixinAppTheme.colors.icon,
                        )
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    TokenInfoHeader(token = token, web3Token = web3Token)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cardBackground(
                                Color.Transparent,
                                MixinAppTheme.colors.borderColor,
                                cornerRadius = 8.dp
                            ),
                    ) {
                        OutlinedTextField(
                            value = contentText,
                            onValueChange = onContentTextChange,
                            modifier = Modifier.height(96.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                backgroundColor = Color.Transparent,
                                textColor = MixinAppTheme.colors.textPrimary,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                cursorColor = MixinAppTheme.colors.textBlue
                            ),
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.hint_address),
                                    color = MixinAppTheme.colors.textAssist,
                                    fontSize = 14.sp
                                )
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 14.sp,
                                color = MixinAppTheme.colors.textPrimary,
                                textAlign = TextAlign.Start
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text, imeAction = ImeAction.Done
                            ),
                            minLines = 3,
                            maxLines = 3
                        )

                        if (contentText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    onContentTextChange("")
                                }, modifier = Modifier.align(Alignment.BottomEnd)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_addr_remove),
                                    contentDescription = null,
                                    tint = MixinAppTheme.colors.textPrimary
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    onScan?.invoke()
                                }, modifier = Modifier.align(Alignment.BottomEnd)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_addr_qr),
                                    contentDescription = null,
                                    tint = MixinAppTheme.colors.textPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (contentText.isBlank()) {
                        Column {
                            DestinationMenu(
                                R.drawable.ic_destination_contact,
                                R.string.Contact,
                                R.string.Send_crypto_to_contact,
                                onClick = {
                                    scope.launch {
                                        modalSheetState.show()
                                    }
                               })
                            Spacer(modifier = Modifier.height(16.dp))
                            DestinationMenu(
                                R.drawable.ic_destination_wallet,
                                R.string.Account,
                                R.string.Send_crypto_to_account,
                                onClick = { })
                            Spacer(modifier = Modifier.height(16.dp))
                            DestinationMenu(
                                R.drawable.ic_destination_address,
                                R.string.Address,
                                R.string.Send_crypto_to_address
                            ) {

                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

