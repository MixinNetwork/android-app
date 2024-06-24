package one.mixin.android.ui.home.web3.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Cyan
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.api.response.Web3Token
import one.mixin.android.api.response.web3.BalanceChange
import one.mixin.android.api.response.web3.Item
import one.mixin.android.api.response.web3.ParsedInstruction
import one.mixin.android.api.response.web3.ParsedTx
import one.mixin.android.api.response.wrappedSolTokenAssetKey
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.currencyFormat
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.priceUSD
import one.mixin.android.vo.safe.Token
import one.mixin.android.web3.js.SolanaTxSource
import java.math.BigDecimal

private val gradientColors = listOf(Cyan, Color(0xFF0066FF), Color(0xFF800080))

@Composable
fun TransactionPreview(
    balance: BigDecimal,
    chain: Chain,
    asset: Token?,
) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(MixinAppTheme.colors.background)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Box(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.Balance_Change),
            color = MixinAppTheme.colors.textSubtitle,
            fontSize = 14.sp,
        )
        Box(modifier = Modifier.height(8.dp))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                modifier = Modifier.alignByBaseline(),
                text = "-$balance",
                color = Color(0xFFE86B67),
                fontFamily = FontFamily(Font(R.font.mixin_font)),
                fontSize = 30.sp,
            )
            Box(modifier = Modifier.width(4.dp))
            Text(
                modifier = Modifier.alignByBaseline(),
                text = chain.symbol,
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 12.sp,
            )
            Box(modifier = Modifier.weight(1f))
            CoilImage(
                model = asset?.iconUrl,
                modifier =
                Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                placeholder = R.drawable.ic_avatar_place_holder,
            )
        }
        Text(
            text = balance.multiply(asset.priceUSD()).currencyFormat(),
            color = MixinAppTheme.colors.textMinor,
            fontSize = 12.sp,
        )
        Box(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun TokenTransactionPreview(
    amount: String,
    token: Web3Token,
) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(MixinAppTheme.colors.background)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        BalanceChangeHead()
        Row(
            modifier =
                Modifier
                    .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            CoilImage(
                model = token.iconUrl,
                modifier =
                Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                placeholder = R.drawable.ic_avatar_place_holder,
            )
            Box(modifier = Modifier.width(12.dp))
            Text(
                text = token.name,
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.W600
            )
            Box(modifier = Modifier.weight(1f))
            Text(
                text = "-${amount} ${token.symbol}",
                color = MixinAppTheme.colors.red,
                fontSize = 14.sp,
            )
        }
        Box(modifier = Modifier.height(4.dp))
        Text(
            text = BigDecimal(amount).multiply(BigDecimal(token.price)).currencyFormat(),
            color = MixinAppTheme.colors.textMinor,
            fontSize = 12.sp,
        )
        Box(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun SolanaParsedTxPreview(
    asset: Token?,
    parsedTx: ParsedTx?,
    solanaTxSource: SolanaTxSource,
) {
    if (parsedTx?.tokens == null || parsedTx.balanceChanges == null) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(MixinAppTheme.colors.background)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            BalanceChangeHead()
            // is refreshing parsedTx or tokens
            if (parsedTx?.instructions == null || (parsedTx.balanceChanges != null && parsedTx.tokens == null)) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MixinAppTheme.colors.accent,
                )
            } else {
                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        modifier = Modifier.alignByBaseline(),
                        text = stringResource(id = R.string.preview_unavailable),
                        color = MixinAppTheme.colors.textPrimary,
                        fontFamily = FontFamily(Font(R.font.mixin_font)),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.W600
                    )
                    Box(modifier = Modifier.weight(1f))
                    CoilImage(
                        model = asset?.iconUrl,
                        modifier =
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        placeholder = R.drawable.ic_avatar_place_holder,
                    )
                }
            }
        }
        return
    }
    val viewDetails = remember {
        mutableStateOf(false)
    }
    val rotation by animateFloatAsState(if (viewDetails.value) 90f else 0f, label = "rotation")
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(MixinAppTheme.colors.background)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        BalanceChangeHead()
        parsedTx.balanceChanges.forEach { bc ->
            val token = parsedTx.tokens?.get(bc.address) ?: return
            BalanceChangeItem(token, bc)
            Box(modifier = Modifier.height(10.dp))
        }
        if (!solanaTxSource.isInnerTx()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewDetails.value = !viewDetails.value
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_arrow),
                    modifier =
                    Modifier
                        .size(24.dp, 24.dp)
                        .rotate(rotation),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.accent,
                )
                Text(
                    modifier = Modifier.padding(start = 4.dp),
                    text = stringResource(id = R.string.View_details),
                    color = MixinAppTheme.colors.accent,
                    fontFamily = FontFamily(Font(R.font.mixin_font)),
                    fontSize = 14.sp,
                )
            }
            if (viewDetails.value) {
                Box(modifier = Modifier.height(10.dp))
                Instructions(parsedTx.instructions)
            }
        }
    }
}

@Composable
fun BalanceChangeHead() {
    Box(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(id = R.string.Balance_Change),
        color = MixinAppTheme.colors.textSubtitle,
        fontSize = 14.sp,
    )
    Box(modifier = Modifier.height(8.dp))
}

@Composable
fun MessagePreview(
    content: String,
    onPreviewMessage: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Box(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(id = R.string.Message),
            color = MixinAppTheme.colors.textSubtitle,
            fontSize = 14.sp,
        )
        Box(modifier = Modifier.height(4.dp))
        Box(
            modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(0.dp, 128.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MixinAppTheme.colors.backgroundWindow)
                .clickable { onPreviewMessage(content) },
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                text = content,
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 16.sp,
            )
            Image(
                painter = painterResource(R.drawable.ic_post),
                modifier =
                Modifier
                    .size(40.dp, 40.dp)
                    .padding(horizontal = 8.dp)
                    .align(Alignment.TopEnd),
                contentDescription = null,
            )
        }
    }
}

@Composable
fun Warning(
    modifier: Modifier,
) {
    var isVisible by remember { mutableStateOf(true) }
    if (isVisible) {
        Row(
            modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .background(MixinAppTheme.colors.tipWarning)
                .border(1.dp, MixinAppTheme.colors.tipWarningBorder, shape = RoundedCornerShape(8.dp))
                .padding(20.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_warning),
                modifier =
                Modifier
                    .size(40.dp, 40.dp)
                    .padding(horizontal = 7.dp),
                contentDescription = null,
            )
            Box(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    text = stringResource(id = R.string.decode_transaction_failed),
                    color = MixinAppTheme.colors.tipError,
                    fontSize = 14.sp,
                )
                Box(modifier = Modifier.width(8.dp))
                Row(modifier = Modifier.align(Alignment.End)) {
                    Text(
                        modifier =
                            Modifier.clickable {
                                isVisible = false
                            },
                        text = stringResource(id = R.string.Got_it),
                        color = MixinAppTheme.colors.textBlue,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceChangeItem(
    token: Web3Token,
    balanceChange: BalanceChange,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoilImage(
            model = token.iconUrl,
            modifier =
            Modifier
                .size(32.dp)
                .clip(CircleShape),
            placeholder = R.drawable.ic_avatar_place_holder,
        )
        Box(modifier = Modifier.width(12.dp))
        Text(
            text = if (balanceChange.address == wrappedSolTokenAssetKey) "Solana" else token.name,
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.W600
        )
        Box(modifier = Modifier.weight(1f))
        Text(
            text = "${token.toStringAmount(balanceChange.amount)} ${token.symbol}",
            color = if (balanceChange.amount >= 0) MixinAppTheme.colors.green else MixinAppTheme.colors.red,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun Instructions(
    instructions: List<ParsedInstruction>,
) {
    LazyColumn(
        modifier = Modifier.height(200.dp)
    ) {
        items(instructions.size) { i ->
            Instruction(instruction = instructions[i])
            Box(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Instruction(
    instruction: ParsedInstruction,
) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(MixinAppTheme.colors.backgroundGray)
            .border(1.dp, MixinAppTheme.colors.backgroundDark, shape = RoundedCornerShape(8.dp))
            .padding(16.dp, 12.dp),
    ) {
        Text(
            text = instruction.programName,
            fontSize = 16.sp,
            style = TextStyle(
                brush = Brush.linearGradient(
                    colors = gradientColors
                )
            )
        )
        Box(modifier = Modifier.height(12.dp))
        Row(
            modifier =
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = instruction.programId.formatPublicKey(20),
                color = MixinAppTheme.colors.accent,
                fontSize = 14.sp,
            )
            Box(modifier = Modifier.weight(1f))
            Text(
                text = instruction.instructionName,
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp,
            )
        }
        instruction.items?.forEach { item ->
            Box(modifier = Modifier.height(4.dp))
            Item(item)
        }
    }
}

@Composable
private fun Item(
    item: Item,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.key,
            color = MixinAppTheme.colors.textSubtitle,
            fontSize = 14.sp,
            fontWeight = FontWeight.W600
        )
        Box(modifier = Modifier.weight(1f))
        Text(
            modifier = Modifier.fillMaxWidth(0.8f),
            textAlign = TextAlign.End,
            text = item.value,
            color = MixinAppTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 14.sp,
        )
    }
}

@Preview
@Composable
fun PreviewMessage() {
    Box(modifier = Modifier.background(MixinAppTheme.colors.background)) {
        MessagePreview(
            content = """{
          "raw": [
            "0x9df67f5a05fb594c4357d87221cbd69f1d5a6fbb",
            "{\"types\":{\"Alias\":[{\"name\":\"from\",\"type\":\"address\"},{\"name\":\"alias\",\"type\":\"address\"},{\"name\":\"timestamp\",\"type\":\"uint64\"}],\"EIP712Domain\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"version\",\"type\":\"string\"}]},\"domain\":{\"name\":\"snapshot\",\"version\":\"0.1.4\"},\"primaryType\":\"Alias\",\"message\":{\"from\":\"0x9df67f5a05fb594c4357d87221cbd69f1d5a6fbb\",\"alias\":\"0x8f14e8dbc7b3619e5210201022f637f271545c90\",\"timestamp\":\"1710766295\"}}"
          ],
          "type": "TYPED_MESSAGE"
        }
    """,
        ) {
        }
    }
}

@Preview
@Composable
private fun TransactionPreview() {
    TransactionPreview(balance = BigDecimal(0.134), chain = Chain.Ethereum, null)
}

@Preview
@Composable
private fun WarningPreview() {
    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(300.dp),
    ) {
        ActionBottom(
            modifier = Modifier.align(Alignment.BottomCenter),
            cancelTitle = stringResource(id = R.string.Cancel),
            confirmTitle = stringResource(id = R.string.Confirm),
            cancelAction = { },
        ) {
        }

        Warning(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun ActionBottom(
    modifier: Modifier,
    cancelTitle: String,
    confirmTitle: String,
    cancelAction: () -> Unit,
    confirmAction: () -> Unit,
) {
    Row(
        modifier =
        modifier
            .background(MixinAppTheme.colors.background)
            .padding(20.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Button(
            onClick = cancelAction,
            colors =
                ButtonDefaults.outlinedButtonColors(
                    backgroundColor = MixinAppTheme.colors.backgroundGray,
                    contentColor = MixinAppTheme.colors.shadow,
                ),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 36.dp, vertical = 11.dp),
            elevation =
                ButtonDefaults.elevation(
                    pressedElevation = 0.dp,
                    defaultElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    focusedElevation = 0.dp,
                ),
        ) {
            Text(text = cancelTitle, color = MixinAppTheme.colors.textPrimary)
        }
        Box(modifier = Modifier.width(36.dp))
        Button(
            onClick = confirmAction,
            colors =
                ButtonDefaults.outlinedButtonColors(
                    backgroundColor = MixinAppTheme.colors.accent,
                    contentColor = MixinAppTheme.colors.shadow,
                ),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 36.dp, vertical = 11.dp),
            elevation =
                ButtonDefaults.elevation(
                    pressedElevation = 0.dp,
                    defaultElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    focusedElevation = 0.dp,
                ),
        ) {
            Text(text = confirmTitle, color = Color.White)
        }
    }
}

@Preview
@Composable
fun TransferBottomPreview() {
    Column {
        ActionBottom(modifier = Modifier, stringResource(id = R.string.Cancel), stringResource(id = R.string.Confirm), {}, {})
        ActionBottom(modifier = Modifier, stringResource(id = R.string.Discard), stringResource(id = R.string.Send), {}, {})
    }
}

@Preview
@Composable
fun BalanceChangePreview() {
    val token = Web3Token(fungibleId = "", name = "Solana", symbol = "SOL", chainId = "solana", chainName = "Solana", chainIconUrl = "https://raw.githubusercontent.com/solana-labs/token-list/main/assets/mainnet/So11111111111111111111111111111111111111112/logo.png", balance = "0.01605982", price = "132.9102434930042", changeAbsolute = "-0.030625", changePercent = "-0.023036555963245636", decimals = 9, assetKey = "asset_key", iconUrl = "https://raw.githubusercontent.com/solana-labs/token-list/main/assets/mainnet/So11111111111111111111111111111111111111112/logo.png")
    BalanceChangeItem(token = token, balanceChange = BalanceChange("So11111111111111111111111111111111111111112", -10000000))
}

@Preview
@Composable
fun ItemPreview() {
    Item(Item("Compute Unit Limit", "1400000 compute units"))
}

@Preview
@Composable
fun SolanaParsedTxPreviewPreview() {
    val data = """{"balance_changes":[{"address":"So11111111111111111111111111111111111111112","amount":-10000000},{"address":"EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v","amount":1323264}],"instructions":[{"program_id":"ComputeBudget111111111111111111111111111111","program_name":"ComputeBudget","instruction_name":"SetComputeUnitLimit","items":[{"key":"Compute Unit Limit","value":"600000 compute units"}]},{"program_id":"ComputeBudget111111111111111111111111111111","program_name":"ComputeBudget","instruction_name":"SetComputeUnitPrice","items":[{"key":"Compute Unit Price","value":"0.1 lamports per compute unit"}]},{"program_id":"ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL","program_name":"AssociatedTokenAccount","instruction_name":"Create"},{"program_id":"11111111111111111111111111111111","program_name":"System","instruction_name":"Transfer","items":[{"key":"Transfer Amount (SOL)","value":"0.01"}]},{"program_id":"TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA","program_name":"Token","instruction_name":"SyncNative"},{"program_id":"JUP6LkbZbjS1jKKwapdHNy74zcZ3tLUZoi5QNyVTaV4","program_name":"Jupiter","instruction_name":"Route","items":[{"key":"Route Plan","value":""},{"key":"In Amount","value":"824635312696"},{"key":"Quoted Out Amount","value":"824635312704"},{"key":"Slippage Bps","value":"824635312712"},{"key":"Platform Fee Bps","value":"50"}],"token_changes":[{"address":"So11111111111111111111111111111111111111112","amount":10000000,"is_pay":true},{"address":"EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v","amount":1323264,"is_pay":false}]},{"program_id":"TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA","program_name":"Token","instruction_name":"CloseAccount"}]}"""
    val parsedTx = GsonHelper.customGson.fromJson(data, ParsedTx::class.java)
    val tokensData = """[{"id":"So11111111111111111111111111111111111111112","fungible_id":"","name":"Wrapped SOL","symbol":"SOL","icon_url":"https://raw.githubusercontent.com/solana-labs/token-list/main/assets/mainnet/So11111111111111111111111111111111111111112/logo.png","chain_id":"solana","chain_name":"Solana","chain_icon_url":"https://raw.githubusercontent.com/solana-labs/token-list/main/assets/mainnet/So11111111111111111111111111111111111111112/logo.png","balance":"0","price":"131.23961288579045","change_absolute":"-5.5895303","change_percent":"-4.085043702224179","decimals":9,"asset_key":"So11111111111111111111111111111111111111112","associated_account":""},{"id":"EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v","fungible_id":"","name":"USD Coin","symbol":"USDC","icon_url":"https://raw.githubusercontent.com/solana-labs/token-list/main/assets/mainnet/EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v/logo.png","chain_id":"solana","chain_name":"Solana","chain_icon_url":"https://raw.githubusercontent.com/solana-labs/token-list/main/assets/mainnet/So11111111111111111111111111111111111111112/logo.png","balance":"0","price":"0.9999952","change_absolute":"-0.00004657","change_percent":"-0.004655805573562128","decimals":6,"asset_key":"EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v","associated_account":""}]"""
    val tokens = GsonHelper.customGson.fromJson(tokensData, Array<Web3Token>::class.java)
    parsedTx.tokens = tokens.associateBy { it.assetKey }
    SolanaParsedTxPreview(parsedTx = parsedTx, asset = null, solanaTxSource = SolanaTxSource.Web)
}