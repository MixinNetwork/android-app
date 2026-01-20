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
import one.mixin.android.Constants
import one.mixin.android.R

import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.web3.vo.WalletItem
import one.mixin.android.db.web3.vo.isClassic
import one.mixin.android.db.web3.vo.isImported
import one.mixin.android.db.web3.vo.isMixinSafe
import one.mixin.android.db.web3.vo.isOwner
import one.mixin.android.db.web3.vo.isWatch
import one.mixin.android.db.web3.vo.toWeb3Wallet
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.common.NoKeyWarningBottomSheetDialogFragment
import one.mixin.android.ui.wallet.components.KEY_HIDE_COMMON_WALLET_INFO
import one.mixin.android.ui.wallet.components.KEY_HIDE_PRIVACY_WALLET_INFO
import one.mixin.android.ui.wallet.components.KEY_HIDE_SAFE_WALLET_INFO
import one.mixin.android.ui.wallet.components.PREF_NAME
import one.mixin.android.ui.wallet.components.WalletCard
import one.mixin.android.ui.wallet.components.WalletCategoryFilter
import one.mixin.android.ui.wallet.components.WalletDestination
import one.mixin.android.ui.wallet.components.WalletInfoCard
import one.mixin.android.vo.WalletCategory

@AndroidEntryPoint
class WalletListBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {

    private val viewModel by viewModels<WalletViewModel>()
    private var onWalletClickListener: ((WalletItem?) -> Unit)? = null
    private var onDismissListener: (() -> Unit)? = null

    private val excludeWalletId: String? by lazy {
        requireArguments().getString(ARGS_EXCLUDE_WALLET_ID)
    }

    private val chainId: String? by lazy {
        requireArguments().getString(ARGS_CHAIN_ID)
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    @OptIn(FlowPreview::class)
    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            val searchQuery = remember { MutableStateFlow("") }
            val wallets by viewModel.walletsFlow.collectAsState()
            val allWallets by viewModel.allWalletsFlow.collectAsState()
            LaunchedEffect(Unit) {
                launch {
                    searchQuery.collect { query ->
                        if (query.isEmpty()) {
                            viewModel.searchWallets(excludeWalletId ?: "", chainId ?: "", query)
                        }
                    }
                }
                launch {
                    searchQuery
                        .debounce(150)
                        .collect { query ->
                            if (query.isNotEmpty()) {
                                viewModel.searchWallets(excludeWalletId ?: "", chainId ?: "", query)
                            }
                        }
                }
            }

            WalletListScreen(
                chainId = chainId,
                wallets = wallets,
                allWallets = allWallets,
                excludeWalletId = excludeWalletId,
                onQueryChanged = { query ->
                    lifecycleScope.launch {
                        searchQuery.emit(query)
                    }
                },
                onWalletClick = { wallet ->
                    if (wallet != null && (wallet.isWatch() || (wallet.isImported() && !wallet.hasLocalPrivateKey))) {
                        NoKeyWarningBottomSheetDialogFragment.newInstance(wallet.toWeb3Wallet()).apply {
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

    fun setOnWalletClickListener(listener: (WalletItem?) -> Unit) {
        onWalletClickListener = listener
    }

    fun setOnDismissListener(listener: () -> Unit): WalletListBottomSheetDialogFragment {
        onDismissListener = listener
        return this
    }

    override fun showError(error: String) {
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.invoke()
    }

    companion object {
        const val TAG = "WalletListBottomSheetDialogFragment"
        private const val ARGS_EXCLUDE_WALLET_ID = "args_exclude_wallet_id"
        private const val ARGS_CHAIN_ID = "args_chain_id"

        fun newInstance(excludeWalletId: String?, chainId: String? = null): WalletListBottomSheetDialogFragment {
            return WalletListBottomSheetDialogFragment().withArgs {
                putString(ARGS_EXCLUDE_WALLET_ID, excludeWalletId)
                putString(ARGS_CHAIN_ID, chainId)
            }
        }
    }
}

sealed class WalletListItem {
    object PrivacyWallet : WalletListItem()
    data class RegularWallet(val wallet: WalletItem) : WalletListItem() // common wallet or imported wallet
}

@Composable
fun WalletListScreen(
    chainId:String?,
    wallets: List<WalletItem>,
    allWallets: List<WalletItem>,
    excludeWalletId: String?,
    onQueryChanged: (String) -> Unit,
    onWalletClick: (WalletItem?) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) }
    var query by remember { mutableStateOf("") }
    val hidePrivacyWalletInfo = remember { mutableStateOf(prefs.getBoolean(KEY_HIDE_PRIVACY_WALLET_INFO, false)) }
    val hideCommonWalletInfo = remember { mutableStateOf(prefs.getBoolean(KEY_HIDE_COMMON_WALLET_INFO, false)) }
    val hideSafeWalletInfo = remember { mutableStateOf(prefs.getBoolean(KEY_HIDE_SAFE_WALLET_INFO, false)) }

    val hasAll = remember(allWallets) { allWallets.any { it.isMixinSafe().not() && it.id != excludeWalletId } }
    val hasSafe = remember(allWallets) { allWallets.any { it.safeChainId == chainId } }
    val hasImported = remember(wallets) { allWallets.any { it.isImported() && excludeWalletId != it.id} }
    val hasCreated = remember(wallets) { (chainId == Constants.ChainId.SOLANA_CHAIN_ID || chainId in Constants.Web3ChainIds || chainId == Constants.ChainId.BITCOIN_CHAIN_ID) && allWallets.any { it.isClassic() && it.id != excludeWalletId } }
    val hasWatch = remember(wallets) { allWallets.any { it.isWatch() } }
    var selectedCategory by remember(hasAll, hasSafe, hasCreated, hasImported, hasWatch) {
        mutableStateOf(
            when {
                hasAll -> null
                hasSafe -> WalletCategory.MIXIN_SAFE.value
                hasCreated -> WalletCategory.CLASSIC.value
                hasImported -> "import"
                hasWatch -> "watch"
                else -> null
            }
        )
    }

    val walletItems = remember(wallets, excludeWalletId, query, selectedCategory) {
        buildList {
            if (excludeWalletId != null && query.isEmpty() && selectedCategory == null) {
                add(WalletListItem.PrivacyWallet)
            }
            wallets.forEach { wallet ->
                val shouldShow = when (selectedCategory) {
                    null -> wallet.category != WalletCategory.MIXIN_SAFE.value &&  wallet.category != WalletCategory.WATCH_ADDRESS.value
                    WalletCategory.MIXIN_SAFE.value -> wallet.isMixinSafe()
                    WalletCategory.CLASSIC.value -> wallet.category == WalletCategory.CLASSIC.value
                    "import" -> wallet.isImported()
                    "watch" -> wallet.isWatch()
                    else -> true
                }
                if (shouldShow) {
                    add(WalletListItem.RegularWallet(wallet))
                }
            }
        }
    }

    Column(modifier = Modifier
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
            if (hasAll || hasSafe || hasImported || hasWatch || hasCreated) {
                WalletCategoryFilter(
                    selectedCategory = selectedCategory,
                    hasAll = hasAll,
                    hasImported = hasImported,
                    hasWatch = hasWatch,
                    hasSafe = hasSafe,
                    hasCreated = hasCreated,
                    onCategorySelected = { selectedCategory = it }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
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
                                onClick = { onWalletClick(wallet) },
                            )
                        } else if (wallet.isWatch()) {
                            val destination = WalletDestination.Watch(wallet.id, wallet.category)
                            WalletCard(
                                name = wallet.name,
                                destination = destination,
                                onClick = { onWalletClick(wallet) },
                            )
                        } else if (wallet.isMixinSafe()) {
                            val destination = WalletDestination.Safe(wallet.id, wallet.isOwner(), wallet.safeChainId, wallet.safeUrl)
                            WalletCard(
                                name = wallet.name,
                                destination = destination,
                                onClick = { onWalletClick(wallet) },
                                topArrow = false
                            )
                        } else {
                            val destination = WalletDestination.Classic(wallet.id)
                            WalletCard(
                                name = wallet.name,
                                destination = destination,
                                onClick = { onWalletClick(wallet) },
                            )
                        }
                    }
                }
                if (index < walletItems.size - 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (!hidePrivacyWalletInfo.value || !hideCommonWalletInfo.value || !hideSafeWalletInfo.value) {
                Spacer(modifier = Modifier.weight(1f))
                WalletInfoCard(
                    hidePrivacyWalletInfo = hidePrivacyWalletInfo.value,
                    hideCommonWalletInfo = hideCommonWalletInfo.value,
                    hideSafeWalletInfo = hideSafeWalletInfo.value,
                    onPrivacyClose = {
                        hidePrivacyWalletInfo.value = true
                        prefs.edit { putBoolean(KEY_HIDE_PRIVACY_WALLET_INFO, true) }
                    },
                    onCommonClose = {
                        hideCommonWalletInfo.value = true
                        prefs.edit { putBoolean(KEY_HIDE_COMMON_WALLET_INFO, true) }
                    },
                    onSafeClose = {
                        hideSafeWalletInfo.value = true
                        prefs.edit { putBoolean(KEY_HIDE_SAFE_WALLET_INFO, true) }
                    }
                )
                Spacer(modifier = Modifier.height(30.dp))
            }
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
                    color = MixinAppTheme.colors.backgroundWindow, shape = RoundedCornerShape(24.dp)
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