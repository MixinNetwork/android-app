package one.mixin.android.ui.wallet

import android.content.Context
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.compose.GetNavBarHeightValue
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

@AndroidEntryPoint
class WalletListBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {

    private val viewModel by viewModels<WalletViewModel>()
    private var onWalletClickListener: ((Web3Wallet?) -> Unit)? = null

    private val excludeWalletId: String? by lazy {
        requireArguments().getString(ARGS_EXCLUDE_WALLET_ID)
    }

    private val chainId: String by lazy {
        requireNotNull(requireArguments().getString(ARGS_CHAIN_ID))
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    @OptIn(FlowPreview::class)
    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            val searchQuery = remember { MutableStateFlow("") }
            val wallets by viewModel.walletsFlow.collectAsState()
            LaunchedEffect(Unit) {
                launch {
                    searchQuery.collect { query ->
                        if (query.isEmpty()) {
                            viewModel.searchWallets(excludeWalletId ?: "", chainId, query)
                        }
                    }
                }
                launch {
                    searchQuery
                        .debounce(150)
                        .collect { query ->
                            if (query.isNotEmpty()) {
                                viewModel.searchWallets(excludeWalletId ?: "", chainId, query)
                            }
                        }
                }
            }

            WalletListScreen(
                wallets = wallets,
                excludeWalletId = excludeWalletId,
                onQueryChanged = { query ->
                    lifecycleScope.launch {
                        searchQuery.emit(query)
                    }
                },
                onWalletClick = { wallet ->
                    if (wallet != null && (wallet.isWatch() || (wallet.isImported() && !wallet.hasLocalPrivateKey))) {
                        NoKeyWarningBottomSheetDialogFragment.newInstance(wallet).apply {
                            onConfirm = {
                                this@WalletListBottomSheetDialogFragment.onWalletClickListener?.invoke(wallet)
                                this@WalletListBottomSheetDialogFragment.dismiss()
                            }
                        }.show(parentFragmentManager, NoKeyWarningBottomSheetDialogFragment.TAG)
                    } else {
                        onWalletClickListener?.invoke(wallet)
                        dismiss()
                    }
                },
                onCancel = {
                    dismiss()
                }
            )
        }
    }

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop()
    }

    fun setOnWalletClickListener(listener: (Web3Wallet?) -> Unit) {
        onWalletClickListener = listener
    }

    override fun showError(error: String) {
    }

    companion object {
        const val TAG = "WalletListBottomSheetDialogFragment"
        private const val ARGS_EXCLUDE_WALLET_ID = "args_exclude_wallet_id"
        private const val ARGS_CHAIN_ID = "args_chain_id"

        fun newInstance(excludeWalletId: String?, chainId: String): WalletListBottomSheetDialogFragment {
            return WalletListBottomSheetDialogFragment().withArgs {
                putString(ARGS_EXCLUDE_WALLET_ID, excludeWalletId)
                putString(ARGS_CHAIN_ID, chainId)
            }
        }
    }
}

sealed class WalletListItem {
    object PrivacyWallet : WalletListItem()
    data class RegularWallet(val wallet: Web3Wallet) : WalletListItem() // common wallet or imported wallet
}

@Composable
fun WalletListScreen(
    wallets: List<Web3Wallet>,
    excludeWalletId: String?,
    onQueryChanged: (String) -> Unit,
    onWalletClick: (Web3Wallet?) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) }
    var query by remember { mutableStateOf("") }
    val hidePrivacyWalletInfo = remember { mutableStateOf(prefs.getBoolean(KEY_HIDE_PRIVACY_WALLET_INFO, false)) }
    val hideCommonWalletInfo = remember { mutableStateOf(prefs.getBoolean(KEY_HIDE_COMMON_WALLET_INFO, false)) }

    val walletItems = remember(wallets, excludeWalletId, query) {
        buildList {
            if (excludeWalletId != null && query.isEmpty()) {
                add(WalletListItem.PrivacyWallet)
            }
            wallets.forEach { wallet ->
                add(WalletListItem.RegularWallet(wallet))
            }
        }
    }

    Column(modifier = Modifier
        .padding(bottom = GetNavBarHeightValue())
        .fillMaxSize()) {
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Render unified wallet items
            walletItems.forEachIndexed { index, item ->
                when (item) {
                    is WalletListItem.PrivacyWallet -> {
                        WalletCard(
                            name = stringResource(id = R.string.Privacy_Wallet),
                            destination = WalletDestination.Privacy,
                            onClick = {
                                onWalletClick.invoke(null)
                            }
                        )
                    }

                    is WalletListItem.RegularWallet -> {
                        val wallet = item.wallet
                        if (wallet.isImported()) {
                            val destination = WalletDestination.Import(wallet.id, wallet.category)
                            WalletCard(
                                name = wallet.name,
                                destination = destination,
                                hasLocalPrivateKey = wallet.hasLocalPrivateKey,
                                onClick = { onWalletClick(wallet) }
                            )
                        } else if (wallet.isWatch()) {
                            val destination = WalletDestination.Watch(wallet.id, wallet.category)
                            WalletCard(
                                name = wallet.name,
                                destination = destination,
                                onClick = { onWalletClick(wallet) }
                            )
                        } else {
                            val destination = WalletDestination.Classic(wallet.id)
                            WalletCard(
                                name = wallet.name,
                                destination = destination,
                                onClick = { onWalletClick(wallet) }
                            )
                        }
                    }
                }
                if (index < walletItems.size - 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (!hidePrivacyWalletInfo.value || !hideCommonWalletInfo.value) {
                Spacer(modifier = Modifier.weight(1f))
                WalletInfoCard(
                    hidePrivacyWalletInfo = hidePrivacyWalletInfo.value,
                    hideCommonWalletInfo = hideCommonWalletInfo.value,
                    onPrivacyClose = {
                        hidePrivacyWalletInfo.value = true
                        prefs.edit { putBoolean(KEY_HIDE_PRIVACY_WALLET_INFO, true) }
                    },
                    onCommonClose = {
                        hideCommonWalletInfo.value = true
                        prefs.edit { putBoolean(KEY_HIDE_COMMON_WALLET_INFO, true) }
                    }
                )
                Spacer(modifier = Modifier.height(30.dp))
            }
            Spacer(modifier = Modifier.height(GetNavBarHeightValue()))
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    isSearching: Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .background(
                    color = MixinAppTheme.colors.backgroundWindow,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 12.dp),
            textStyle = TextStyle(
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp
            ),
            singleLine = true,
            cursorBrush = SolidColor(MixinAppTheme.colors.textBlue),
            decorationBox = { innerTextField ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit_search),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.icon
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(id = R.string.search_placeholder_asset),
                                color = MixinAppTheme.colors.textAssist,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChanged("") },
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_asset_add_search_clear),
                                contentDescription = "Clear",
                                tint = Color.Unspecified
                            )
                        }
                    }
                }
            }
        )

        if (isSearching) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .padding(start = 8.dp),
                color = MixinAppTheme.colors.accent,
                strokeWidth = 2.dp
            )
        }

        Text(
            text = stringResource(id = R.string.Cancel),
            color = MixinAppTheme.colors.textBlue,
            fontSize = 16.sp,
            modifier = Modifier
                .clickable(onClick = onCancel)
                .padding(start = 16.dp)
        )
    }
}