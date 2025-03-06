package one.mixin.android.ui.wallet.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.numberFormat2
import one.mixin.android.ui.wallet.alert.components.cardBackground
import java.math.BigDecimal

@Composable
fun WalletCard(
    balance: BigDecimal,
    assets: List<AssetDistribution>,
    destination: WalletDestination?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current
            ) { onClick() }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (destination == WalletDestination.Privacy) stringResource(R.string.Privacy_Wallet) else stringResource(
                        R.string.Classic_Wallet
                    ),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W500,
                    color = MixinAppTheme.colors.textPrimary
                )
                if (destination == WalletDestination.Privacy) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_wallet_privacy),
                        tint = Color.Unspecified,
                        contentDescription = null,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    tint = Color.Unspecified,
                    contentDescription = null,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = balance.numberFormat2(),
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.W400,
                    fontFamily = FontFamily(Font(R.font.mixin_font))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("USD", color = MixinAppTheme.colors.textAssist, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (assets.isNotEmpty()) {
                Distribution(assets, destination = destination)
            } else {
                if (destination == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        privacyChain.forEachIndexed { index, iconRes ->
                            Image(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(18.dp)
                                    .offset(x = (-6 * index).dp)
                                    .border(1.dp, MixinAppTheme.colors.background, CircleShape)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        classicChain.forEachIndexed { index, iconRes ->
                            Image(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(18.dp)
                                    .offset(x = (-6 * index).dp)
                                    .border(1.dp, MixinAppTheme.colors.background, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

val privacyChain = listOf(
    R.drawable.ic_chain_btc,
    R.drawable.ic_chain_eth,
    R.drawable.ic_chain_sol,
    R.drawable.ic_chain_polygon,
    R.drawable.ic_chain_xmr,
    R.drawable.ic_chain_xrp,
    R.drawable.ic_chain_doge,
    R.drawable.ic_chain_bsc,
    R.drawable.ic_chain_ton,
    R.drawable.ic_chain_base,
)

val classicChain = listOf(
    R.drawable.ic_chain_eth,
    R.drawable.ic_chain_polygon,
    R.drawable.ic_chain_bsc,
    R.drawable.ic_chain_base,
    R.drawable.ic_chain_arbitrum_eth,
    R.drawable.ic_chain_blast,
    R.drawable.ic_chain_sol,
)