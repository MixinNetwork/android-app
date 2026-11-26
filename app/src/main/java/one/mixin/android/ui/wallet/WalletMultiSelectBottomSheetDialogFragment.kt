package one.mixin.android.ui.wallet

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.web3.vo.WalletItem
import one.mixin.android.db.web3.vo.isImported
import one.mixin.android.db.web3.vo.isMixinSafe
import one.mixin.android.db.web3.vo.isWatch
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.components.ActionBottom
import one.mixin.android.ui.wallet.components.WalletCard
import one.mixin.android.ui.wallet.components.WalletDestination
import one.mixin.android.widget.bottomSheetHeight

@AndroidEntryPoint
class WalletMultiSelectBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    override val centerOnExpandedScreen = true


    private val viewModel by viewModels<WalletViewModel>()
    private var onConfirmListener: ((List<WalletItem?>) -> Unit)? = null
    private var onDismissListener: (() -> Unit)? = null

    private var initialSelectedIds: List<String> = emptyList()
    private var initialPrivacySelected: Boolean = false

    override fun getTheme() = R.style.AppTheme_Dialog

    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            var query by rememberSaveable { mutableStateOf("") }
            val wallets by viewModel.walletsFlow.collectAsState()
            LaunchedEffect(query) {
                if (query.isNotEmpty()) {
                    delay(150)
                }
                viewModel.searchWallets("", "", query)
            }

            val filteredWallets = remember(wallets) { wallets.filter { !it.isWatch() && !it.isMixinSafe() } }
            WalletMultiSelectScreen(
                wallets = filteredWallets,
                initialSelectedIds = initialSelectedIds,
                initialPrivacySelected = initialPrivacySelected,
                query = query,
                onQueryChanged = { query = it },
                onConfirm = { selected ->
                    onConfirmListener?.invoke(selected)
                    dismiss()
                },
                onReset = {
                    onConfirmListener?.invoke(emptyList())
                    dismiss()
                },
                onCancel = { dismiss() }
            )
        }
    }

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().bottomSheetHeight() - view.getSafeAreaInsetsTop()
    }

    fun setOnConfirmListener(listener: (List<WalletItem?>) -> Unit): WalletMultiSelectBottomSheetDialogFragment {
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
    wallets: List<WalletItem>,
    initialSelectedIds: List<String>,
    initialPrivacySelected: Boolean,
    query: String,
    onQueryChanged: (String) -> Unit,
    onConfirm: (List<WalletItem?>) -> Unit,
    onReset: () -> Unit,
    onCancel: () -> Unit,
) {
    var selectedWalletIds by rememberSaveable(
        saver = Saver(
            save = { ArrayList(it.value) },
            restore = { mutableStateOf(it.toSet()) },
        ),
    ) {
        mutableStateOf(initialSelectedIds.toSet())
    }
    var privacySelected by rememberSaveable { mutableStateOf(initialPrivacySelected) }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = query,
            onQueryChanged = onQueryChanged,
            isSearching = false,
            onCancel = onCancel,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Privacy wallet row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MixinAppTheme.colors.background, RoundedCornerShape(8.dp)),
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
            wallets.forEachIndexed { index, wallet ->
                val checked = wallet.id in selectedWalletIds
                val toggle = {
                    selectedWalletIds = if (wallet.id in selectedWalletIds) selectedWalletIds - wallet.id else selectedWalletIds + wallet.id
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MixinAppTheme.colors.background, RoundedCornerShape(8.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (wallet.isImported()) {
                        val destination = WalletDestination.Import(wallet.id, wallet.category)
                        WalletCard(
                            name = wallet.name,
                            destination = destination,
                            hasLocalPrivateKey = wallet.hasLocalPrivateKey,
                            onClick = toggle,
                            isSelectable = true,
                            isSelected = checked,
                        )
                    } else if (wallet.isWatch()) {
                        val destination = WalletDestination.Watch(wallet.id, wallet.category)
                        WalletCard(
                            name = wallet.name,
                            destination = destination,
                            onClick = toggle,
                            isSelectable = true,
                            isSelected = checked,
                        )
                    } else {
                        val destination = WalletDestination.Classic(wallet.id)
                        WalletCard(
                            name = wallet.name,
                            destination = destination,
                            onClick = toggle,
                            isSelectable = true,
                            isSelected = checked,
                        )
                    }
                }

                if (index < wallets.size - 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
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
                val selected: List<WalletItem?> = buildList {
                    if (privacySelected) add(null)
                    selectedWalletIds.forEach { id ->
                        wallets.find { it.id == id }?.let { wallet ->
                            add(wallet)
                        }
                    }
                }
                onConfirm(selected)
            }
        )
    }
}
