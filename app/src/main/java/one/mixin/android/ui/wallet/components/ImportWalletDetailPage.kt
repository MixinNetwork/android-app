@file:Suppress("DEPRECATION")

package one.mixin.android.ui.wallet.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.crypto.UtxoKeyGenerator
import one.mixin.android.crypto.isEvmAddressValid
import one.mixin.android.crypto.isEvmPrivateKeyValid
import one.mixin.android.crypto.isSolanaAddressValid
import one.mixin.android.crypto.isSolanaHexPrivateKeyValid
import one.mixin.android.crypto.isSolanaPrivateKeyValid
import one.mixin.android.extension.findFragmentActivityOrNull
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.web3.components.PageScaffold
import one.mixin.android.ui.wallet.WalletSecurityActivity
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.ui.wallet.network.NetworkOption
import one.mixin.android.ui.wallet.network.NetworkSelectBottomSheetDialogFragment
import one.mixin.android.util.encodeToBase58String
import org.bitcoinj.base.BitcoinNetwork
import org.bitcoinj.crypto.DumpedPrivateKey
import org.bitcoinj.crypto.ECKey
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigInteger

@Composable
fun ImportWalletDetailPage(
    mode: WalletSecurityActivity.Mode,
    pop: () -> Unit,
    onConfirmClick: (String, String, String?) -> Unit,
    onScan: (() -> Unit)? = null,
    contentText: String = "",
    walletId: String? = null,
    chainId: String? = null,
    defaultWalletName: String? = null,
) {
    val context = LocalContext.current
    val textState = remember { TextFieldState(initialText = contentText) }
    LaunchedEffect(contentText) {
        if (textState.text.toString() != contentText) {
            textState.setTextAndPlaceCursorAtEnd(contentText)
        }
    }
    val text = textState.text.toString()
    val clipboardManager = LocalClipboardManager.current
    val walletViewModel: Web3ViewModel = hiltViewModel()

    val networks = mapOf(
        "Bitcoin" to Constants.ChainId.BITCOIN_CHAIN_ID,
        "Pearl" to Constants.ChainId.PEARL_CHAIN_ID,
        "Litecoin" to Constants.ChainId.Litecoin,
        "Ethereum" to Constants.ChainId.ETHEREUM_CHAIN_ID,
        "Base" to Constants.ChainId.Base,
        "BSC" to Constants.ChainId.BinanceSmartChain,
        "Polygon" to Constants.ChainId.Polygon,
        "Arbitrum" to Constants.ChainId.Arbitrum,
        "Optimism" to Constants.ChainId.Optimism,
        "Avalanche" to Constants.ChainId.Avalanche,
        "HyperEVM" to Constants.ChainId.HyperEVM,
        "X Layer" to Constants.ChainId.XLayer,
        "Robinhood" to Constants.ChainId.Robinhood,
        "Arc" to Constants.ChainId.Arc,
        "Solana" to Constants.ChainId.SOLANA_CHAIN_ID
    )
    val initialNetworkName = remember(chainId) {
        if (chainId == null) null
        else networks.entries.find { it.value == chainId }?.key
    }

    var selectedNetworkName by rememberSaveable(initialNetworkName) {
        mutableStateOf(initialNetworkName ?: "Ethereum")
    }
    val showWalletNameInput = mode == WalletSecurityActivity.Mode.IMPORT_PRIVATE_KEY ||
        mode == WalletSecurityActivity.Mode.ADD_WATCH_ADDRESS
    val walletNameState = rememberTextFieldState()
    val walletName = walletNameState.text.toString()
    var hasInitializedWalletName by rememberSaveable(showWalletNameInput) { mutableStateOf(false) }

    LaunchedEffect(defaultWalletName, showWalletNameInput) {
        if (showWalletNameInput && !hasInitializedWalletName && !defaultWalletName.isNullOrBlank()) {
            walletNameState.setTextAndPlaceCursorAtEnd(defaultWalletName.take(36))
            hasInitializedWalletName = true
        }
    }


    val isEvmNetwork = when (selectedNetworkName) {
        "Ethereum", "Base", "BSC", "Polygon", "Arbitrum", "Optimism", "Avalanche", "HyperEVM", "X Layer", "Robinhood", "Arc" -> true
        else -> false
    }
    val isBitcoin: Boolean = selectedNetworkName == "Bitcoin"
    val isPearl: Boolean = selectedNetworkName == "Pearl"
    val isLitecoin: Boolean = selectedNetworkName == "Litecoin"
    val isSolana = selectedNetworkName == "Solana"

    val currentChainId = networks[selectedNetworkName] ?: ""

    var addressExists by remember { mutableStateOf(false) }
    var addressToCheck by remember { mutableStateOf<String?>(null) }
    var isAddressMismatched by remember { mutableStateOf(false) }

    LaunchedEffect(text, currentChainId, mode, walletId) {
        if (text.isEmpty()) {
            addressExists = false
            addressToCheck = null
            isAddressMismatched = false
            return@LaunchedEffect
        }

        try {
            val address = when (mode) {
                WalletSecurityActivity.Mode.IMPORT_PRIVATE_KEY, WalletSecurityActivity.Mode.RE_IMPORT_PRIVATE_KEY -> {
                    if (isEvmNetwork && isEvmPrivateKeyValid(text)) {
                        CryptoWalletHelper.privateKeyToAddress(text, currentChainId)
                    } else if ((isBitcoin || isPearl) && isBitcoinPrivateKeyValid(text)) {
                        CryptoWalletHelper.privateKeyToAddress(text, currentChainId)
                    } else if (isLitecoin) {
                        CryptoWalletHelper.privateKeyToAddress(text, currentChainId)
                    } else if (isSolana && isSolanaPrivateKeyValid(text)) {
                        CryptoWalletHelper.privateKeyToAddress(text, currentChainId)
                    } else {
                        null
                    }
                }
                WalletSecurityActivity.Mode.ADD_WATCH_ADDRESS -> {
                    if ((isEvmNetwork && isEvmAddressValid(text)) ||
                        ((isBitcoin || isPearl || isLitecoin) && UtxoKeyGenerator.isAddressValid(text, currentChainId)) ||
                        (isSolana && isSolanaAddressValid(text))) {
                        text
                    } else {
                        null
                    }
                }
                else -> null
            }

            if (address != null && mode == WalletSecurityActivity.Mode.RE_IMPORT_PRIVATE_KEY) {
                val addressMatches = walletViewModel.isAddressMatch(walletId, address)
                isAddressMismatched = !addressMatches
                addressExists = false
            } else if (address != null && address != addressToCheck) {
                addressToCheck = address
                addressExists = walletViewModel.anyAddressExists(listOf(address))
            } else if (address == null) {
                addressExists = false
                addressToCheck = null
            }
        } catch (e: Exception) {
            Timber.e(e)
            addressExists = false
            addressToCheck = null
            isAddressMismatched = false
        }
    }

    val isInputValid = remember(mode, text, isEvmNetwork, isBitcoin, isPearl, isLitecoin, isSolana) {
        when (mode) {
            WalletSecurityActivity.Mode.IMPORT_PRIVATE_KEY,
            WalletSecurityActivity.Mode.RE_IMPORT_PRIVATE_KEY -> {
                if (isEvmNetwork) {
                    isEvmPrivateKeyValid(text)
                } else if (isBitcoin || isPearl) {
                    isBitcoinPrivateKeyValid(text)
                } else if (isLitecoin) {
                    runCatching { UtxoKeyGenerator.parseLitecoinPrivateKey(text) }.isSuccess
                } else if (isSolana) {
                    isSolanaPrivateKeyValid(text)
                } else {
                    false
                }
            }
            WalletSecurityActivity.Mode.ADD_WATCH_ADDRESS -> {
                if (isEvmNetwork) {
                    isEvmAddressValid(text)
                } else if (isBitcoin || isPearl || isLitecoin) {
                    UtxoKeyGenerator.isAddressValid(text, currentChainId)
                } else if(isSolana) {
                    isSolanaAddressValid(text)
                } else {
                    false
                }
            }
            else -> false
        }
    }

    val showInvalidError = text.isNotEmpty() && !isInputValid

    val showReimportMismatchError = mode == WalletSecurityActivity.Mode.RE_IMPORT_PRIVATE_KEY && text.isNotEmpty() && isInputValid && isAddressMismatched
    val isWalletNameValid = !showWalletNameInput || walletName.trim().isNotEmpty()

    val isButtonEnabled = when (mode) {
        WalletSecurityActivity.Mode.RE_IMPORT_PRIVATE_KEY -> isInputValid && !isAddressMismatched
        else -> isInputValid && !addressExists && isWalletNameValid
    }

    val title = when (mode) {
        WalletSecurityActivity.Mode.IMPORT_PRIVATE_KEY -> stringResource(R.string.import_private_key)
        WalletSecurityActivity.Mode.ADD_WATCH_ADDRESS -> stringResource(R.string.add_watch_address)
        WalletSecurityActivity.Mode.RE_IMPORT_PRIVATE_KEY -> stringResource(R.string.import_private_key)
        else -> ""
    }
    val hint = when (mode) {
        WalletSecurityActivity.Mode.IMPORT_PRIVATE_KEY,
        WalletSecurityActivity.Mode.RE_IMPORT_PRIVATE_KEY -> when {
            isSolana -> stringResource(R.string.solana_private_key_hint)
            isBitcoin || isPearl -> stringResource(R.string.bitcoin_private_key_hint)
            isLitecoin -> stringResource(R.string.private_key_hint)
            else -> stringResource(R.string.ethereum_private_key_hint)
        }
        WalletSecurityActivity.Mode.ADD_WATCH_ADDRESS -> stringResource(R.string.address_hint)
        else -> ""
    }

    MixinAppTheme {
        PageScaffold(title = title, pop = pop, backIcon = R.drawable.ic_close_black, actions = {
            IconButton(onClick = { context.openUrl(Constants.HelpLink.CUSTOMER_SERVICE) }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_support),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.icon
                )
            }
        }) {
            Column(
                modifier = Modifier
                    .imePadding()
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 16.dp)
            ) {
                if (showWalletNameInput) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(color = MixinAppTheme.colors.background)
                            .border(
                                width = 1.dp,
                                color = MixinAppTheme.colors.borderColor,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.Wallet_Name),
                            color = MixinAppTheme.colors.accent,
                            fontSize = 12.sp,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        BasicTextField(
                            state = walletNameState,
                            inputTransformation = InputTransformation {
                                if (length > 36) {
                                    revertAllChanges()
                                }
                            },
                            lineLimits = TextFieldLineLimits.SingleLine,
                            cursorBrush = SolidColor(MixinAppTheme.colors.accent),
                            textStyle = TextStyle(
                                color = MixinAppTheme.colors.textPrimary,
                                fontSize = 16.sp,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(color = MixinAppTheme.colors.background)
                        .border(
                            width = 1.dp,
                            color = MixinAppTheme.colors.borderColor,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .then(
                            if (chainId == null) {
                                Modifier.clickable {
                                    val fragmentManager = context.findFragmentActivityOrNull()?.supportFragmentManager
                                        ?: return@clickable
                                    if (fragmentManager.findFragmentByTag(NetworkSelectBottomSheetDialogFragment.TAG) != null) {
                                        return@clickable
                                    }
                                    val options = ArrayList(networks.map { (name, id) -> NetworkOption(id, name) })
                                    NetworkSelectBottomSheetDialogFragment.newInstance(
                                        options,
                                        networks[selectedNetworkName],
                                    ).apply {
                                        onNetworkSelected = { option ->
                                            selectedNetworkName = option.name
                                        }
                                    }.showNow(fragmentManager, NetworkSelectBottomSheetDialogFragment.TAG)
                                }
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.Choose_Network),
                            color = MixinAppTheme.colors.accent,
                            fontSize = 12.sp,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedNetworkName,
                            color = MixinAppTheme.colors.textPrimary,
                            fontSize = 16.sp,
                        )
                    }
                    if (chainId == null) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sort_arrow_down),
                            contentDescription = null,
                            tint = MixinAppTheme.colors.iconGray,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .cardBackground(
                            Color.Transparent,
                            MixinAppTheme.colors.borderColor,
                            cornerRadius = 8.dp
                        )
                ) {
                    BasicTextField(
                        state = textState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp),
                        lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 5),
                        cursorBrush = SolidColor(MixinAppTheme.colors.accent),
                        textStyle = TextStyle(
                            color = MixinAppTheme.colors.textPrimary,
                            fontSize = 16.sp,
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        decorator = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            ) {
                                if (text.isEmpty()) {
                                    Text(
                                        text = hint,
                                        color = MixinAppTheme.colors.textAssist,
                                        fontSize = 16.sp,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                    if (text.isNotBlank()) {
                        IconButton(
                            onClick = {
                                textState.clearText()
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
                                    clipboardManager.getText()?.let {
                                        textState.setTextAndPlaceCursorAtEnd(it.text)
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
                if (mode == WalletSecurityActivity.Mode.IMPORT_PRIVATE_KEY) {
                    Text(
                        stringResource(R.string.Private_Key_Notice),
                        color = MixinAppTheme.colors.textAssist,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        textAlign = TextAlign.Start
                    )
                }

                Spacer(modifier = Modifier.weight(1f))


                when {
                    addressExists -> {
                        Text(
                            text = stringResource(R.string.Wallet_Already_Exists),
                            color = MixinAppTheme.colors.red,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp, vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    showInvalidError -> {
                        Text(
                            text = when (mode) {
                                WalletSecurityActivity.Mode.IMPORT_PRIVATE_KEY,
                                WalletSecurityActivity.Mode.RE_IMPORT_PRIVATE_KEY ->
                                    stringResource(R.string.Invalid_Private_Key)
                                WalletSecurityActivity.Mode.ADD_WATCH_ADDRESS ->
                                    stringResource(R.string.Invalid_Address)
                                else -> ""
                            },
                            color = MixinAppTheme.colors.red,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp, vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    showReimportMismatchError -> {
                        Text(
                            text = stringResource(R.string.reimport_private_key_error),
                            color = MixinAppTheme.colors.red,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp, vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp)
                        .height(48.dp),
                    onClick = {
                        onConfirmClick(
                            currentChainId,
                            if (mode == WalletSecurityActivity.Mode.ADD_WATCH_ADDRESS && isEvmNetwork) {
                                Keys.toChecksumAddress(text)
                            } else if ((mode == WalletSecurityActivity.Mode.IMPORT_PRIVATE_KEY || mode == WalletSecurityActivity.Mode.RE_IMPORT_PRIVATE_KEY) && (isBitcoin || isPearl)) {
                                requireNotNull(normalizeBitcoinPrivateKeyToWif(text))
                            } else if ((mode == WalletSecurityActivity.Mode.IMPORT_PRIVATE_KEY || mode == WalletSecurityActivity.Mode.RE_IMPORT_PRIVATE_KEY) && isSolana // import solana key
                                && isSolanaHexPrivateKeyValid(text)
                            ) {
                                Numeric.hexStringToByteArray(text).encodeToBase58String()
                            } else {
                                text
                            },
                            walletName.trim().takeIf { showWalletNameInput }
                        )
                    },
                    enabled = isButtonEnabled,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isButtonEnabled) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGray,
                        disabledBackgroundColor = MixinAppTheme.colors.backgroundGray,
                        contentColor = Color.White,
                        disabledContentColor = Color.White
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
                        text = stringResource(id = if (mode == WalletSecurityActivity.Mode.ADD_WATCH_ADDRESS) R.string.Add else R.string.Import),
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun isBitcoinPrivateKeyValid(privateKey: String): Boolean {
    return try {
        if (privateKey.length in 51..52 && (privateKey.startsWith("5") ||
                    privateKey.startsWith("K") || privateKey.startsWith("L"))) {
            return isValidWIF(privateKey)
        }

        if (privateKey.matches(Regex("^[0-9a-fA-F]+$"))) {
            val privateKeyBytes = Numeric.hexStringToByteArray(privateKey)
            if (privateKeyBytes.size != 32) return false
            val scalar = BigInteger(1, privateKeyBytes)
            return scalar >= BigInteger.ONE && scalar < ECKey.ecDomainParameters().n
        }

        Timber.e("Unknown private key format")
        false
    } catch (e: Exception) {
        Timber.e(e, "Failed to parse bitcoin private key: ${e.message}")
        false
    }
}

private fun isValidWIF(wif: String): Boolean {
    return try {
        DumpedPrivateKey.fromBase58(BitcoinNetwork.MAINNET, wif)
        true
    } catch (e: Exception) {
        false
    }
}

private fun normalizeBitcoinPrivateKeyToWif(privateKey: String): String? {
    return try {
        if (privateKey.length in 51..52 && (privateKey.startsWith("5") || privateKey.startsWith("K") || privateKey.startsWith("L"))) {
            DumpedPrivateKey.fromBase58(BitcoinNetwork.MAINNET, privateKey)
            privateKey
        } else {
            if (!privateKey.matches(Regex("^[0-9a-fA-F]+$"))) return null
            val privateKeyBytes = Numeric.hexStringToByteArray(privateKey)
            if (privateKeyBytes.size != 32) return null
            val ecKey: ECKey = ECKey.fromPrivate(privateKeyBytes, true)
            ecKey.getPrivateKeyEncoded(BitcoinNetwork.MAINNET).toBase58()
        }
    } catch (e: Exception) {
        null
    }
}
