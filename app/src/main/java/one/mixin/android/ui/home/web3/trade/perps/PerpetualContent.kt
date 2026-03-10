package one.mixin.android.ui.home.web3.trade.perps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.api.response.perps.PerpsPositionHistoryItem
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.putStringSet
import one.mixin.android.session.Session
import one.mixin.android.ui.home.web3.trade.ClosedPositionItem
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.Fiats
import java.math.BigDecimal
import kotlin.math.abs

private const val POSITION_REFRESH_INTERVAL_MS = 3_000L
private const val CLOSED_POSITION_PREVIEW_LIMIT = 10

@Composable
fun PerpetualContent(
    onShowTradingGuide: () -> Unit,
    onShowMarketList: (isLong: Boolean) -> Unit,
    onShowAllMarkets: () -> Unit,
    onShowAllOpenPositions: () -> Unit,
    onShowAllClosedPositions: () -> Unit,
    onOpenPositionClick: (PerpsPositionItem) -> Unit,
    onMarketItemClick: (PerpsMarket) -> Unit,
    onClosedPositionClick: (PerpsPositionHistoryItem) -> Unit,
) {
    val context = LocalContext.current
    val walletId = Session.getAccountId()!!
    val quoteColorReversed = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val risingColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val fallingColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    val fiatSymbol = Fiats.getSymbol()
    val fiatRate = BigDecimal(Fiats.getRate())
    val viewModel = hiltViewModel<PerpetualViewModel>()
    val lifecycleOwner = LocalLifecycleOwner.current

    var markets by remember { mutableStateOf<List<PerpsMarket>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val openPositions by remember(walletId) {
        viewModel.observeOpenPositions(walletId)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val totalPnl by remember(walletId) {
        viewModel.observeTotalUnrealizedPnl(walletId)
    }.collectAsStateWithLifecycle(initialValue = 0.0)
    val closedPositions by remember(walletId) {
        viewModel.observeClosedPositions(walletId, CLOSED_POSITION_PREVIEW_LIMIT)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val openPositionsCount = openPositions.size
    val openPositionsPreview = openPositions.take(3)
    val marketsPreview = markets.take(3)
    val closedPositionsPreview = closedPositions.take(3)
    val totalPositionValue = openPositions.fold(BigDecimal.ZERO) { total, position ->
        val quantity = position.quantity.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val markPrice = position.markPrice?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        total + quantity.abs().multiply(markPrice)
    }
    val totalPositionValueFiatText = "${fiatSymbol}${totalPositionValue.multiply(fiatRate).priceFormat()}"
    val totalPnlFiatText = "${if (totalPnl >= 0) "+" else "-"}$fiatSymbol${BigDecimal.valueOf(totalPnl).abs().multiply(fiatRate).priceFormat()}"
    val totalPnlPercent = if (totalPositionValue == BigDecimal.ZERO) {
        0.0
    } else {
        totalPnl / totalPositionValue.toDouble() * 100
    }

    LaunchedEffect(Unit) {
        viewModel.loadMarkets(
            onSuccess = { data ->
                markets = data
                isLoading = false
            },
            onError = { error ->
                errorMessage = error
                isLoading = false
            }
        )
    }

    LaunchedEffect(walletId, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadAcceptedAssets(
                onSuccess = { assetIds ->
                    context.defaultSharedPreferences.putStringSet(
                        Constants.Account.PREF_PERPS_ACCEPTED_ASSET_IDS,
                        assetIds.toSet()
                    )
                }
            )
            while (isActive) {
                viewModel.refreshPositions(walletId)
                viewModel.refreshPositionHistory(walletId, limit = CLOSED_POSITION_PREVIEW_LIMIT)
                delay(POSITION_REFRESH_INTERVAL_MS)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .cardBackground(Color.Transparent, MixinAppTheme.colors.borderColor)
                    .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.Total_Position_Value),
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = totalPositionValueFiatText,
                fontSize = 18.sp,
                fontWeight = FontWeight.W600,
                color = MixinAppTheme.colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = totalPnlFiatText,
                    fontSize = 14.sp,
                    color = if (totalPnl >= 0) risingColor else fallingColor,
                )
                Text(
                    text = String.format("(%s%.2f%%)", if (totalPnlPercent >= 0) "+" else "", abs(totalPnlPercent)),
                    fontSize = 14.sp,
                    color = if (totalPnl >= 0) risingColor else fallingColor,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .cardBackground(Color.Transparent, MixinAppTheme.colors.borderColor)
                    .padding(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onShowAllOpenPositions),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.Open_Positions, openPositionsCount),
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textPrimary,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.textAssist,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (openPositionsCount == 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_empty_transaction),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.backgroundGrayLight,
                        modifier = Modifier.size(78.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.No_Positions),
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textAssist,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {
                            onShowTradingGuide()
                        })
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.How_Perps_Works),
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.accent,
                    )
                }
            } else {
                openPositionsPreview.forEach { position ->
                    OpenPositionItem(
                        position = position,
                        onClick = {
                            val targetMarket = markets.firstOrNull { it.marketId == position.productId }
                            if (targetMarket != null) {
                                onMarketItemClick(targetMarket)
                            } else {
                                onOpenPositionClick(position)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (openPositionsCount > openPositionsPreview.size) {
                    ViewAllAction(onClick = onShowAllOpenPositions)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(8.dp))
                .cardBackground(Color.Transparent, MixinAppTheme.colors.borderColor)
                .padding(16.dp)
        ) {

            // Markets Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShowAllMarkets() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.Perpetual_Markets),
                    fontSize = 16.sp,
                    color = MixinAppTheme.colors.textPrimary,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.textAssist,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MixinAppTheme.colors.accent,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "Error loading markets",
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.red,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                marketsPreview.forEach { market ->
                    PerpsMarketItem(
                        market = market,
                        quoteColorReversed = quoteColorReversed,
                        onClick = {
                            onMarketItemClick(market)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (markets.size > marketsPreview.size) {
                    ViewAllAction(
                        onClick = onShowAllMarkets
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        // Closed position Section
        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(8.dp))
                .cardBackground(Color.Transparent, MixinAppTheme.colors.borderColor)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onShowAllClosedPositions),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.Closed_Positions),
                    fontSize = 16.sp,
                    color = MixinAppTheme.colors.textPrimary,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.textAssist,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (closedPositions.isEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_empty_transaction),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.backgroundGrayLight,
                        modifier = Modifier.size(78.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.No_Closed_Positions),
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textAssist,
                    )
                }
            } else {
                closedPositionsPreview.forEach { position ->
                    ClosedPositionItem(
                        position = position,
                        onClick = { onClosedPositionClick(position) })
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (closedPositions.size > closedPositionsPreview.size) {
                    ViewAllAction(onClick = onShowAllClosedPositions)
                }
            }
        }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    onShowMarketList(true)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = risingColor,
                    contentColor = Color.White
                ),
                enabled = markets.isNotEmpty()
            ) {
                Text(
                    text = stringResource(R.string.Long),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = {
                    onShowMarketList(false)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = fallingColor,
                    contentColor = Color.White
                ),
                enabled = markets.isNotEmpty()
            ) {
                Text(
                    text = stringResource(R.string.Short),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ViewAllAction(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.view_all),
            fontSize = 14.sp,
            color = MixinAppTheme.colors.accent,
            fontWeight = FontWeight.W500,
        )
    }
}
