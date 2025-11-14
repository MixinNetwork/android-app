package one.mixin.android.ui.wallet.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.reactivex.android.schedulers.AndroidSchedulers
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.event.WalletRefreshedEvent
import one.mixin.android.extension.numberFormat2
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.WalletCategory
import java.math.BigDecimal

@Composable
fun WalletCard(
    name: String? = null,
    hasLocalPrivateKey: Boolean = true,
    destination: WalletDestination?,
    onClick: () -> Unit,
    enableFreeLabel: Boolean = false,
    viewModel: AssetDistributionViewModel = hiltViewModel(),
) {
    var web3TokenTotalBalance by remember { mutableStateOf<BigDecimal?>(null) }
    var tokenTotalBalance by remember { mutableStateOf<BigDecimal?>(null) }
    var assets by remember { mutableStateOf<List<AssetDistribution>?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, destination) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        val disposable = RxBus.listen(WalletRefreshedEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event ->
                val walletId = when (destination) {
                    is WalletDestination.Classic -> destination.walletId
                    is WalletDestination.Import -> destination.walletId
                    is WalletDestination.Watch -> destination.walletId
                    else -> null
                }
                if (walletId == null) {
                    refreshTrigger++
                } else if (event.walletId == walletId) {
                    refreshTrigger++
                }
            }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            disposable.dispose()
        }
    }

    LaunchedEffect(refreshTrigger) {
        when (destination) {
            is WalletDestination.Privacy -> {
                tokenTotalBalance = viewModel.getTokenTotalBalance(excludeWeb3 = true)
                assets = viewModel.getTokenDistribution(excludeWeb3 = true)
            }

            is WalletDestination.Classic -> {
                web3TokenTotalBalance = viewModel.getWeb3TokenTotalBalance(destination.walletId)
                assets = viewModel.getWeb3TokenDistribution(destination.walletId)
            }

            is WalletDestination.Import -> {
                web3TokenTotalBalance = viewModel.getWeb3TokenTotalBalance(destination.walletId)
                assets = viewModel.getWeb3TokenDistribution(destination.walletId)
            }

            is WalletDestination.Watch -> {
                web3TokenTotalBalance = viewModel.getWeb3TokenTotalBalance(destination.walletId)
                assets = viewModel.getWeb3TokenDistribution(destination.walletId)
            }

            else -> {
                tokenTotalBalance = viewModel.getTokenTotalBalance()
                assets = viewModel.getTokenDistribution()
            }
        }
    }

    val balance = if (destination is WalletDestination.Privacy) {
        tokenTotalBalance
    } else {
        web3TokenTotalBalance
    }
    if (balance != null) {
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
                        name
                            ?: if (destination is WalletDestination.Privacy) stringResource(R.string.Privacy_Wallet) else stringResource(
                                R.string.Common_Wallet
                            ),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W500,
                        color = MixinAppTheme.colors.textPrimary,
                        maxLines = 1,
                        modifier = Modifier.widthIn(max = 240.dp),
                        overflow = TextOverflow.Ellipsis
                    )
                    if (destination is WalletDestination.Privacy) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            modifier = Modifier.size(18.dp),
                            painter = painterResource(id = R.drawable.ic_wallet_privacy),
                            tint = Color.Unspecified,
                            contentDescription = null,
                        )
                    } else if (destination is WalletDestination.Import) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.Imported),
                            color = MixinAppTheme.colors.textRemarks,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .background(
                                    color = MixinAppTheme.colors.backgroundGrayLight,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp)
                        )
                        if (!hasLocalPrivateKey) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.NoKey),
                                color = MixinAppTheme.colors.red,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .background(
                                        color = MixinAppTheme.colors.red.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp)
                            )

                        }
                    } else if (destination is WalletDestination.Watch) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            modifier = Modifier.size(18.dp),
                            painter = painterResource(id = R.drawable.ic_wallet_watch),
                            tint = Color.Unspecified,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.Watching),
                            color = MixinAppTheme.colors.textRemarks,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .background(
                                    color = MixinAppTheme.colors.backgroundGrayLight,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp)
                        )
                    }

                    val isFeeFree = enableFreeLabel && when (destination) {
                        is WalletDestination.Classic -> true
                        is WalletDestination.Import -> hasLocalPrivateKey
                        else -> false
                    }
                    if (isFeeFree) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.FREE),
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .background(
                                    color = MixinAppTheme.colors.accent,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp)
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
                        text = balance.multiply(Fiats.getRate().toBigDecimal()).numberFormat2(),
                        color = MixinAppTheme.colors.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.W400,
                        fontFamily = FontFamily(Font(R.font.mixin_font))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Fiats.getAccountCurrencyAppearance(), color = MixinAppTheme.colors.textAssist, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (assets == null) {
                    Spacer(modifier = Modifier.height(18.dp))
                } else if (assets.isNullOrEmpty().not()) {
                    Distribution(assets!!, destination = destination)
                } else {
                    var chains by remember(destination) { mutableStateOf<List<Int>?>(null) }
                    LaunchedEffect(refreshTrigger) {
                        chains = if (destination is WalletDestination.Privacy || destination == null) {
                            privacyChain
                        } else if ((destination is WalletDestination.Watch && (destination.category == WalletCategory.WATCH_ADDRESS.value || destination.category == WalletCategory.IMPORTED_PRIVATE_KEY.value)) ||
                            (destination is WalletDestination.Import && (destination.category == WalletCategory.WATCH_ADDRESS.value || destination.category == WalletCategory.IMPORTED_PRIVATE_KEY.value))
                        ) {
                            val walletId = if (destination is WalletDestination.Watch) destination.walletId else (destination as WalletDestination.Import).walletId
                            val address = viewModel.getAddresses(walletId)
                            if (address.any { it.chainId == Constants.ChainId.SOLANA_CHAIN_ID }) {
                                listOf(R.drawable.ic_chain_sol)
                            } else {
                                listOf(
                                    R.drawable.ic_chain_eth,
                                    R.drawable.ic_chain_polygon,
                                    R.drawable.ic_chain_bsc,
                                    R.drawable.ic_chain_base,
                                    R.drawable.ic_chain_arbitrum_eth,
                                    R.drawable.ic_chain_optimism,
                                )
                            }
                        } else {
                            classicChain
                        }
                    }

                    if (chains != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            chains!!.forEachIndexed { index, iconRes ->
                                Image(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(19.dp)
                                        .offset(x = (-6 * index).dp)
                                        .clip(CircleShape)
                                        .border(1.dp, MixinAppTheme.colors.background, CircleShape)
                                        .padding(0.5.dp)
                                )
                            }
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
    R.drawable.ic_chain_arbitrum_eth,
    R.drawable.ic_chain_optimism,
)

val classicChain = listOf(
    R.drawable.ic_chain_eth,
    R.drawable.ic_chain_polygon,
    R.drawable.ic_chain_bsc,
    R.drawable.ic_chain_base,
    R.drawable.ic_chain_arbitrum_eth,
    R.drawable.ic_chain_optimism,
    // R.drawable.ic_chain_blast,
    R.drawable.ic_chain_sol,
)
