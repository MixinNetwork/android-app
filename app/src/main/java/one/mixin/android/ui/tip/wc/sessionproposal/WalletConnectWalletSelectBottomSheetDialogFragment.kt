package one.mixin.android.ui.tip.wc.sessionproposal

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.web3.vo.isClassic
import one.mixin.android.db.web3.vo.isImported
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.withArgs
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.tip.wc.WalletConnectV2.getProposalChainIds
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.wallet.SearchBar
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.ui.wallet.components.WalletCategoryFilter
import one.mixin.android.vo.WalletCategory
import one.mixin.android.widget.bottomSheetHeight

@AndroidEntryPoint
class WalletConnectWalletSelectBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    override val centerOnExpandedScreen = true

    private val viewModel by viewModels<SessionProposalViewModel>()

    private val topic: String? by lazy { arguments?.getString(ARGS_TOPIC) }
    private val initialSelectedWalletId: String by lazy { arguments?.getString(ARGS_SELECTED_WALLET_ID).orEmpty() }

    override fun getTheme() = R.style.AppTheme_Dialog

    @OptIn(FlowPreview::class)
    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            val searchQueryFlow = remember { MutableStateFlow("") }
            var query by remember { mutableStateOf("") }
            var allWallets by remember { mutableStateOf<List<WalletConnectSelectableWallet>>(emptyList()) }
            var filteredWallets by remember { mutableStateOf<List<WalletConnectSelectableWallet>>(emptyList()) }
            val proposalChainIds = remember(topic) {
                topic?.takeIf { it.isNotBlank() }?.let { WalletConnectV2.getSessionProposal(it)?.getProposalChainIds() }
            }

            LaunchedEffect(topic) {
                val initial = viewModel.loadSelectableWallets(topic, "")
                allWallets = initial
                filteredWallets = initial
            }

            LaunchedEffect(topic) {
                searchQueryFlow.collect { q ->
                    if (q.isEmpty()) {
                        filteredWallets = viewModel.loadSelectableWallets(topic, "")
                    }
                }
            }

            LaunchedEffect(topic) {
                searchQueryFlow
                    .debounce(150)
                    .collect { q ->
                        if (q.isNotEmpty()) {
                            filteredWallets = viewModel.loadSelectableWallets(topic, q)
                        }
                    }
            }

            WalletConnectWalletSelectScreen(
                allWallets = allWallets,
                wallets = filteredWallets,
                selectedWalletId = initialSelectedWalletId,
                proposalChainIds = proposalChainIds,
                query = query,
                onQueryChanged = { newQuery ->
                    query = newQuery
                    searchQueryFlow.tryEmit(newQuery)
                },
                onWalletClick = { walletId ->
                    setFragmentResult(
                        REQUEST_KEY_WALLET_SELECT,
                        Bundle().apply { putString(RESULT_WALLET_ID, walletId) },
                    )
                    dismissAllowingStateLoss()
                },
                onCancel = { dismissAllowingStateLoss() },
            )
        }
    }

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().bottomSheetHeight() - view.getSafeAreaInsetsTop()
    }

    override fun showError(error: String) {
    }

    companion object {
        const val TAG = "WalletConnectWalletSelectBottomSheetDialogFragment"
        const val REQUEST_KEY_WALLET_SELECT = "request_key_wc_wallet_select"
        const val RESULT_WALLET_ID = "result_wallet_id"
        private const val ARGS_TOPIC = "args_topic"
        private const val ARGS_SELECTED_WALLET_ID = "args_selected_wallet_id"

        fun newInstance(
            topic: String?,
            selectedWalletId: String,
        ): WalletConnectWalletSelectBottomSheetDialogFragment =
            WalletConnectWalletSelectBottomSheetDialogFragment().withArgs {
                putString(ARGS_TOPIC, topic)
                putString(ARGS_SELECTED_WALLET_ID, selectedWalletId)
            }
    }
}

@Composable
private fun WalletConnectWalletSelectScreen(
    allWallets: List<WalletConnectSelectableWallet>,
    wallets: List<WalletConnectSelectableWallet>,
    selectedWalletId: String,
    proposalChainIds: Set<String>?,
    query: String,
    onQueryChanged: (String) -> Unit,
    onWalletClick: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val hasCreated = remember(allWallets) { allWallets.any { it.wallet.isClassic() } }
    val hasImported = remember(allWallets) { allWallets.any { it.wallet.isImported() } }
    var selectedCategory by remember(hasCreated, hasImported) {
        mutableStateOf<String?>(null)
    }

    val displayedWallets =
        remember(wallets, selectedCategory) {
            wallets.filter { item ->
                when (selectedCategory) {
                    WalletCategory.CLASSIC.value -> item.wallet.isClassic()
                    "import" -> item.wallet.isImported()
                    else -> true
                }
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MixinAppTheme.colors.background),
    ) {
        SearchBar(
            query = query,
            onQueryChanged = onQueryChanged,
            isSearching = false,
            onCancel = onCancel,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            placeholderResId = R.string.search_placeholder_wallet,
        )

        Column(
            modifier =
                Modifier
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
        ) {
            if (hasCreated || hasImported) {
                WalletCategoryFilter(
                    selectedCategory = selectedCategory,
                    hasAll = true,
                    hasImported = hasImported,
                    hasWatch = false,
                    hasSafe = false,
                    hasCreated = hasCreated,
                    onCategorySelected = { selectedCategory = it },
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                displayedWallets.forEach { item ->
                    WalletConnectSelectCard(
                        item = item,
                        isSelected = item.wallet.id == selectedWalletId,
                        proposalChainIds = proposalChainIds,
                        onClick = { onWalletClick(item.wallet.id) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun WalletConnectSelectCard(
    item: WalletConnectSelectableWallet,
    isSelected: Boolean,
    proposalChainIds: Set<String>?,
    onClick: () -> Unit,
) {
    val wallet = item.wallet
    val displayName = wallet.name.takeIf { it.isNotBlank() } ?: stringResource(R.string.Common_Wallet)

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .cardBackground(
                    backgroundColor = MixinAppTheme.colors.background,
                    borderColor = if (isSelected) MixinAppTheme.colors.accent else MixinAppTheme.colors.borderColor,
                    borderWidth = if (isSelected) 1.dp else 0.8.dp,
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current,
                    onClick = onClick,
                ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = displayName,
                        color = MixinAppTheme.colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W500,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (wallet.isImported()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        WalletLabelBadge(
                            text = stringResource(R.string.Imported),
                            textColor = MixinAppTheme.colors.textRemarks,
                            backgroundColor = MixinAppTheme.colors.backgroundGrayLight,
                        )
                    }
                    if (item.isConnected) {
                        Spacer(modifier = Modifier.width(4.dp))
                        WalletLabelBadge(
                            text = stringResource(R.string.Connected),
                            textColor = Color.White,
                            backgroundColor = Color(0xFF50BD5C),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = painterResource(id = if (isSelected) R.drawable.ic_wallet_select else R.drawable.ic_wallet_unselect),
                    tint = Color.Unspecified,
                    contentDescription = null,
                    modifier = Modifier.requiredSize(16.dp),
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            WalletBalanceRow(balance = item.balance)
            Spacer(modifier = Modifier.height(12.dp))
            WalletChainAddressRows(
                addresses = item.addresses,
                proposalChainIds = proposalChainIds,
            )
        }
    }
}
