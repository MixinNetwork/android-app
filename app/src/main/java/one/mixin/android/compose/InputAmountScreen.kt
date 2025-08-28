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
import one.mixin.android.Constants.ChainId
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.tickVibrate
import one.mixin.android.vo.safe.TokenItem

object InputAmountDestinations {
    const val INPUT = "input"
    const val PREVIEW = "preview"
}

@Composable
fun InputAmountFlow(
    primaryAmount: String,
    minorAmount: String,
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onSwitchClick: () -> Unit,
    modifier: Modifier = Modifier,
    token: TokenItem? = null,
    address: String? = null,
    navController: NavHostController = rememberNavController()
) {
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
                }
            )
        }

        composable(InputAmountDestinations.PREVIEW) {
            InputAmountPreviewScreen(
                primaryAmount = primaryAmount,
                token = token,
                address = address,
                onBackClick = {
                    navController.popBackStack()
                },
                onCloseClick = {

                },
                onConfirmClick = {
                    // TODO: Handle final confirmation
                }
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MixinAppTheme.colors.background)
            .padding(horizontal = 20.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = {},
                modifier = Modifier.size(32.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_close_black),
                    contentDescription = "close",
                    modifier = Modifier.size(24.dp)
                )
            }

        }
        // Primary amount display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = primaryAmount,
                fontSize = getPrimaryTextSize(primaryAmount),
                fontWeight = FontWeight.Medium,
                color = MixinAppTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )
        }

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
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
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
    if (token == null || address.isNullOrBlank() || amount == "0") return null

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
                "ethereum:$address?amount=$cleanAmount"
            } else {
                "ethereum:${token.assetKey}@42161/transfer?address=$address&amount=$cleanAmount"
            }
        }

        ChainId.Optimism -> {
            if (token.assetId == ChainId.Optimism) {
                "ethereum:$address?amount=$cleanAmount"
            } else {
                "ethereum:${token.assetKey}@10/transfer?address=$address&amount=$cleanAmount"
            }
        }

        ChainId.Base -> {
            if (token.assetId == ChainId.Base) {
                "ethereum:$address?amount=$cleanAmount"
            } else {
                "ethereum:${token.assetKey}@8453/transfer?address=$address&amount=$cleanAmount"
            }
        }

        ChainId.Polygon -> {
            if (token.assetId == ChainId.Polygon) {
                "ethereum:$address?amount=$cleanAmount"
            } else {
                "ethereum:${token.assetKey}@137/transfer?address=$address&amount=$cleanAmount"
            }
        }

        ChainId.BinanceSmartChain -> {
            if (token.assetId == ChainId.BinanceSmartChain) {
                "ethereum:$address?amount=$cleanAmount"
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
private fun generateQrCodeBitmap(token: TokenItem?, address: String?, amount: String): android.graphics.Bitmap {
    val depositUri = generateDepositUri(token, address, amount)
    return depositUri?.generateQRCode(200, 8)?.first
        ?: // Generate a fallback QR code with the address if URI generation fails
        (address ?: "").generateQRCode(200, 8).first
}

@Composable
fun InputAmountPreviewScreen(
    primaryAmount: String,
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
    token: TokenItem? = null,
    address: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MixinAppTheme.colors.background)
            .padding(horizontal = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(32.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Back",
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = onCloseClick,
                modifier = Modifier.size(32.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_close_black),
                    contentDescription = "close",
                    modifier = Modifier.size(24.dp)
                )
            }

        }

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(54.dp))
            Text(
                text = stringResource(R.string.Deposit_to_Mixin, token?.symbol ?: ""),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MixinAppTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.Deposit_to_Mixin_sub, token?.symbol ?: ""),
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist,
                textAlign = TextAlign.Center,
            )
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
                        bitmap = generateQrCodeBitmap(token, address, primaryAmount).asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Token icon in the center
                Box {
                    CoilImage(
                        model = token?.iconUrl,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape),
                        placeholder = R.drawable.ic_avatar_place_holder
                    )
                    CoilImage(
                        model = token?.chainIconUrl,
                        modifier = Modifier
                            .size(13.dp)
                            .offset(x = 0.dp, y = (31).dp)
                            .border(1.dp, MixinAppTheme.colors.background, CircleShape),
                        placeholder = R.drawable.ic_avatar_place_holder
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            address?.let { addr ->
                Column(
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
                }
            }

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
                        text = token?.chainName ?: "Unknown",
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

        Spacer(modifier = Modifier.height(20.dp))
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    // TODO: Implement copy functionality
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = MixinAppTheme.colors.backgroundWindow),
            ) {
                Text(
                    text = stringResource(R.string.Copy),
                    fontSize = 16.sp,
                    color = MixinAppTheme.colors.textPrimary
                )
            }

            Button(
                onClick = onConfirmClick,
                modifier = Modifier.weight(2f),
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
}

@Composable
fun NumberKeyboard(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
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
        currencyName: String? = null
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
            // Check if already has two decimal places
            isTwoDecimal(currentValue) -> {
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

    private fun isTwoDecimal(value: String): Boolean {
        return value.matches(Regex("\\d+\\.\\d{2}"))
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
