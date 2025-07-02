package one.mixin.android.ui.wallet.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ripple.rememberRipple
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.ui.wallet.viewmodel.FetchWalletViewModel

enum class FetchWalletState {
    FETCHING,
    SELECT,
    IMPORTING
}

data class WalletInfo(
    val index: Int,
    val address: String,
    val balance: String = "0",
)

@Composable
fun FetchWalletPage(
    mnemonic: String? = null,
    onBackPressed: () -> Unit,
    viewModel: FetchWalletViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val selectedWallets by viewModel.selectedWallets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(mnemonic) {
        if (!mnemonic.isNullOrEmpty()) {
            viewModel.setMnemonic(mnemonic)
        }
    }

    MixinAppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (state) {
                FetchWalletState.FETCHING -> {
                    FetchingContent(
                        onCancel = onBackPressed
                    )
                }

                FetchWalletState.SELECT -> {
                    SelectContent(
                        wallets = wallets,
                        selectedWallets = selectedWallets,
                        onWalletToggle = viewModel::toggleWalletSelection,
                        onContinue = viewModel::startImporting,
                        onBackPressed = onBackPressed
                    )
                }

                FetchWalletState.IMPORTING -> {
                    ImportingContent(
                        selectedWallets = selectedWallets,
                        isLoading = isLoading,
                        onComplete = onBackPressed
                    )
                }
            }
        }
    }
}

@Composable
fun FetchingContent(
    onCancel: () -> Unit
) {
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
        Text(
            text = stringResource(R.string.fetching_into_wallet),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MixinAppTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.fetching_shouldnt_take_long),
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

@Composable
fun SelectContent(
    wallets: List<WalletInfo>,
    selectedWallets: Set<Int>,
    onWalletToggle: (Int) -> Unit,
    onContinue: () -> Unit,
    onBackPressed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row {
            Text(
                text = pluralStringResource(
                    R.plurals.wallets_selected,
                    selectedWallets.size,
                    selectedWallets.size
                ),
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.Select_all),
                fontSize = 14.sp,
                color = MixinAppTheme.colors.accent
            )
        }


        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(wallets) { wallet ->
                WalletItem(
                    wallet = wallet,
                    isSelected = selectedWallets.contains(wallet.index),
                    onToggle = { onWalletToggle(wallet.index) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp)
        ) {
            Button(
                onClick = {

                },
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        backgroundColor = if (selectedWallets.isNotEmpty()) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGrayLight,
                    ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                enabled = selectedWallets.isNotEmpty(),
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
                        selectedWallets.size,
                        selectedWallets.size
                    ),
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun ImportingContent(
    selectedWallets: Set<Int>,
    isLoading: Boolean,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MixinAppTheme.colors.accent
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Importing wallets...",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MixinAppTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Importing ${selectedWallets.size} wallet addresses",
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist
            )
        } else {
            Text(
                text = "Import Complete!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MixinAppTheme.colors.accent
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Successfully imported ${selectedWallets.size} wallet addresses",
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onComplete,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MixinAppTheme.colors.accent
                )
            ) {
                Text(
                    text = "Done",
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun WalletItem(
    wallet: WalletInfo,
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
                indication = rememberRipple(color = MixinAppTheme.colors.accent)
            ) { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${stringResource(R.string.Common_Wallet)} ${wallet.index}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MixinAppTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            val iconRes =
                if (isSelected) R.drawable.ic_sticker_checked else R.drawable.ic_sticker_unchecked
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = Color.Unspecified
            )

        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = wallet.address,
                fontSize = 12.sp,
                color = MixinAppTheme.colors.textMinor,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )

            if (wallet.hasTransactions) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Balance: ${wallet.balance} SOL",
                    fontSize = 12.sp,
                    color = MixinAppTheme.colors.textAssist
                )
            }
        }
    }
}
