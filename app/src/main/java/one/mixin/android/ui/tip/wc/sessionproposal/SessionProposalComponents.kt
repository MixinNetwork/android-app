package one.mixin.android.ui.tip.wc.sessionproposal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.numberFormat2
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.WalletConnectAddresses
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

internal data class WalletChainAddressRow(
    val iconResList: List<Int>,
    val address: String,
)

private val evmPreviewChainIcons = listOf(
    R.drawable.ic_chain_eth,
    R.drawable.ic_chain_polygon,
    R.drawable.ic_chain_bsc,
    R.drawable.ic_chain_base,
)

internal fun buildWalletChainAddressRows(
    addresses: WalletConnectAddresses,
    proposalChainIds: Set<String>?,
): List<WalletChainAddressRow> {
    val hasFilter = !proposalChainIds.isNullOrEmpty()
    val wantsEvm = !hasFilter || proposalChainIds.any { it.startsWith("eip155:") }
    val wantsSolana = !hasFilter || proposalChainIds.any { Chain.Solana.supportsWalletConnectChainId(it) }
    val wantsBitcoin = hasFilter && Chain.Bitcoin.chainId in proposalChainIds

    val rows =
        buildList {
            if (wantsEvm && addresses.evm.isNotBlank()) {
                add(WalletChainAddressRow(evmPreviewChainIcons, addresses.evm))
            }
            if (wantsSolana && addresses.solana.isNotBlank()) {
                add(WalletChainAddressRow(listOf(R.drawable.ic_chain_sol), addresses.solana))
            }
            if (wantsBitcoin && addresses.bitcoin.isNotBlank()) {
                add(WalletChainAddressRow(listOf(R.drawable.ic_chain_btc), addresses.bitcoin))
            }
        }
    if (rows.isNotEmpty()) return rows

    return buildList {
        if (addresses.evm.isNotBlank()) {
            add(WalletChainAddressRow(evmPreviewChainIcons, addresses.evm))
        }
        if (addresses.solana.isNotBlank()) {
            add(WalletChainAddressRow(listOf(R.drawable.ic_chain_sol), addresses.solana))
        }
        if (addresses.bitcoin.isNotBlank()) {
            add(WalletChainAddressRow(listOf(R.drawable.ic_chain_btc), addresses.bitcoin))
        }
    }
}

@Composable
internal fun WalletLabelBadge(
    text: String,
    textColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = textColor,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        maxLines = 1,
        modifier =
            modifier
                .background(color = backgroundColor, shape = RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

@Composable
internal fun WalletBalanceRow(
    balance: BigDecimal,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = balance.multiply(Fiats.getRate().toBigDecimal()).numberFormat2(),
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.W400,
            fontFamily = FontFamily(Font(R.font.mixin_font)),
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = Fiats.getAccountCurrencyAppearance(),
            color = MixinAppTheme.colors.textAssist,
            fontSize = 12.sp,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
    }
}

@Composable
internal fun WalletChainAddressRows(
    addresses: WalletConnectAddresses,
    proposalChainIds: Set<String>?,
    modifier: Modifier = Modifier,
) {
    val rows = buildWalletChainAddressRows(addresses, proposalChainIds)
    if (rows.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val overlapStep = 12.dp
                val iconsWidth = 18.dp + overlapStep * (row.iconResList.size - 1)
                Box(modifier = Modifier.width(iconsWidth)) {
                    row.iconResList.forEachIndexed { index, iconRes ->
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .size(18.dp)
                                    .offset(x = overlapStep * index)
                                    .zIndex(index.toFloat())
                                    .clip(CircleShape)
                                    .border(1.dp, MixinAppTheme.colors.background, CircleShape),
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = row.address.formatPublicKey(limit = 16, prefixLen = 7, suffixLen = 7),
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
            }
        }
    }
}
