package one.mixin.android.ui.address.component

import PageScaffold
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.request.ImageRequest
import coil3.request.transformations
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.Web3Token
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.address.AddressViewModel
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.util.getChainNetwork
import one.mixin.android.vo.Address
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.CoilRoundedHexagonTransformation

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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    Spacer(modifier = Modifier.height(56.dp))
                    AddressSearchBottomSheet(addresses = addresses, modalSheetState = modalSheetState)
                }
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
                            Item(
                                R.drawable.ic_destination_contact,
                                R.string.Contact,
                                R.string.Send_crypto_to_contact,
                                onClick = {
                                    scope.launch {
                                        modalSheetState.show()
                                    }
                               })
                            Spacer(modifier = Modifier.height(16.dp))
                            Item(
                                R.drawable.ic_destination_wallet,
                                R.string.Account,
                                R.string.Send_crypto_to_account,
                                onClick = { })
                            Spacer(modifier = Modifier.height(16.dp))
                            Item(
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

@Composable
private fun AddressSearchBottomSheet(
    addresses: List<Address>,
    modalSheetState: ModalBottomSheetState? = null
) {
    val context = LocalContext.current
    var searchText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val filteredAddresses = remember(searchText, addresses) {
        if (searchText.isBlank()) {
            addresses
        } else {
            addresses.filter { address ->
                address.label.contains(searchText, ignoreCase = true) ||
                    address.destination.contains(searchText, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MixinAppTheme.colors.background,
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
            )
            .padding(horizontal = 28.dp, vertical = 15.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
            ,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = MixinAppTheme.colors.backgroundWindow,
                    textColor = MixinAppTheme.colors.textPrimary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    cursorColor = MixinAppTheme.colors.textBlue
                ),
                placeholder = {
                    Text(
                        stringResource(R.string.Search),
                        color = MixinAppTheme.colors.textAssist,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.icon
                    )
                },
                trailingIcon = if (searchText.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { searchText = "" },
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = null,
                                tint = MixinAppTheme.colors.icon,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                } else null,
                shape = RoundedCornerShape(32.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textPrimary
                ),
                singleLine = true
            )

            if (searchText.isNotEmpty() || modalSheetState != null) {
                Text(
                    text = stringResource(R.string.Cancel),
                    color = MixinAppTheme.colors.textBlue,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clickable {
                            if (searchText.isNotEmpty()) {
                                searchText = ""
                            } else if (modalSheetState != null) {
                                scope.launch {
                                    modalSheetState.hide()
                                }
                            }
                        }
                )
            }
        }

        LazyColumn {
            items(filteredAddresses) { address ->
                AddressListItem(address = address)
            }
        }
    }
}

@Composable
private fun AddressListItem(address: Address) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(vertical = 8.dp)
    ) {
        HighlightedText(
            text = address.label,
            highlight = "",
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 16.sp,
                color = MixinAppTheme.colors.textPrimary
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        HighlightedText(
            text = address.destination,
            highlight = "",
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist
            )
        )
    }
}

@Composable
private fun TokenInfoHeader(
    token: TokenItem?,
    web3Token: Web3Token?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (token?.collectionHash != null) {
            CoilImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(token?.iconUrl ?: "")
                    .transformations(CoilRoundedHexagonTransformation()).build(),
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp)),
                placeholder = R.drawable.ic_inscription_icon,
            )
        } else {
            CoilImage(
                model = token?.iconUrl ?: web3Token?.iconUrl ?: "",
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape),
                placeholder = R.drawable.ic_avatar_place_holder,
            )
        }

        Text(
            text = token?.name ?: web3Token?.name ?: "",
            color = MixinAppTheme.colors.textPrimary,
            modifier = Modifier.padding(start = 4.dp)
        )

        token?.let { token ->
            val networkName =
                getChainNetwork(token.assetId, token.chainId, token.chainId)
            if (!networkName.isNullOrEmpty()) {
                Text(
                    text = networkName,
                    color = MixinAppTheme.colors.textAssist,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .background(
                            color = MixinAppTheme.colors.backgroundWindow,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Bal: ${token?.balance ?: web3Token?.balance} ${token?.symbol ?: web3Token?.symbol}",
            color = MixinAppTheme.colors.textPrimary
        )
    }
}

@Composable
fun Item(icon: Int, title: Int, subTile: Int, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .cardBackground(
                Color.Transparent, MixinAppTheme.colors.borderColor, cornerRadius = 8.dp
            )
            .padding(vertical = 13.dp, horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.padding(8.dp),
            painter = painterResource(icon),
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                stringResource(title), fontSize = 18.sp, color = MixinAppTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(subTile), fontSize = 13.sp, color = MixinAppTheme.colors.textAssist
            )
        }
    }
}

@Composable
private fun HighlightedText(
    text: String,
    highlight: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier
) {
    if (highlight.isBlank()) {
        Text(text = text, style = style, modifier = modifier)
        return
    }

    val annotatedString = buildAnnotatedString {
        var startIndex = 0
        var currentIndex = text.indexOf(highlight, ignoreCase = true)
        while (currentIndex >= 0) {
            append(text.substring(startIndex, currentIndex))
            withStyle(SpanStyle(color = MixinAppTheme.colors.textBlue)) {
                append(text.substring(currentIndex, currentIndex + highlight.length))
            }
            startIndex = currentIndex + highlight.length
            currentIndex = text.indexOf(highlight, startIndex, ignoreCase = true)
        }
        if (startIndex < text.length) {
            append(text.substring(startIndex))
        }
    }

    Text(
        text = annotatedString,
        style = style,
        modifier = modifier
    )
}

@Preview
@Composable
fun Input() {
    var contentText by rememberSaveable { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(
                Color.Transparent, MixinAppTheme.colors.borderColor, cornerRadius = 8.dp
            )
            .padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = contentText,
            onValueChange = { contentText = it },
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
                    stringResource(R.string.hint_address),
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
                onClick = {}) {
                Icon(
                    painter = painterResource(R.drawable.ic_addr_remove),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.textPrimary
                )
            }
        } else {
            IconButton(
                onClick = { }) {
                Icon(
                    painter = painterResource(R.drawable.ic_addr_qr),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.textPrimary
                )
            }
        }
    }
}