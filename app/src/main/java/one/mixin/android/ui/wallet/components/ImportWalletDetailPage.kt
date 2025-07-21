package one.mixin.android.ui.wallet.components

import PageScaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.wallet.WalletSecurityActivity
import one.mixin.android.ui.wallet.alert.components.cardBackground
import org.sol4k.Base58
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import org.web3j.crypto.WalletUtils
import org.web3j.utils.Numeric

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ImportWalletDetailPage(
    mode: WalletSecurityActivity.Mode,
    pop: () -> Unit,
    onConfirmClick: (String, String) -> Unit,
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    val networks = mapOf(
        "Ethereum" to Constants.ChainId.ETHEREUM_CHAIN_ID,
        "Base" to Constants.ChainId.Base,
        "BSC" to Constants.ChainId.BinanceSmartChain,
        "Polygon" to Constants.ChainId.Polygon,
        "Solana" to Constants.ChainId.SOLANA_CHAIN_ID
    )
    var expanded by remember { mutableStateOf(false) }
    var selectedNetworkName by remember { mutableStateOf(networks.keys.first()) }

    val isEvmNetwork = selectedNetworkName != "Solana"

    val isButtonEnabled by remember(mode, text, isEvmNetwork) {
        derivedStateOf {
            when (mode) {
                WalletSecurityActivity.Mode.IMPORT_PRIVATE_KEY -> {
                    if (isEvmNetwork) {
                        isEvmPrivateKeyValid(text)
                    } else {
                        isSolanaPrivateKeyValid(text)
                    }
                }
                WalletSecurityActivity.Mode.ADD_WATCH_ADDRESS -> {
                    if (isEvmNetwork) {
                        isEvmAddressValid(text)
                    } else {
                        isSolanaAddressValid(text)
                    }
                }
                else -> false
            }
        }
    }

    val title = when (mode) {
        WalletSecurityActivity.Mode.IMPORT_PRIVATE_KEY -> stringResource(R.string.import_private_key)
        WalletSecurityActivity.Mode.ADD_WATCH_ADDRESS -> stringResource(R.string.add_watch_address)
        else -> ""
    }
    val hint = when (mode) {
        WalletSecurityActivity.Mode.IMPORT_PRIVATE_KEY -> stringResource(R.string.private_key_hint)
        WalletSecurityActivity.Mode.ADD_WATCH_ADDRESS -> stringResource(R.string.address_hint)
        else -> ""
    }

    MixinAppTheme {
        PageScaffold(title = title, pop = pop, actions = { IconButton(onClick = { context.openUrl(Constants.HelpLink.CUSTOMER_SERVICE) }) { Icon(painter = painterResource(id = R.drawable.ic_support), contentDescription = null, tint = MixinAppTheme.colors.icon) } }) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {


                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = {
                        expanded = !expanded
                    },
                ) {
                    TextField(
                        readOnly = true,
                        value = selectedNetworkName,
                        onValueChange = { },
                        label = { Text(stringResource(R.string.Select_Network), color = MixinAppTheme.colors.accent) },
                        trailingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_sort_arrow_down),
                                contentDescription = null,
                                tint = MixinAppTheme.colors.iconGray
                            )
                        },
                        colors = TextFieldDefaults.textFieldColors(
                            textColor = MixinAppTheme.colors.textPrimary,
                            backgroundColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                color = MixinAppTheme.colors.background,
                            )
                            .border(
                                width = 1.dp,
                                color = MixinAppTheme.colors.borderColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {
                            expanded = false
                        },
                    ) {
                        networks.keys.forEach { networkName ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedNetworkName = networkName
                                    expanded = false
                                }
                            ) {
                                Text(text = networkName, color = MixinAppTheme.colors.textPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .cardBackground(
                            Color.Transparent,
                            MixinAppTheme.colors.borderColor,
                            cornerRadius = 8.dp
                        )
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text(hint) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp)
                            .padding(0.dp),
                        maxLines = 5,
                        shape = RoundedCornerShape(8.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = MixinAppTheme.colors.textPrimary,
                            backgroundColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            cursorColor = MixinAppTheme.colors.accent
                        )
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_copy_gray),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.iconGray,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .clickable {
                                clipboardManager
                                    .getText()
                                    ?.text?.let {
                                        text = it
                                    }
                            }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp)
                        .height(48.dp),
                    onClick = {
                        val chainId = networks[selectedNetworkName] ?: ""
                        onConfirmClick(chainId, text)
                    },
                    enabled = isButtonEnabled,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isButtonEnabled) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGrayLight,
                        disabledBackgroundColor = MixinAppTheme.colors.backgroundGrayLight,
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
                        text = stringResource(id = if (mode == WalletSecurityActivity.Mode.IMPORT_PRIVATE_KEY) R.string.Import else R.string.Add),
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

private fun isEvmAddressValid(address: String): Boolean {
    return try {
        WalletUtils.isValidAddress(address)
    } catch (e: Exception) {
        false
    }
}

private fun isEvmPrivateKeyValid(privateKey: String): Boolean {
    return try {
        WalletUtils.isValidPrivateKey(privateKey)
    } catch (e: Exception) {
        false
    }
}

private fun isSolanaAddressValid(address: String): Boolean {
    return try {
        val decoded = Base58.decode(address)
        decoded.size == 32
    } catch (e: Exception) {
        false
    }
}

private fun isSolanaPrivateKeyValid(privateKey: String): Boolean {
    return try {
        val decoded = Base58.decode(privateKey)
        decoded.size == 64
    } catch (e: Exception) {
        false
    }
}
