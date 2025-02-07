package one.mixin.android.ui.address.page

import PageScaffold
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.R
import one.mixin.android.api.response.Web3Token
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.extension.openUrl
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.ui.address.AddressViewModel
import one.mixin.android.ui.address.component.DestinationMenu
import one.mixin.android.ui.address.component.TokenInfoHeader
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.Address
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.Constants.ChainId
import one.mixin.android.vo.WithdrawalMemoPossibility

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TransferDestinationInputPage(
    token: TokenItem?,
    web3Token: Web3Token?,
    web3Chain: Web3Token?,
    pop: (() -> Unit)?,
    onScan: (() -> Unit)? = null,
    contentText: String = "",
    toAddAddress: () -> Unit,
    toContact: () -> Unit,
    toWallet: () -> Unit,
    toAccount: (String) -> Unit,
    onSend: (String) -> Unit,
    onDeleteAddress: (Address) -> Unit,
    onAddressClick: (Address) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: AddressViewModel = hiltViewModel()
    val addresses by viewModel.addressesFlow(token?.assetId ?: "")
        .collectAsState(initial = emptyList())

    var account by remember { mutableStateOf("") }
    val memoEnabled = token?.withdrawalMemoPossibility == WithdrawalMemoPossibility.POSITIVE

    LaunchedEffect(token?.chainId) {
        account = when (token?.chainId) {
            ChainId.SOLANA_CHAIN_ID -> PropertyHelper.findValueByKey(
                Constants.Account.ChainAddress.SOLANA_ADDRESS,
                ""
            )

            ChainId.ETHEREUM_CHAIN_ID,
            ChainId.Base,
            ChainId.Polygon,
            ChainId.Arbitrum,
            ChainId.Optimism -> PropertyHelper.findValueByKey(EVM_ADDRESS, "")

            else -> ""
        }
    }

    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    var text by remember(contentText) { mutableStateOf(contentText) }

    MixinAppTheme {
        ModalBottomSheetLayout(
            sheetState = modalSheetState,
            scrimColor = Color.Black.copy(alpha = 0.3f),
            sheetBackgroundColor = Color.Transparent,
            sheetContent = {
                AddressSearchBottomSheet(
                    addresses = addresses,
                    modalSheetState = modalSheetState,
                    onAddClick = toAddAddress,
                    onDeleteAddress = onDeleteAddress,
                    onAddressClick = onAddressClick
                )
            }
        ) {
            PageScaffold(
                title = stringResource(R.string.Send),
                subTitle = if (memoEnabled) "1/3" else "1/2",
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
                        .verticalScroll(rememberScrollState())
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
                            value = text,
                            onValueChange = {
                                text = it
                            },
                            modifier = Modifier
                                .height(96.dp),
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
                            textStyle = TextStyle(
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

                        if (text.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    text = ""
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

                    if (text.isBlank()) {
                        Column {
                            if (token != null) {
                                DestinationMenu(
                                    R.drawable.ic_destination_contact,
                                    R.string.Mixin_Contact,
                                    R.string.Send_crypto_to_contact,
                                    onClick = {
                                        toContact.invoke()
                                    }, true
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            if (web3Token != null) {
                                DestinationMenu(
                                    R.drawable.ic_destination_wallet,
                                    R.string.Mixin_Wallet,
                                    R.string.Send_crypto_to_wallet,
                                    onClick = {
                                        toWallet.invoke()
                                    }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            if (account.isBlank().not()) {
                                DestinationMenu(
                                    R.drawable.ic_destination_wallet,
                                    R.string.Account,
                                    stringResource(
                                        R.string.Send_crypto_to_account,
                                        token?.chainName ?: ""
                                    ),
                                    onClick = {
                                        toAccount.invoke(account)
                                    })
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            DestinationMenu(
                                R.drawable.ic_destination_address,
                                R.string.Address_Book,
                                R.string.Send_crypto_to_address,
                                onClick = {
                                    scope.launch {
                                        modalSheetState.show()
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            onClick = {
                                onSend.invoke(text)
                            },
                            enabled = text.isBlank().not(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                backgroundColor = if (text.isBlank()
                                        .not()
                                ) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGrayLight,
                            ),
                            shape = RoundedCornerShape(32.dp),
                            elevation = ButtonDefaults.elevation(
                                pressedElevation = 0.dp,
                                defaultElevation = 0.dp,
                                hoveredElevation = 0.dp,
                                focusedElevation = 0.dp,
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.Send),
                                color = if (text.isNullOrBlank()) MixinAppTheme.colors.textAssist else Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}
