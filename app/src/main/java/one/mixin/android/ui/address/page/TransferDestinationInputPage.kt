package one.mixin.android.ui.address.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import one.mixin.android.ui.home.web3.components.PageScaffold
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.vo.Address
import one.mixin.android.vo.WalletCategory
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.components.MixinButton

@Composable
fun TransferDestinationInputPage(
    token: TokenItem?,
    web3Token: Web3TokenItem?,
    name: String?,
    addressShown: Boolean,
    isLoading: Boolean = false,
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
    onShowAddressBook: () -> Unit,
) {
    val context = LocalContext.current
    val localLocalSoftwareKeyboardController = LocalSoftwareKeyboardController.current
    val viewModel: AddressViewModel = hiltViewModel()

    var walletDisplayName by remember { mutableStateOf<String?>(null) }
    var hasSafeWallet by remember { mutableStateOf(false) }
    var safeWalletChainId by remember { mutableStateOf<String?>(null) }
    var text by remember(contentText) { mutableStateOf(contentText) }
    val clipboardManager = LocalClipboard.current

    LaunchedEffect(web3Token?.walletId) {
        if (web3Token?.walletId != null) {
            viewModel.findWeb3WalletById(web3Token.walletId)?.let {
                if (it.category == WalletCategory.CLASSIC.value ||
                    it.category == WalletCategory.IMPORTED_MNEMONIC.value ||
                    it.category == WalletCategory.IMPORTED_PRIVATE_KEY.value ||
                    it.category == WalletCategory.WATCH_ADDRESS.value
                ) {
                    walletDisplayName = it.name
                }
            }
        }
    }

    LaunchedEffect(token, web3Token) {
        val chainId = token?.chainId ?: web3Token?.chainId ?: return@LaunchedEffect
        val safeWallets = viewModel.getSafeWalletsByChainId(chainId)
        hasSafeWallet = safeWallets.isNotEmpty()
        safeWalletChainId = safeWallets.firstOrNull()?.safeChainId
    }

    LaunchedEffect(addressShown) {
        if (addressShown) {
            onShowAddressBook()
        }
    }

    PageScaffold(
        title = stringResource(R.string.Send),
        subtitle = {
            val subtitleText = when {
                name != null -> name
                web3Token != null -> walletDisplayName ?: stringResource(R.string.Common_Wallet)
                else -> stringResource(R.string.Privacy_Wallet)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = subtitleText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MixinAppTheme.colors.textAssist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (name == null && web3Token == null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_wallet_privacy),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        },
        verticalScrollable = false,
        pop = pop,
        actions = {
            IconButton(onClick = {
                context.openUrl(
                    Constants.HelpLink.CUSTOMER_SERVICE,
                    source = AnalyticsTracker.CustomerServiceSource.SEND_RECIPIENT,
                )
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
                .imePadding(),
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
                            if (inputText.length <= 12) {
                                return@VisualTransformation TransformedText(input, OffsetMapping.Identity)
                            }

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
                        },
                        modifier = Modifier.align(Alignment.BottomEnd)
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
                                onShowAddressBook()
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
                            },
                            true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        DestinationMenu(
                            R.drawable.ic_destination_contact,
                            R.string.Mixin_Contact,
                            R.string.send_to_mixin_contact_description,
                            onClick = {
                                toContact.invoke()
                            },
                            true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    if (web3Token != null) {
                        DestinationMenu(
                            R.drawable.ic_destination_wallet,
                            R.string.My_Wallet,
                            stringResource(R.string.send_to_my_wallet_description),
                            free = true,
                            onClick = {
                                toWallet.invoke(web3Token.walletId)
                            },
                            isPrivacy = false
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    } else if (hasSafeWallet && safeWalletChainId != null) {
                        DestinationMenu(
                            R.drawable.ic_destination_wallet,
                            R.string.My_Wallet,
                            stringResource(R.string.send_to_my_wallet_description),
                            free = true,
                            onClick = {
                                toWallet.invoke(null)
                            },
                            isPrivacy = false
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    } else if (
                        token?.chainId == ChainId.SOLANA_CHAIN_ID ||
                        token?.chainId == ChainId.BITCOIN_CHAIN_ID ||
                        token?.chainId in Constants.Web3EvmChainIds
                    ) {
                        DestinationMenu(
                            R.drawable.ic_destination_wallet,
                            stringResource(R.string.My_Wallet),
                            stringResource(R.string.send_to_my_wallet_description),
                            onClick = {
                                toWallet.invoke(null)
                            },
                            free = true
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
                MixinButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    onClick = {
                        onSend.invoke(text)
                    },
                    enabled = text.isBlank().not() && !isLoading,
                    shape = RoundedCornerShape(32.dp),
                    backgroundColor = MixinAppTheme.colors.accent,
                    disabledBackgroundColor = if (text.isBlank()) {
                        MixinAppTheme.colors.backgroundGrayLight
                    } else {
                        MixinAppTheme.colors.accent.copy(alpha = 0.6f)
                    },
                    disabledContentColor = MixinAppTheme.colors.textAssist,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            fontSize = 16.sp,
                            text = stringResource(R.string.Send),
                            color = if (text.isBlank()) MixinAppTheme.colors.textAssist else Color.White,
                        )
                    }
                }
            }
        }
    }
}
