package one.mixin.android.ui.address.page

import PageScaffold
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.ChainId
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.isExternalTransferUrl
import one.mixin.android.extension.isLightningUrl
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.address.AddressViewModel
import one.mixin.android.ui.address.component.DestinationMenu
import one.mixin.android.ui.address.component.TokenInfoHeader
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.ui.wallet.components.PREF_NAME
import one.mixin.android.vo.Address
import one.mixin.android.vo.WalletCategory
import one.mixin.android.vo.WithdrawalMemoPossibility
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.web3.js.JsSigner

@Composable
fun TransferDestinationInputPage(
    token: TokenItem?,
    web3Token: Web3TokenItem?,
    name: String?,
    addressShown: Boolean,
    pop: (() -> Unit)?,
    onScan: (() -> Unit)? = null,
    contentText: String = "",
    errorInfo: String? = null,
    toAddAddress: () -> Unit,
    toContact: () -> Unit,
    toWallet: (String?) -> Unit,
    onSend: (String) -> Unit,
    onDeleteAddress: (Address) -> Unit,
    onAddressClick: (Address) -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) }
    val localLocalSoftwareKeyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val viewModel: AddressViewModel = hiltViewModel()

    val addresses by viewModel.addressesFlow(token?.chainId ?: web3Token?.chainId ?: "")
        .collectAsState(initial = emptyList())

    var account by remember { mutableStateOf("") }
    val memoEnabled = token?.withdrawalMemoPossibility == WithdrawalMemoPossibility.POSITIVE
    var walletDisplayName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(web3Token?.walletId) {
        if (web3Token?.walletId != null) {
            viewModel.findWeb3WalletById(web3Token.walletId)?.let {
                if (it.category == WalletCategory.CLASSIC.value ||
                    it.category == WalletCategory.IMPORTED_MNEMONIC.value ||
                    it.category == WalletCategory.IMPORTED_PRIVATE_KEY.value ||
                    it.category == WalletCategory.WATCH_ADDRESS.value) {
                    walletDisplayName = it.name
                }
            }
        }
    }

    LaunchedEffect(token?.chainId) {
        account = when {
            token?.chainId == ChainId.SOLANA_CHAIN_ID -> JsSigner.solanaAddress
            token?.chainId in Constants.Web3ChainIds -> JsSigner.evmAddress
            else -> ""
        }
    }

    val modalSheetState = rememberModalBottomSheetState(
        initialValue = if (addressShown) ModalBottomSheetValue.Expanded else ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    var text by remember(contentText) { mutableStateOf(contentText) }
    val clipboardManager = LocalClipboard.current

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
                subtitle = {
                    val subtitleText = when {
                        name != null -> name
                        web3Token != null -> walletDisplayName ?: stringResource(R.string.Common_Wallet)
                        else -> stringResource(R.string.Privacy_Wallet)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (name == null && web3Token == null) { // Privacy Wallet
                            Icon(
                                painter = painterResource(id = R.drawable.ic_wallet_privacy),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = subtitleText,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MixinAppTheme.colors.textAssist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
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
                                .wrapContentHeight()
                                .heightIn(min = 120.dp),
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
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            },
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = MixinAppTheme.colors.textPrimary,
                                textAlign = TextAlign.Start
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text, imeAction = ImeAction.Done
                            ),
                            minLines = 3,
                            maxLines = 20,
                            visualTransformation = if (text.isExternalTransferUrl() || text.isLightningUrl()) {
                                VisualTransformation { input ->
                                    val inputText = input.text
                                    if (inputText.length <= 12) return@VisualTransformation TransformedText(
                                        input,
                                        OffsetMapping.Identity
                                    )

                                    val annotatedString = buildAnnotatedString {
                                        append(inputText)
                                        addStyle(
                                            style = SpanStyle(fontWeight = FontWeight.ExtraBold),
                                            start = 0,
                                            end = 6.coerceAtMost(inputText.length)
                                        )
                                        addStyle(
                                            style = SpanStyle(fontWeight = FontWeight.ExtraBold),
                                            start = (inputText.length - 6).coerceAtLeast(0),
                                            end = inputText.length
                                        )
                                    }
                                    TransformedText(annotatedString, OffsetMapping.Identity)
                                }
                            } else {
                                VisualTransformation.None
                            }
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
                                    tint = MixinAppTheme.colors.iconGray
                                )
                            }
                        } else {
                            Row(modifier = Modifier.align(Alignment.BottomEnd)) {
                                IconButton(
                                    onClick = {
                                        clipboardManager.nativeClipboard.primaryClip?.getItemAt(0)?.text?.let {
                                            text = it.toString()
                                        }
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_paste),
                                        contentDescription = null,
                                        tint = MixinAppTheme.colors.iconGray
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                IconButton(
                                    onClick = {
                                        onScan?.invoke()
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_scan),
                                        contentDescription = null,
                                        tint = MixinAppTheme.colors.iconGray
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (text.isBlank()) {
                        Column {
                            if (token != null || web3Token?.assetId != null) {
                                DestinationMenu(
                                    R.drawable.ic_destination_address,
                                    R.string.Address_Book,
                                    R.string.send_to_address_description,
                                    onClick = {
                                        localLocalSoftwareKeyboardController?.hide()
                                        scope.launch {
                                            modalSheetState.show()
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            if (token != null) {
                                DestinationMenu(
                                    R.drawable.ic_destination_contact,
                                    R.string.Mixin_Contact,
                                    R.string.send_to_contact_description,
                                    onClick = {
                                        toContact.invoke()
                                    }, true
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            if (web3Token != null) {
                                DestinationMenu(
                                    R.drawable.ic_destination_wallet,
                                    R.string.My_Wallet,
                                    stringResource(R.string.send_to_my_wallet_description),
                                    onClick = {
                                        toWallet.invoke(web3Token.walletId)
                                    },
                                    isPrivacy = false
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            } else if (account.isNotBlank()) {
                                DestinationMenu(
                                    R.drawable.ic_destination_wallet,
                                    stringResource(R.string.My_Wallet),
                                    stringResource(R.string.send_to_my_wallet_description),
                                    onClick = {
                                        toWallet.invoke(null)
                                    },
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    } else {
                        Text(
                            text = errorInfo ?: "",
                            color = MixinAppTheme.colors.red,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .align(Alignment.CenterHorizontally)
                                .alpha(if (errorInfo.isNullOrBlank()) 0f else 1f)
                        )
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
                                color = if (text.isBlank()) MixinAppTheme.colors.textAssist else Color.White,
                            )
                        }
                    }
                }
            }
        }
    }

