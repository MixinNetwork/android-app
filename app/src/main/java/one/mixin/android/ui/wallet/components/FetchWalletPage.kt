package one.mixin.android.ui.wallet.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.api.response.AssetView
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.crypto.CryptoWallet
import one.mixin.android.extension.priceFormat
import one.mixin.android.ui.wallet.alert.components.cardBackground
import java.math.BigDecimal
import java.text.NumberFormat

enum class FetchWalletState {
    FETCHING,
    SELECT,
    IMPORTING,
    IMPORT_ERROR,
    IMPORT_SUCCESS
}

data class AssetInfo(
    val symbol: String,
    val iconUrl: String,
    val value: Double
)

data class IndexedWallet(
    val index: Int,
    val ethereumWallet: CryptoWallet,
    val solanaWallet: CryptoWallet,
    val assets: List<AssetView> = emptyList(),
    val exists: Boolean
) {
    val totalValue: BigDecimal
        get() = assets.sumOf {
            (it.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) * (it.priceUSD.toBigDecimalOrNull()
                ?: BigDecimal.ZERO)
        }
}

@Composable
private fun LoadingState(title: String, subtitle: String) {
    MixinAppTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(120.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_wallet_fetching),
                contentDescription = null,
                tint = Color.Unspecified,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MixinAppTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist
            )


            Spacer(modifier = Modifier.weight(1f))
            CircularProgressIndicator(
                modifier = Modifier.size(30.dp),
                color = MixinAppTheme.colors.backgroundGray
            )
            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}

@Composable
fun FetchingContent() {
    LoadingState(
        title = stringResource(R.string.fetching_in_to_your_wallet),
        subtitle = stringResource(R.string.fetching_shouldnt_take_long)
    )
}

@Composable
fun SelectContent(
    wallets: List<IndexedWallet>,
    selectedWalletInfos: Set<IndexedWallet>,
    onWalletToggle: (IndexedWallet) -> Unit,
    onContinue: (Set<IndexedWallet>) -> Unit,
    onBackPressed: () -> Unit,
    onSelectAll: () -> Unit,
    onFindMore: () -> Unit,
    isLoadingMore: Boolean,
) {
    MixinAppTheme {
        val selectableWallets = wallets.filter { !it.exists }
        val isAllSelected = selectableWallets.isNotEmpty() && selectedWalletInfos.containsAll(selectableWallets)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Row {
                Text(
                    text = pluralStringResource(
                        R.plurals.items_selected,
                        selectedWalletInfos.size,
                        selectedWalletInfos.size
                    ),
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = stringResource(
                        if (isAllSelected) R.string.Deselect_all else R.string.Select_all
                    ),
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.accent,
                    modifier = Modifier.clickable { onSelectAll() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = wallets, key = { it.index }) { wallet ->
                    WalletItem(
                        wallet = wallet,
                        isSelected = selectedWalletInfos.contains(wallet),
                        onToggle = { onWalletToggle(wallet) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isLoadingMore) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(30.dp),
                                color = MixinAppTheme.colors.backgroundGray
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(id = R.string.find_more_wallets),
                            color = MixinAppTheme.colors.accent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFindMore() }
                                .padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp)
            ) {
                Button(
                    onClick = { onContinue(selectedWalletInfos) },
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            backgroundColor = if (selectedWalletInfos.isNotEmpty()) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGrayLight,
                        ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    enabled = selectedWalletInfos.isNotEmpty(),
                    elevation =
                        ButtonDefaults.elevation(
                            pressedElevation = 0.dp,
                            defaultElevation = 0.dp,
                            hoveredElevation = 0.dp,
                            focusedElevation = 0.dp,
                        ),
                ) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.import_wallets,
                            selectedWalletInfos.size,
                            selectedWalletInfos.size
                        ),
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun ImportingContent(onFinished: () -> Unit) {
    MixinAppTheme {
        LoadingState(
            title = stringResource(R.string.importing_into_your_wallet),
            subtitle = stringResource(R.string.fetching_shouldnt_take_long)
        )
    }
}

@Composable
fun ImportErrorContent(
    partialSuccess: Boolean? = null,
    errorCode: Int? = null,
    errorMessage: String? = null,
    onUpgradePlan: () -> Unit,
    onNotNow: () -> Unit
) {
    val context = LocalContext.current
    MixinAppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(132.dp))

            Icon(
                painter = painterResource(id = R.drawable.ic_wallet_warning),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (partialSuccess == true) {
                    stringResource(R.string.partial_import_successful)
                } else {
                    stringResource(R.string.Import_Failed)
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MixinAppTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (errorCode == 10632) {
                    stringResource(R.string.error_too_many_wallets)
                } else {
                    errorMessage ?: stringResource(R.string.Import_Failed)
                },
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            if (errorCode == 10632) {
                Button(
                    onClick = onUpgradePlan,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MixinAppTheme.colors.accent
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    elevation = ButtonDefaults.elevation(
                        pressedElevation = 0.dp,
                        defaultElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        focusedElevation = 0.dp,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.Upgrade_Plan),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onNotNow,
                ) {
                    Text(
                        text = stringResource(id = R.string.Done),
                        color = MixinAppTheme.colors.accent
                    )
                }
            } else {
                TextButton(
                    onClick = onNotNow,
                ) {
                    Text(
                        text = stringResource(id = R.string.Done),
                        color = MixinAppTheme.colors.accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun WalletItem(
    wallet: IndexedWallet,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(
                backgroundColor = MixinAppTheme.colors.background,
                borderColor = MixinAppTheme.colors.borderColor
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = if (wallet.exists) {
                    {}
                } else {
                    onToggle
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${stringResource(R.string.Common_Wallet)} ${wallet.index + 1}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MixinAppTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.weight(1f))

            val iconRes = when {
                wallet.exists -> R.drawable.ic_wallet_disabled
                isSelected -> R.drawable.ic_wallet_select
                else -> R.drawable.ic_wallet_unselect
            }
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = Color.Unspecified
            )

        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = wallet.totalValue.priceFormat(),
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.W400,
                fontFamily = FontFamily(Font(R.font.mixin_font))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("USD", color = MixinAppTheme.colors.textAssist, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (wallet.assets.isEmpty()) {
                Row {
                    classicChain.forEachIndexed { index, iconRes ->
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .size(18.dp)
                                .offset(x = (-8 * index).dp)
                        )
                    }
                }
            } else {
                val totalValue = wallet.totalValue
                wallet.assets.take(2).sortedBy { it.value }.forEach { asset ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CoilImage(
                            model = asset.iconUrl,
                            placeholder = R.drawable.ic_avatar_place_holder,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        if (totalValue > BigDecimal.ZERO) {
                            val percentage = (asset.value / totalValue) * BigDecimal(100)
                            Text(
                                text = "${
                                    NumberFormat.getPercentInstance()
                                        .format(percentage.toDouble() / 100)
                                }",
                                fontSize = 12.sp,
                                color = MixinAppTheme.colors.textMinor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
