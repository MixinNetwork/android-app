package one.mixin.android.ui.wallet

import android.content.Context
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.db.web3.vo.isImported
import one.mixin.android.db.web3.vo.isWatch
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.common.NoKeyWarningBottomSheetDialogFragment
import one.mixin.android.ui.wallet.components.KEY_HIDE_COMMON_WALLET_INFO
import one.mixin.android.ui.wallet.components.KEY_HIDE_PRIVACY_WALLET_INFO
import one.mixin.android.ui.wallet.components.PREF_NAME
import one.mixin.android.ui.wallet.components.WalletCard
import one.mixin.android.ui.wallet.components.WalletDestination
import one.mixin.android.ui.wallet.components.WalletInfoCard
import one.mixin.android.ui.home.web3.components.ActionBottom

@AndroidEntryPoint
class WalletMultiSelectBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {

    private val viewModel by viewModels<WalletViewModel>()
    private var onConfirmListener: ((List<Web3Wallet?>) -> Unit)? = null
    private var onDismissListener: (() -> Unit)? = null

    private var initialSelectedIds: List<String> = emptyList()
    private var initialPrivacySelected: Boolean = false

    override fun getTheme() = R.style.AppTheme_Dialog

    @OptIn(FlowPreview::class)
    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            val searchQuery = remember { MutableStateFlow("") }
            val wallets by viewModel.walletsFlow.collectAsState()
            val scope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                // initial load: show all wallets (privacy + all web3 wallets)
                viewModel.searchWallets("", "", "")
                searchQuery.debounce(150).collect { query ->
                    viewModel.searchWallets("", "", query)
                }
            }

            WalletMultiSelectScreen(
                wallets = wallets.filter { it.isWatch().not() },
                initialSelectedIds = initialSelectedIds,
                initialPrivacySelected = initialPrivacySelected,
                onQueryChanged = { q -> scope.launch { searchQuery.emit(q) } },
                onConfirm = { selected ->
                    onConfirmListener?.invoke(selected)
                    dismiss()
                },
                onReset = {
                    onConfirmListener?.invoke(emptyList())
                    dismiss()
                },
                onShowNoKeyWarning = { wallet, onProceed ->
                    NoKeyWarningBottomSheetDialogFragment.newInstance(wallet).apply {
                        onConfirm = onProceed
                    }.show(parentFragmentManager, NoKeyWarningBottomSheetDialogFragment.TAG)
                },
                onCancel = { dismiss() }
            )
        }
    }

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop()
    }

    fun setOnConfirmListener(listener: (List<Web3Wallet?>) -> Unit): WalletMultiSelectBottomSheetDialogFragment {
        onConfirmListener = listener
        return this
    }

    fun setInitialSelection(selectedIds: List<String>, privacySelected: Boolean): WalletMultiSelectBottomSheetDialogFragment {
        initialSelectedIds = selectedIds
        initialPrivacySelected = privacySelected
        return this
    }

    fun setOnDismissListener(listener: () -> Unit): WalletMultiSelectBottomSheetDialogFragment {
        onDismissListener = listener
        return this
    }

    override fun showError(error: String) {}

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.invoke()
    }

    companion object {
        const val TAG = "WalletMultiSelectBottomSheetDialogFragment"
        fun newInstance(): WalletMultiSelectBottomSheetDialogFragment {
            return WalletMultiSelectBottomSheetDialogFragment().withArgs { }
        }
    }
}

@Composable
private fun WalletMultiSelectScreen(
    wallets: List<Web3Wallet>,
    initialSelectedIds: List<String>,
    initialPrivacySelected: Boolean,
    onQueryChanged: (String) -> Unit,
    onConfirm: (List<Web3Wallet?>) -> Unit,
    onReset: () -> Unit,
    onShowNoKeyWarning: (Web3Wallet, () -> Unit) -> Unit,
    onCancel: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) }
    var query by remember { mutableStateOf("") }
    val hidePrivacyWalletInfo = remember { mutableStateOf(prefs.getBoolean(KEY_HIDE_PRIVACY_WALLET_INFO, false)) }
    val hideCommonWalletInfo = remember { mutableStateOf(prefs.getBoolean(KEY_HIDE_COMMON_WALLET_INFO, false)) }

    val selectedWalletIds = remember { mutableStateListOf<String>().apply { addAll(initialSelectedIds) } }
    var privacySelected by remember { mutableStateOf(initialPrivacySelected) }

    val walletItems = remember(wallets, query) {
        wallets
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = query,
            onQueryChanged = {
                query = it
                onQueryChanged(it)
            },
            isSearching = false,
            onCancel = onCancel,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        Column(
            modifier = Modifier
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Privacy wallet row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MixinAppTheme.colors.background, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WalletCard(
                    name = stringResource(id = R.string.Privacy_Wallet),
                    destination = WalletDestination.Privacy,
                    onClick = { privacySelected = !privacySelected },
                    isSelectable = true,
                    isSelected = privacySelected,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Web3 wallets
            walletItems.forEachIndexed { index, wallet ->
                val checked = selectedWalletIds.contains(wallet.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MixinAppTheme.colors.background, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (wallet.isImported()) {
                        val destination = WalletDestination.Import(wallet.id, wallet.category)
                        WalletCard(
                            name = wallet.name,
                            destination = destination,
                            hasLocalPrivateKey = wallet.hasLocalPrivateKey,
                            onClick = {
                                if (wallet.isWatch() || (wallet.isImported() && !wallet.hasLocalPrivateKey)) {
                                    onShowNoKeyWarning(wallet) {
                                        toggleSelection(selectedWalletIds, wallet.id)
                                    }
                                } else {
                                    toggleSelection(selectedWalletIds, wallet.id)
                                }
                            },
                            isSelectable = true,
                            isSelected = checked,
                        )
                    } else if (wallet.isWatch()) {
                        val destination = WalletDestination.Watch(wallet.id, wallet.category)
                        WalletCard(
                            name = wallet.name,
                            destination = destination,
                            onClick = {
                                if (wallet.isWatch() || (wallet.isImported() && !wallet.hasLocalPrivateKey)) {
                                    onShowNoKeyWarning(wallet) {
                                        toggleSelection(selectedWalletIds, wallet.id)
                                    }
                                } else {
                                    toggleSelection(selectedWalletIds, wallet.id)
                                }
                            },
                            isSelectable = true,
                            isSelected = checked,
                        )
                    } else {
                        val destination = WalletDestination.Classic(wallet.id)
                        WalletCard(
                            name = wallet.name,
                            destination = destination,
                            onClick = {
                                if (wallet.isWatch() || (wallet.isImported() && !wallet.hasLocalPrivateKey)) {
                                    onShowNoKeyWarning(wallet) {
                                        toggleSelection(selectedWalletIds, wallet.id)
                                    }
                                } else {
                                    toggleSelection(selectedWalletIds, wallet.id)
                                }
                            },
                            isSelectable = true,
                            isSelected = checked,
                        )
                    }
                }

                if (index < walletItems.size - 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            if (!hidePrivacyWalletInfo.value || !hideCommonWalletInfo.value) {
                Spacer(modifier = Modifier.height(10.dp))
                WalletInfoCard(
                    hidePrivacyWalletInfo = hidePrivacyWalletInfo.value,
                    hideCommonWalletInfo = hideCommonWalletInfo.value,
                    onPrivacyClose = {
                        hidePrivacyWalletInfo.value = true
                        prefs.edit().putBoolean(KEY_HIDE_PRIVACY_WALLET_INFO, true).apply()
                    },
                    onCommonClose = {
                        hideCommonWalletInfo.value = true
                        prefs.edit().putBoolean(KEY_HIDE_COMMON_WALLET_INFO, true).apply()
                    }
                )
            }
        }

        // Action buttons
        ActionBottom(
            modifier = Modifier
                .fillMaxWidth()
                .background(MixinAppTheme.colors.background)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            cancelTitle = stringResource(id = R.string.Reset),
            confirmTitle = stringResource(id = R.string.Apply),
            cancelAction = onReset,
            confirmAction = {
                val selected = buildList<Web3Wallet?> {
                    if (privacySelected) add(null)
                    selectedWalletIds.forEach { id ->
                        wallets.find { it.id == id }?.let { add(it) }
                    }
                }
                onConfirm(selected)
            }
        )
    }
}

private fun toggleSelection(selected: MutableList<String>, id: String) {
    if (selected.contains(id)) selected.remove(id) else selected.add(id)
}
