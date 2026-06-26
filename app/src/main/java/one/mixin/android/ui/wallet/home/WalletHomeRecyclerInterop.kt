package one.mixin.android.ui.wallet.home

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.databinding.ItemWeb3TransactionsBinding
import one.mixin.android.db.web3.vo.TransactionType
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.formatTransactionHashIfNeeded
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.priceFormat
import one.mixin.android.ui.home.web3.trade.perps.OpenPositionItem
import one.mixin.android.ui.wallet.adapter.OnSnapshotListener
import one.mixin.android.ui.wallet.adapter.SnapshotAdapter
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.safe.SafeSnapshotType
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.web3.details.Web3TransactionHolder
import java.math.BigDecimal

@Composable
fun PrivacyTokenRecycler(
    tokens: List<TokenItem>,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 20.dp,
) {
    WalletHomeItemColumn(
        count = tokens.size,
        modifier = modifier,
        itemSpacing = itemSpacing,
    ) { index ->
        PrivacyWalletTokenItem(
            token = tokens[index],
            onClick = { onClick(index) },
        )
    }
}

@Composable
fun Web3TokenRecycler(
    tokens: List<Web3TokenItem>,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 20.dp,
) {
    WalletHomeItemColumn(
        count = tokens.size,
        modifier = modifier,
        itemSpacing = itemSpacing,
    ) { index ->
        Web3WalletTokenItem(
            token = tokens[index],
            onClick = { onClick(index) },
        )
    }
}

@Composable
fun PrivacyTransactionRecycler(
    transactions: List<SnapshotItem>,
    onClick: (Int) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 20.dp,
    contentHorizontalPadding: Dp = 16.dp,
) {
    val spacing = with(LocalDensity.current) { itemSpacing.roundToPx() }
    val contentHorizontalPaddingPx = with(LocalDensity.current) { contentHorizontalPadding.roundToPx() }
    val currentTransactions = rememberUpdatedState(transactions)
    val currentOnClick = rememberUpdatedState(onClick)
    val currentOnUserClick = rememberUpdatedState(onUserClick)
    AndroidView(
        modifier = modifier,
        factory = { context ->
            RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context)
                isNestedScrollingEnabled = false
                if (spacing > 0) addItemDecoration(TokenItemSpacingDecoration(spacing))
                adapter = SnapshotAdapter(
                    compact = true,
                    compactAvatarStartMargin = contentHorizontalPaddingPx,
                ).apply {
                    listener =
                        object : OnSnapshotListener {
                            override fun <T> onNormalItemClick(item: T) {
                                currentOnClick.value(currentTransactions.value.indexOf(item as SnapshotItem))
                            }

                            override fun onUserClick(userId: String) {
                                currentOnUserClick.value(userId)
                            }

                            override fun onMoreClick() = Unit
                        }
                }
            }
        },
        update = { recyclerView ->
            (recyclerView.adapter as SnapshotAdapter).apply {
                listener =
                    object : OnSnapshotListener {
                        override fun <T> onNormalItemClick(item: T) {
                            currentOnClick.value(currentTransactions.value.indexOf(item as SnapshotItem))
                        }

                        override fun onUserClick(userId: String) {
                            currentOnUserClick.value(userId)
                        }

                        override fun onMoreClick() = Unit
                    }
                list = transactions
                notifyDataSetChanged()
            }
        },
    )
}

@Composable
fun Web3TransactionRecycler(
    transactions: List<Web3TransactionItem>,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 20.dp,
    contentHorizontalPadding: Dp = 16.dp,
) {
    val spacing = with(LocalDensity.current) { itemSpacing.roundToPx() }
    val contentHorizontalPaddingPx = with(LocalDensity.current) { contentHorizontalPadding.roundToPx() }
    val currentOnClick = rememberUpdatedState(onClick)
    AndroidView(
        modifier = modifier,
        factory = { context ->
            RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context)
                isNestedScrollingEnabled = false
                if (spacing > 0) addItemDecoration(TokenItemSpacingDecoration(spacing))
                adapter = Web3TransactionListAdapter(
                    compact = true,
                    compactAvatarStartMargin = contentHorizontalPaddingPx,
                )
            }
        },
        update = { recyclerView ->
            (recyclerView.adapter as Web3TransactionListAdapter).apply {
                updateOnClick { currentOnClick.value(it) }
                submitList(transactions)
            }
        },
    )
}

@Composable
fun PositionRecycler(
    positions: List<PerpsPositionItem>,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    itemSpacing: Dp = 0.dp,
) {
    WalletHomeItemColumn(
        count = positions.size,
        modifier = modifier,
        itemSpacing = itemSpacing,
    ) { index ->
        OpenPositionItem(
            position = positions[index],
            onClick = { onClick(index) },
            compact = compact,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = if (compact) 4.dp else 8.dp),
        )
    }
}

private class TokenItemSpacingDecoration(
    private val spacing: Int,
) : ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        val position = parent.getChildAdapterPosition(view)
        if (position != RecyclerView.NO_POSITION && position < state.itemCount - 1) {
            outRect.bottom = spacing
        }
    }
}

private class Web3TransactionListAdapter(
    private val compact: Boolean,
    private val compactAvatarStartMargin: Int,
) : RecyclerView.Adapter<Web3TransactionHolder>() {
    private var items: List<Web3TransactionItem> = emptyList()
    private var onClick: (Int) -> Unit = {}

    fun submitList(newItems: List<Web3TransactionItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateOnClick(onClick: (Int) -> Unit) {
        this.onClick = onClick
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Web3TransactionHolder {
        val binding = ItemWeb3TransactionsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Web3TransactionHolder(binding, compact, compactAvatarStartMargin)
    }

    override fun onBindViewHolder(holder: Web3TransactionHolder, position: Int) {
        holder.bind(items[position])
        holder.itemView.setOnClickListener { onClick(position) }
    }

    override fun getItemCount(): Int = items.size
}

@Composable
private fun WalletHomeItemColumn(
    count: Int,
    modifier: Modifier,
    itemSpacing: Dp,
    item: @Composable (Int) -> Unit,
) {
    Column(modifier = modifier) {
        repeat(count) { index ->
            item(index)
            if (index != count - 1) {
                Spacer(modifier = Modifier.height(itemSpacing))
            }
        }
    }
}

@Composable
fun PrivacyWalletTokenItem(
    token: TokenItem,
    onClick: () -> Unit,
) {
    WalletTokenItemLayout(
        iconUrl = token.iconUrl,
        chainIconUrl = token.chainIconUrl,
        collectionHash = token.collectionHash,
        showSpam = false,
        amount = formatPrivacyTokenBalance(token.balance),
        amountFontSize = 22.sp,
        symbol = token.symbol,
        fiatValue = "≈ ${Fiats.getSymbol()}${token.fiat().numberFormat2()}",
        price = tokenPriceText(token.priceUsd, token.priceFiat()),
        change = token.changeUsd.toBigDecimalOrNull()
            ?.multiply(BigDecimal(100))
            ?.let { "${it.numberFormat2()}%" },
        isRising = token.changeUsd.toBigDecimalOrNull()?.let { it >= BigDecimal.ZERO },
        onClick = onClick,
    )
}

@Composable
fun Web3WalletTokenItem(
    token: Web3TokenItem,
    onClick: () -> Unit,
) {
    WalletTokenItemLayout(
        iconUrl = token.iconUrl,
        chainIconUrl = token.chainIcon,
        collectionHash = null,
        showSpam = token.isSpam(),
        amount = formatWeb3TokenBalance(token.balance),
        amountFontSize = 22.sp,
        symbol = token.symbol,
        fiatValue = "≈ ${Fiats.getSymbol()}${token.fiat().numberFormat2()}",
        price = tokenPriceText(token.priceUsd, token.priceFiat()),
        change = token.changeUsd.toBigDecimalOrNull()?.let { "${it.numberFormat2()}%" },
        isRising = token.changeUsd.toBigDecimalOrNull()?.let { it >= BigDecimal.ZERO },
        onClick = onClick,
    )
}

@Composable
private fun WalletTokenItemLayout(
    iconUrl: String?,
    chainIconUrl: String?,
    collectionHash: String?,
    showSpam: Boolean,
    amount: String,
    amountFontSize: TextUnit,
    symbol: String,
    fiatValue: String,
    price: String?,
    change: String?,
    isRising: Boolean?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TokenIcon(
            iconUrl = iconUrl,
            chainIconUrl = chainIconUrl,
            collectionHash = collectionHash,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .offset(y = 2.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                if (showSpam) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_spam_token),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 6.dp, bottom = 2.dp)
                            .size(14.dp),
                    )
                }
                Text(
                    text = amount,
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = amountFontSize,
                    lineHeight = amountFontSize,
                    fontFamily = FontFamily(Font(R.font.mixin_font)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = noFontPaddingTextStyle(),
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = symbol,
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = noFontPaddingTextStyle(),
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = fiatValue,
                color = MixinAppTheme.colors.textAssist,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = noFontPaddingTextStyle(),
                modifier = Modifier.offset(y = (-2).dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        TokenPriceColumn(
            price = price,
            change = change,
            isRising = isRising,
            modifier = Modifier.offset(x = 4.dp),
        )
    }
}

@Composable
private fun TokenPriceColumn(
    price: String?,
    change: String?,
    isRising: Boolean?,
    modifier: Modifier = Modifier,
) {
    val quoteColorReversed = LocalContext.current.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val quoteColor = when (isRising) {
        true -> if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
        false -> if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
        null -> MixinAppTheme.colors.textAssist
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center,
    ) {
        if (price == null) {
            Text(
                text = stringResource(R.string.N_A),
                color = MixinAppTheme.colors.textAssist,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                maxLines = 1,
                textAlign = TextAlign.End,
                style = noFontPaddingTextStyle(),
            )
        } else {
            Text(
                text = change.orEmpty(),
                color = quoteColor,
                fontSize = 14.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                textAlign = TextAlign.End,
                style = noFontPaddingTextStyle(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = price,
                color = MixinAppTheme.colors.textAssist,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                maxLines = 1,
                textAlign = TextAlign.End,
                style = noFontPaddingTextStyle(),
                modifier = Modifier.offset(y = (-2).dp),
            )
        }
    }
}

@Composable
private fun PrivacyTransactionItem(
    transaction: SnapshotItem,
    onClick: () -> Unit,
    onUserClick: (String) -> Unit,
) {
    val type = transaction.simulateType()
    val title = when (type) {
        SafeSnapshotType.deposit -> transaction.deposit?.sender?.formatTransactionHashIfNeeded().orEmpty()
        SafeSnapshotType.withdrawal -> transaction.withdrawal?.receiver?.formatTransactionHashIfNeeded().orEmpty()
        else -> transaction.opponentFullName
            ?: transaction.transactionHash?.formatTransactionHashIfNeeded()
            ?: transaction.opponentId.formatTransactionHashIfNeeded()
    }
    val amountValue = transaction.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val isPositive = amountValue >= BigDecimal.ZERO
    val amountText = if (transaction.inscriptionHash.isNullOrEmpty()) {
        "${if (isPositive) "+" else ""}${transaction.amount.numberFormat()}"
    } else {
        if (isPositive) "+1" else "-1"
    }
    val amountColor = when {
        transaction.type == SafeSnapshotType.pending.name -> MixinAppTheme.colors.textPrimary
        isPositive -> MixinAppTheme.colors.walletGreen
        else -> MixinAppTheme.colors.walletRed
    }

    WalletTransactionRow(
        iconUrl = transaction.iconUrl,
        title = title,
        subtitle = transaction.assetSymbol.orEmpty(),
        amount = "$amountText ${transaction.assetSymbol.orEmpty()}".trim(),
        amountColor = amountColor,
        onClick = onClick,
        onTitleClick = if (type == SafeSnapshotType.snapshot && transaction.opponentId.isNotBlank()) {
            { onUserClick(transaction.opponentId) }
        } else {
            null
        },
    )
}

@Composable
private fun Web3TransactionItemRow(
    transaction: Web3TransactionItem,
    onClick: () -> Unit,
) {
    val isSend = transaction.transactionType == TransactionType.TRANSFER_OUT.value ||
        transaction.transactionType == TransactionType.APPROVAL.value
    val isReceive = transaction.transactionType == TransactionType.TRANSFER_IN.value
    val iconUrl = when (transaction.transactionType) {
        TransactionType.TRANSFER_IN.value -> transaction.receiveAssetIconUrl ?: transaction.chainIconUrl
        TransactionType.TRANSFER_OUT.value -> transaction.sendAssetIconUrl ?: transaction.chainIconUrl
        TransactionType.SWAP.value -> transaction.receiveAssetIconUrl ?: transaction.sendAssetIconUrl ?: transaction.chainIconUrl
        else -> transaction.sendAssetIconUrl ?: transaction.receiveAssetIconUrl ?: transaction.chainIconUrl
    }
    val symbol = when (transaction.transactionType) {
        TransactionType.TRANSFER_IN.value -> transaction.receiveAssetSymbol
        TransactionType.TRANSFER_OUT.value -> transaction.sendAssetSymbol
        TransactionType.SWAP.value -> transaction.receiveAssetSymbol ?: transaction.sendAssetSymbol
        else -> transaction.sendAssetSymbol ?: transaction.receiveAssetSymbol
    }.orEmpty()
    val title = when (transaction.transactionType) {
        TransactionType.TRANSFER_IN.value -> stringResource(R.string.Receive)
        TransactionType.TRANSFER_OUT.value -> stringResource(R.string.Send)
        TransactionType.SWAP.value -> stringResource(R.string.Swap)
        TransactionType.APPROVAL.value -> stringResource(R.string.Approval)
        else -> transaction.transactionType.replaceFirstChar { it.uppercase() }
    }
    val amountPrefix = when {
        isSend -> "-"
        isReceive -> "+"
        else -> ""
    }
    val amountColor = when {
        isSend -> MixinAppTheme.colors.walletRed
        isReceive -> MixinAppTheme.colors.walletGreen
        else -> MixinAppTheme.colors.textPrimary
    }

    WalletTransactionRow(
        iconUrl = iconUrl,
        title = title,
        subtitle = transaction.getFromAddress().ifBlank { transaction.transactionHash }.formatTransactionHashIfNeeded(),
        amount = "$amountPrefix${transaction.getFormattedAmount()} $symbol".trim(),
        amountColor = amountColor,
        onClick = onClick,
    )
}

@Composable
private fun WalletTransactionRow(
    iconUrl: String?,
    title: String,
    subtitle: String,
    amount: String,
    amountColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    onTitleClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoilImage(
            model = iconUrl,
            placeholder = R.drawable.ic_avatar_place_holder,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 16.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (onTitleClick == null) Modifier else Modifier.clickable(onClick = onTitleClick),
                style = noFontPaddingTextStyle(),
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = noFontPaddingTextStyle(),
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = amount,
            color = amountColor,
            fontSize = 16.sp,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            fontFamily = FontFamily(Font(R.font.mixin_font)),
            style = noFontPaddingTextStyle(),
        )
    }
}

@Composable
private fun TokenIcon(
    iconUrl: String?,
    chainIconUrl: String?,
    collectionHash: String?,
) {
    Box(modifier = Modifier.size(42.dp)) {
        CoilImage(
            model = iconUrl,
            placeholder = R.drawable.ic_avatar_place_holder,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        if (collectionHash.isNullOrEmpty() && !chainIconUrl.isNullOrBlank()) {
            CoilImage(
                model = chainIconUrl,
                placeholder = R.drawable.ic_avatar_place_holder,
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-1).dp, y = 1.dp)
                    .clip(CircleShape)
                    .background(MixinAppTheme.colors.background)
                    .border(1.dp, MixinAppTheme.colors.background, CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

private fun formatPrivacyTokenBalance(balance: String): String {
    val formatted = runCatching { balance.numberFormat() }.getOrDefault("0.00")
    return if (formatted.toFloatOrNull() == 0f) "0.00" else formatted
}

private fun formatWeb3TokenBalance(balance: String): String {
    if (balance.isBlank()) return "0.00"
    val formatted = runCatching { balance.numberFormat8() }.getOrDefault("0.00")
    return if (formatted.toFloatOrNull() == 0f) "0.00" else formatted
}

private fun tokenPriceText(
    priceUsd: String,
    priceFiat: BigDecimal,
): String? =
    if (priceUsd == "0") {
        null
    } else {
        "${Fiats.getSymbol()}${priceFiat.priceFormat()}"
    }

@Composable
private fun noFontPaddingTextStyle(): TextStyle =
    TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
