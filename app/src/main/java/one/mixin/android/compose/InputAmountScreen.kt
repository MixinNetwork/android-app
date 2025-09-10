package one.mixin.android.compose

import android.graphics.Bitmap
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
import androidx.compose.ui.unit.TextUnit
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
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.tickVibrate
import one.mixin.android.session.Session
import one.mixin.android.util.getChainName
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow
import one.mixin.android.extension.dp as dip

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
    onForward: (String, String, String) -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
    token: TokenItem? = null,
    web3Token: Web3TokenItem? = null,
    address: String? = null,
    navController: NavHostController = rememberNavController(),
) {
    val tokenSymbol = token?.symbol ?: web3Token?.symbol ?: ""
    val tokenIconUrl = token?.iconUrl ?: web3Token?.iconUrl
    val tokenChainIconUrl = token?.chainIconUrl ?: web3Token?.chainIcon
    val tokenChainName = token?.chainName ?: web3Token?.chainName
    val tokenAssetId = token?.assetId ?: web3Token?.assetId ?: ""
    val tokenAssetKey = token?.assetKey ?: web3Token?.assetKey
    val tokenChainId = token?.chainId ?: web3Token?.chainId ?: ""
    val tokenPrecision = token?.precision ?: web3Token?.precision

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
                tokenPrecision = tokenPrecision,
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
                .padding(vertical = 6.dp, horizontal = 4.dp)
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
private fun generateDepositUri(
    assetId: String?,
    chainId: String?,
    assetKey: String?,
    address: String?,
    amount: String,
    precision: Int? = null
): String? {
    if (assetId.isNullOrEmpty() || amount == "0") return null
    if (address == null) {
        val code =
            "${Constants.Scheme.HTTPS_PAY}/${Session.getAccountId()}?asset=${assetId}&amount=$amount"
        return code
    }

    val cleanAmount = amount.replace(Regex("[^0-9.]"), "") // Remove symbol and spaces
    if (cleanAmount.isEmpty() || cleanAmount.toDoubleOrNull() == 0.0) return null

    return when (chainId) {
        ChainId.BITCOIN_CHAIN_ID -> {
            "bitcoin:$address?amount=$cleanAmount"
        }

        ChainId.ETHEREUM_CHAIN_ID -> {
            if (assetId == ChainId.ETHEREUM_CHAIN_ID) {
                // Native ETH transfer
                "ethereum:$address?amount=$cleanAmount"
            } else {
                // ERC20 token transfer - use precision for accurate conversion with BigDecimal
                val uint256Amount = try {
                    val tokenAmount = BigDecimal(cleanAmount)
                    val decimals = precision ?: 18 // Default to 18 if precision not available
                    val multiplier = BigDecimal.TEN.pow(decimals)
                    tokenAmount.multiply(multiplier).toBigInteger().toString()
                } catch (_: Exception) {
                    cleanAmount
                }
                "ethereum:${assetKey}@1/transfer?address=$address&amount=$cleanAmount&uint256=$uint256Amount"
            }
        }

        ChainId.Arbitrum -> {
            if (assetId == ChainId.Arbitrum) {
                "ethereum:$address@42161?amount=$cleanAmount"
            } else {
                val uint256Amount = try {
                    val tokenAmount = BigDecimal(cleanAmount)
                    val decimals = precision ?: 18
                    val multiplier = BigDecimal.TEN.pow(decimals)
                    tokenAmount.multiply(multiplier).toBigInteger().toString()
                } catch (_: Exception) {
                    cleanAmount
                }
                "ethereum:${assetKey}@42161/transfer?address=$address&amount=$cleanAmount&uint256=$uint256Amount"
            }
        }

        ChainId.Optimism -> {
            if (assetId == ChainId.Optimism) {
                "ethereum:$address@10?amount=$cleanAmount"
            } else {
                val uint256Amount = try {
                    val tokenAmount = BigDecimal(cleanAmount)
                    val decimals = precision ?: 18
                    val multiplier = BigDecimal.TEN.pow(decimals)
                    tokenAmount.multiply(multiplier).toBigInteger().toString()
                } catch (_: Exception) {
                    cleanAmount
                }
                "ethereum:${assetKey}@10/transfer?address=$address&amount=$cleanAmount&uint256=$uint256Amount"
            }
        }

        ChainId.Base -> {
            if (assetId == ChainId.Base) {
                "ethereum:$address@8453?amount=$cleanAmount"
            } else {
                val uint256Amount = try {
                    val tokenAmount = BigDecimal(cleanAmount)
                    val decimals = precision ?: 18
                    val multiplier = BigDecimal.TEN.pow(decimals)
                    tokenAmount.multiply(multiplier).toBigInteger().toString()
                } catch (_: Exception) {
                    cleanAmount
                }
                "ethereum:${assetKey}@8453/transfer?address=$address&amount=$cleanAmount&uint256=$uint256Amount"
            }
        }

        ChainId.Polygon -> {
            if (assetId == ChainId.Polygon) {
                "ethereum:$address@137?amount=$cleanAmount"
            } else {
                val uint256Amount = try {
                    val tokenAmount = BigDecimal(cleanAmount)
                    val decimals = precision ?: 18
                    val multiplier = BigDecimal.TEN.pow(decimals)
                    tokenAmount.multiply(multiplier).toBigInteger().toString()
                } catch (_: Exception) {
                    cleanAmount
                }
                "ethereum:${assetKey}@137/transfer?address=$address&amount=$cleanAmount&uint256=$uint256Amount"
            }
        }

        ChainId.BinanceSmartChain -> {
            if (assetId == ChainId.BinanceSmartChain) {
                "ethereum:$address@56?amount=$cleanAmount"
            } else {
                val uint256Amount = try {
                    val tokenAmount = BigDecimal(cleanAmount)
                    val decimals = precision ?: 18
                    val multiplier = BigDecimal.TEN.pow(decimals)
                    tokenAmount.multiply(multiplier).toBigInteger().toString()
                } catch (_: Exception) {
                    cleanAmount
                }
                "ethereum:${assetKey}@56/transfer?address=$address&amount=$cleanAmount&uint256=$uint256Amount"
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
            if (assetKey.isNullOrBlank()) {
                "solana:$address?amount=$cleanAmount"
            } else {
                "solana:$address?amount=$cleanAmount&spl-token=${assetKey}&token=${assetKey}"
            }
        }

        ChainId.TON_CHAIN_ID -> {
            if (assetId == ChainId.TON_CHAIN_ID) {
                // Native TON transfer - convert from nanotons to TON (divide by 1e9)
                val tonAmount = try {
                    val nanoAmount = BigDecimal(cleanAmount)
                    val divisor = BigDecimal("1000000000") // 1e9 without scientific notation
                    nanoAmount.multiply(divisor).stripTrailingZeros().toPlainString()
                } catch (_: Exception) {
                    cleanAmount
                }
                "ton://transfer/$address?amount=$tonAmount"
            } else {
                // Jetton token transfer - use precision for accurate conversion
                val jettonAmount = try {
                    val tokenAmount = BigDecimal(cleanAmount)
                    val decimals = precision ?: when (assetId) {
                        Constants.AssetId.USDT_ASSET_TON_ID -> 6
                        else -> 9 // TON standard
                    }
                    val multiplier = BigDecimal.TEN.pow(decimals)
                    tokenAmount.multiply(multiplier).toBigInteger().toString()
                } catch (_: Exception) {
                    cleanAmount

                }
                "ton://transfer/$address?jetton=$assetKey&amount=$jettonAmount"
            }
        }

        else -> null
    }
}

// Helper function to generate QR code bitmap from deposit URI
@Composable
private fun generateQrCodeBitmap(
    assetId: String,
    chainId: String,
    assetKey: String?,
    address: String?,
    amount: String
): Bitmap {
    val depositUri = generateDepositUri(
        assetId = assetId,
        chainId = chainId,
        assetKey = assetKey,
        address = address,
        amount = amount.split(" ").first()
    )
    return depositUri?.generateQRCode(200.dip, 0, 32.dip)?.first
        ?: // Generate a fallback QR code with the address if URI generation fails
        (address ?: "").generateQRCode(200.dip, 0, 32.dip).first
}

@Composable
fun InputAmountPreviewScreen(
    primaryAmount: String,
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    onShareClick: (String) -> Unit,
    onCopyClick: (String) -> Unit,
    onForward: (String, String, String) -> Unit,
    modifier: Modifier = Modifier,
    tokenSymbol: String = "",
    tokenIconUrl: String? = null,
    tokenChainIconUrl: String? = null,
    tokenChainName: String? = null,
    tokenAssetId: String = "",
    tokenAssetKey: String? = null,
    tokenChainId: String = "",
    tokenPrecision: Int? = null,
    address: String? = null,
) {
    // Generate deposit URI for copy and share operations
    val depositUri = generateDepositUri(
        assetId = tokenAssetId,
        chainId = tokenChainId,
        assetKey = tokenAssetKey,
        address = address,
        amount = primaryAmount.split(" ").first(),
        precision = tokenPrecision
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
                .padding(vertical = 8.dp, horizontal = 4.dp)
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
                .padding(horizontal = 28.dp),
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
                    fontSize = 16.sp,
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
                            assetId = tokenAssetId,
                            chainId = tokenChainId,
                            assetKey = tokenAssetKey,
                            address = address,
                            amount = primaryAmount
                        ).asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
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
                    fontSize = 14.sp,
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
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textAssist,
                    )

                    Text(
                        text = addr,
                        color = MixinAppTheme.colors.textPrimary,
                        fontSize = 16.sp,
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
                                fontSize = 14.sp,
                                color = MixinAppTheme.colors.textAssist,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = tokenChainName ?: "Unknown",
                                fontSize = 16.sp,
                                color = MixinAppTheme.colors.textPrimary
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = stringResource(R.string.Amount),
                                fontSize = 14.sp,
                                color = MixinAppTheme.colors.textAssist,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = primaryAmount,
                                fontSize = 16.sp,
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
                .padding(horizontal = 50.dp, vertical = 16.dp),
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
                        val tokenDisplayName = "${getChainName(tokenChainId, tokenChainName, tokenAssetKey)}"
                        onForward(tokenDisplayName, depositUri, primaryAmount)
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
private fun getPrimaryTextSize(text: String): TextUnit {
    val length = text.length
    val size = if (length <= 8) {
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
