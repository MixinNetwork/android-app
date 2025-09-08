package one.mixin.android.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import one.mixin.android.Constants
import one.mixin.android.Constants.ChainId
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.tickVibrate
import one.mixin.android.session.Session
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.util.getChainName
import timber.log.Timber

object InputAmountDestinations {
    const val INPUT = "input"
    const val PREVIEW = "preview"
}

@Composable
fun InputAmountFlow(
    primaryAmount: String,
    minorAmount: String,
    tokenAmount: String,
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onSwitchClick: () -> Unit,
    onShareClick: (String) -> Unit,
    onCopyClick: (String) -> Unit,
    onForward: (TokenItem, String, String) -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
    token: TokenItem? = null,
    web3Token: Web3TokenItem? = null,
    address: String? = null,
    navController: NavHostController = rememberNavController(),
) {
    // Get unified token information from either TokenItem or Web3TokenItem
    val tokenSymbol = token?.symbol ?: web3Token?.symbol ?: ""
    val tokenIconUrl = token?.iconUrl ?: web3Token?.iconUrl
    val tokenChainIconUrl = token?.chainIconUrl ?: web3Token?.chainIcon
    val tokenChainName = token?.chainName ?: web3Token?.chainName
    val tokenAssetId = token?.assetId ?: web3Token?.assetId ?: ""
    val tokenAssetKey = token?.assetKey ?: web3Token?.assetKey
    val tokenChainId = token?.chainId ?: web3Token?.chainId ?: ""

    NavHost(
        navController = navController,
        startDestination = InputAmountDestinations.INPUT,
        modifier = modifier
    ) {
        composable(InputAmountDestinations.INPUT) {
            InputAmountScreen(
                primaryAmount = primaryAmount,
                minorAmount = minorAmount,
                onNumberClick = onNumberClick,
                onDeleteClick = onDeleteClick,
                onSwitchClick = onSwitchClick,
                onContinueClick = {
                    navController.navigate(InputAmountDestinations.PREVIEW)
                },
                onCloseClick = onCloseClick,
            )
        }

        composable(InputAmountDestinations.PREVIEW) {
            InputAmountPreviewScreen(
                primaryAmount = tokenAmount,
                tokenSymbol = tokenSymbol,
                tokenIconUrl = tokenIconUrl,
                tokenChainIconUrl = tokenChainIconUrl,
                tokenChainName = tokenChainName,
                tokenAssetId = tokenAssetId,
                tokenAssetKey = tokenAssetKey,
                tokenChainId = tokenChainId,
                address = address,
                onBackClick = {
                    navController.popBackStack()
                },
                onCloseClick = onCloseClick,
                onShareClick = onShareClick,
                onCopyClick = onCopyClick,
                onForward = onForward
            )
        }
    }
}

@Composable
fun InputAmountScreen(
    primaryAmount: String,
    minorAmount: String,
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onSwitchClick: () -> Unit,
    onContinueClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MixinAppTheme.colors.background)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 20.dp)
                .fillMaxWidth()
        ) {
            IconButton(
                onClick = {},
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "back",
                    modifier = Modifier.alpha(0f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.Enter_Amount),
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = MixinAppTheme.colors.textPrimary,
            )
            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = onCloseClick,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_close_black),
                    contentDescription = "close",
                    modifier = Modifier
                )
            }

        }
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = primaryAmount,
                fontSize = getPrimaryTextSize(primaryAmount),
                fontWeight = FontWeight.Medium,
                color = MixinAppTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Minor amount display with switch button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Text(
                    text = minorAmount,
                    fontSize = 16.sp,
                    color = MixinAppTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = onSwitchClick,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_switch),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Number keyboard
        NumberKeyboard(
            onNumberClick = onNumberClick,
            onDeleteClick = onDeleteClick,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Full width button with 20dp horizontal margins
        Button(
            onClick = onContinueClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = MixinAppTheme.colors.accent),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            Text(
                stringResource(id = R.string.Review),
                fontSize = 16.sp,
                color = Color.White,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// Helper function to generate deposit URI based on token chain and address
private fun generateDepositUri(token: TokenItem?, address: String?, amount: String): String? {
    if (token == null || amount == "0") return null
    if (address == null) {
        val code =
            "${Constants.Scheme.HTTPS_PAY}/${Session.getAccountId()}?asset=${token.assetId}&amount=$amount"
        return code
    }

    val cleanAmount = amount.replace(Regex("[^0-9.]"), "") // Remove symbol and spaces
    if (cleanAmount.isEmpty() || cleanAmount.toDoubleOrNull() == 0.0) return null

    return when (token.chainId) {
        ChainId.BITCOIN_CHAIN_ID -> {
            "bitcoin:$address?amount=$cleanAmount"
        }

        ChainId.ETHEREUM_CHAIN_ID -> {
            if (token.assetId == ChainId.ETHEREUM_CHAIN_ID) {
                // Native ETH transfer
                "ethereum:$address?amount=$cleanAmount"
            } else {
                // ERC20 token transfer
                "ethereum:${token.assetKey}@1/transfer?address=$address&amount=$cleanAmount"
            }
        }

        ChainId.Arbitrum -> {
            if (token.assetId == ChainId.Arbitrum) {
                "ethereum:$address@42161?amount=$cleanAmount"
            } else {
                "ethereum:${token.assetKey}@42161/transfer?address=$address&amount=$cleanAmount"
            }
        }

        ChainId.Optimism -> {
            if (token.assetId == ChainId.Optimism) {
                "ethereum:$address@10?amount=$cleanAmount"
            } else {
                "ethereum:${token.assetKey}@10/transfer?address=$address&amount=$cleanAmount"
            }
        }

        ChainId.Base -> {
            if (token.assetId == ChainId.Base) {
                "ethereum:$address@8453?amount=$cleanAmount"
            } else {
                "ethereum:${token.assetKey}@8453/transfer?address=$address&amount=$cleanAmount"
            }
        }

        ChainId.Polygon -> {
            if (token.assetId == ChainId.Polygon) {
                "ethereum:$address@137?amount=$cleanAmount"
            } else {
                "ethereum:${token.assetKey}@137/transfer?address=$address&amount=$cleanAmount"
            }
        }

        ChainId.BinanceSmartChain -> {
            if (token.assetId == ChainId.BinanceSmartChain) {
                "ethereum:$address@56?amount=$cleanAmount"
            } else {
                "ethereum:${token.assetKey}@56/transfer?address=$address&amount=$cleanAmount"
            }
        }

        ChainId.Litecoin -> {
            "litecoin:$address?amount=$cleanAmount"
        }

        ChainId.Dogecoin -> {
            "dogecoin:$address?amount=$cleanAmount"
        }

        ChainId.Dash -> {
            "dash:$address?amount=$cleanAmount&IS=1"
        }

        ChainId.Monero -> {
            "monero:$address?tx_amount=$cleanAmount"
        }

        ChainId.Solana -> {
            if (token.assetKey.isNullOrBlank()) {
                "solana:$address?amount=$cleanAmount"
            } else {
                "solana:$address?amount=$cleanAmount&spl-token=${token.assetKey}"
            }
        }

        else -> null
    }
}

// Helper function to generate QR code bitmap from deposit URI
@Composable
private fun generateQrCodeBitmap(
    token: TokenItem?,
    address: String?,
    amount: String
): android.graphics.Bitmap {
    val depositUri = generateDepositUri(token, address, amount.split(" ").first())
    return depositUri?.generateQRCode(200, 8)?.first
        ?: // Generate a fallback QR code with the address if URI generation fails
        (address ?: "").generateQRCode(200, 8).first
}

@Composable
fun InputAmountPreviewScreen(
    primaryAmount: String,
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    onShareClick: (String) -> Unit,
    onCopyClick: (String) -> Unit,
    onForward: (TokenItem, String, String) -> Unit,
    modifier: Modifier = Modifier,
    tokenSymbol: String = "",
    tokenIconUrl: String? = null,
    tokenChainIconUrl: String? = null,
    tokenChainName: String? = null,
    tokenAssetId: String = "",
    tokenAssetKey: String? = null,
    tokenChainId: String = "",
    address: String? = null,
) {
    // Generate deposit URI for copy and share operations
    val depositUri = generateDepositUri(
        token = if (tokenAssetId.isNotEmpty()) TokenItem(
            assetId = tokenAssetId,
            symbol = tokenSymbol,
            name = tokenSymbol, // Use symbol as name fallback
            iconUrl = tokenIconUrl ?: "",
            balance = "0", // Default balance
            priceBtc = "0", // Default BTC price
            priceUsd = "0", // Default USD price
            chainId = tokenChainId,
            changeUsd = "0", // Default change
            changeBtc = "0", // Default BTC change
            hidden = false, // Default visibility
            confirmations = 0, // Default confirmations
            chainIconUrl = tokenChainIconUrl,
            chainSymbol = null, // Chain symbol not available
            chainName = tokenChainName,
            assetKey = tokenAssetKey,
            dust = null, // Dust not available
            withdrawalMemoPossibility = null, // Memo possibility not available
            collectionHash = null, // Collection hash not available
            level = null // Level not available
        ) else null,
        address = address,
        amount = primaryAmount
    ) ?: (address ?: "")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MixinAppTheme.colors.background)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            IconButton(
                onClick = onBackClick,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Back",
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = onCloseClick,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_close_black),
                    contentDescription = "close",
                )
            }

        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(54.dp))
            if (address == null) {
                Text(
                    text = Session.getAccount()?.fullName ?: "",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MixinAppTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.contact_mixin_id,
                        Session.getAccount()?.identityNumber ?: ""
                    ),
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = stringResource(R.string.Deposit_to_Mixin, tokenSymbol),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MixinAppTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.Deposit_to_Mixin_sub, tokenSymbol),
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = generateQrCodeBitmap(
                            token = if (tokenAssetId.isNotEmpty()) TokenItem(
                                assetId = tokenAssetId,
                                symbol = tokenSymbol,
                                name = tokenSymbol, // Use symbol as name fallback
                                iconUrl = tokenIconUrl ?: "",
                                balance = "0", // Default balance
                                priceBtc = "0", // Default BTC price
                                priceUsd = "0", // Default USD price
                                chainId = tokenChainId,
                                changeUsd = "0", // Default change
                                changeBtc = "0", // Default BTC change
                                hidden = false, // Default visibility
                                confirmations = 0, // Default confirmations
                                chainIconUrl = tokenChainIconUrl,
                                chainSymbol = null, // Chain symbol not available
                                chainName = tokenChainName,
                                assetKey = tokenAssetKey,
                                dust = null, // Dust not available
                                withdrawalMemoPossibility = null, // Memo possibility not available
                                collectionHash = null, // Collection hash not available
                                level = null // Level not available
                            ) else null,
                            address = address,
                            primaryAmount
                        ).asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Token icon in the center
                Box {
                    CoilImage(
                        model = tokenIconUrl,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape),
                        placeholder = R.drawable.ic_avatar_place_holder
                    )
                    CoilImage(
                        model = tokenChainIconUrl,
                        modifier = Modifier
                            .size(13.dp)
                            .offset(x = 0.dp, y = (31).dp)
                            .border(1.dp, MixinAppTheme.colors.background, CircleShape),
                        placeholder = R.drawable.ic_avatar_place_holder
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            if (address == null) {
                Text(
                    text = stringResource(R.string.transfer_qrcode_prompt_amount, "$primaryAmount"),
                    fontSize = 12.sp,
                    color = MixinAppTheme.colors.textAssist,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            address?.let { addr ->
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = stringResource(R.string.Address),
                        fontSize = 12.sp,
                        color = MixinAppTheme.colors.textAssist,
                    )

                    Text(
                        text = addr,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = stringResource(R.string.network),
                                fontSize = 12.sp,
                                color = MixinAppTheme.colors.textAssist,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = tokenChainName ?: "Unknown",
                                color = MixinAppTheme.colors.textPrimary
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = stringResource(R.string.Amount),
                                fontSize = 12.sp,
                                color = MixinAppTheme.colors.textAssist,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = primaryAmount,
                                color = MixinAppTheme.colors.textPrimary,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (address == null) {
                Button(
                    onClick = {
                        onShareClick(depositUri)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MixinAppTheme.colors.backgroundWindow),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.Share),
                        fontSize = 16.sp,
                        color = MixinAppTheme.colors.textPrimary
                    )
                }

                Button(
                    onClick = {
                        val tokenItem = TokenItem(
                            assetId = tokenAssetId,
                            symbol = tokenSymbol,
                            name = tokenSymbol,
                            iconUrl = tokenIconUrl ?: "",
                            balance = "0",
                            priceBtc = "0",
                            priceUsd = "0",
                            chainId = tokenChainId,
                            changeUsd = "0",
                            changeBtc = "0",
                            hidden = false,
                            confirmations = 0,
                            chainIconUrl = tokenChainIconUrl,
                            chainSymbol = null,
                            chainName = tokenChainName,
                            assetKey = tokenAssetKey,
                            dust = null,
                            withdrawalMemoPossibility = null,
                            collectionHash = null,
                            level = null
                        )
                        onForward(tokenItem, depositUri, primaryAmount)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MixinAppTheme.colors.accent),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.Forward),
                        fontSize = 16.sp,
                        color = Color.White,
                    )
                }
            } else {
                Button(
                    onClick = {
                        onCopyClick(depositUri)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MixinAppTheme.colors.backgroundWindow),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.Copy),
                        fontSize = 16.sp,
                        color = MixinAppTheme.colors.textPrimary
                    )
                }

                Button(
                    onClick = {
                        onShareClick(depositUri)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MixinAppTheme.colors.accent),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.Share),
                        fontSize = 16.sp,
                        color = Color.White,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun NumberKeyboard(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val keys = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        ".", "0", "<<"
    )

    Column(modifier = modifier) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            itemsIndexed(keys) { index, key ->
                when {
                    key == "<<" -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MixinAppTheme.colors.background)
                                .clickable {
                                    context.clickVibrate()
                                    onDeleteClick()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_key_delete_white),
                                contentDescription = "Delete",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    key.isNotEmpty() -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MixinAppTheme.colors.background)
                                .clickable {
                                    context.tickVibrate()
                                    onNumberClick(key)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium,
                                color = MixinAppTheme.colors.textPrimary
                            )
                        }
                    }

                    else -> {
                        Spacer(modifier = Modifier.size(64.dp))
                    }
                }
            }
        }
    }
}

// Helper function to calculate text size based on content length
@Composable
private fun getPrimaryTextSize(text: String): androidx.compose.ui.unit.TextUnit {
    val length = text.length
    val size = if (length <= 4) {
        56f
    } else {
        56f - 2 * (length - 4)
    }
    return size.coerceAtLeast(24f).sp
}

// Input handling functions following CalculateFragment logic
object AmountInputHandler {

    fun handleNumberInput(
        currentValue: String,
        inputValue: String,
        isPrimary: Boolean,
        currencyName: String? = null,
    ): String {
        return when {
            // Handle decimal point
            inputValue == "." -> {
                if (currentValue.contains(".") || isFullCurrency(currencyName)) {
                    // Already has decimal point or currency doesn't support decimals
                    currentValue
                } else {
                    if (currentValue == "0") "0." else "$currentValue."
                }
            }
            // Handle number input when current value is "0"
            currentValue == "0" && inputValue != "." -> {
                inputValue
            }
            // Check if already has max decimal places
            hasMaxDecimalPlaces(currentValue, isPrimary) -> {
                currentValue
            }
            // Check for illegal input
            isIllegalInput(currentValue, currencyName) -> {
                currentValue
            }

            else -> {
                "$currentValue$inputValue"
            }
        }
    }

    fun handleDeleteInput(currentValue: String): String {
        return if (currentValue == "0") {
            "0"
        } else if (currentValue.length == 1) {
            "0"
        } else {
            currentValue.substring(0, currentValue.length - 1)
        }
    }

    private fun hasMaxDecimalPlaces(value: String, isPrimary: Boolean): Boolean {
        if (!value.contains(".")) return false
        val decimalPart = value.substringAfter('.')
        val limit = if (isPrimary) 8 else 2
        return decimalPart.length >= limit
    }

    private fun isFullCurrency(currencyName: String?): Boolean {
        // Some currencies don't support decimal places (like JPY, KRW)
        return currencyName in listOf("JPY", "KRW", "VND")
    }

    @Suppress("UNUSED_PARAMETER")
    private fun isIllegalInput(value: String, currencyName: String?): Boolean {
        // Add specific validation logic based on currency requirements
        return false
    }
}
