package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.edit
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
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
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.realSize
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.NoKeyWarningBottomSheetDialogFragment
import one.mixin.android.ui.wallet.components.KEY_HIDE_COMMON_WALLET_INFO
import one.mixin.android.ui.wallet.components.KEY_HIDE_PRIVACY_WALLET_INFO
import one.mixin.android.ui.wallet.components.PREF_NAME
import one.mixin.android.ui.wallet.components.WalletCard
import one.mixin.android.ui.wallet.components.WalletDestination
import one.mixin.android.ui.wallet.components.WalletInfoCard
import one.mixin.android.util.SystemUIManager
import one.mixin.android.vo.WalletCategory

@AndroidEntryPoint
class WalletListBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val viewModel by viewModels<WalletViewModel>()
    private var onWalletClickListener: ((Web3Wallet?) -> Unit)? = null
    private var behavior: BottomSheetBehavior<*>? = null

    private val excludeWalletId: String? by lazy {
        requireArguments().getString(ARGS_EXCLUDE_WALLET_ID)
    }

    private val chainId: String by lazy {
        requireArguments().getString(ARGS_CHAIN_ID) ?: ""
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    @OptIn(FlowPreview::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MixinAppTheme {
                    val searchQuery = remember { MutableStateFlow("") }
                    val wallets by viewModel.walletsFlow.collectAsState()
                    LaunchedEffect(Unit) {
                        searchQuery
                            .debounce(300)
                            .collect { query ->
                                viewModel.searchWallets(excludeWalletId ?: "", chainId, query)
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
            doOnPreDraw {
                val params = (it.parent as View).layoutParams as? CoordinatorLayout.LayoutParams
                behavior = params?.behavior as? BottomSheetBehavior<*>
                behavior?.peekHeight =
                    requireContext().realSize().y - requireContext().statusBarHeight() - requireContext().navigationBarHeight()
                behavior?.isDraggable = false
                behavior?.addBottomSheetCallback(bottomSheetBehaviorCallback)
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, R.style.MixinBottomSheet)
        dialog.window?.let { window ->
            SystemUIManager.lightUI(window, requireContext().isNightMode())
            window.setGravity(Gravity.BOTTOM)
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(window, !requireContext().isNightMode())
        }
    }

    override fun dismiss() {
        dismissAllowingStateLoss()
    }

    private val bottomSheetBehaviorCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismissAllowingStateLoss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }

    fun setOnWalletClickListener(listener: (Web3Wallet?) -> Unit) {
        onWalletClickListener = listener
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
    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = query,
            onQueryChanged = {
                query = it
                onQueryChanged(it)
            },
            isSearching = false,
            onCancel = onCancel,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
        Column(modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp).verticalScroll(rememberScrollState())) {
            if (excludeWalletId != null && query.isEmpty()) {
                WalletCard(
                    name = stringResource(id = R.string.Privacy_Wallet),
                    destination = WalletDestination.Privacy,
                    onClick = {
                        onWalletClick.invoke(null)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            wallets.forEach { wallet ->
                if (wallet.isImported()) {
                    val destination = WalletDestination.Import(wallet.id, wallet.category)
                    WalletCard(
                        name = if (wallet.category == WalletCategory.CLASSIC.value) stringResource(R.string.Common_Wallet) else wallet.name,
                        destination = destination,
                        hasLocalPrivateKey = wallet.hasLocalPrivateKey,
                        onClick = { onWalletClick(wallet) }
                    )
                } else if (wallet.isWatch()) {
                    val destination = WalletDestination.Watch(wallet.id, wallet.category)
                    WalletCard(
                        name = if (wallet.category == WalletCategory.CLASSIC.value) stringResource(R.string.Common_Wallet) else wallet.name,
                        destination = destination,
                        onClick = { onWalletClick(wallet) }
                    )
                } else {
                    val destination = WalletDestination.Classic(wallet.id)
                    WalletCard(
                        name = if (wallet.category == WalletCategory.CLASSIC.value) stringResource(R.string.Common_Wallet) else wallet.name,
                        destination = destination,
                        onClick = { onWalletClick(wallet) }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

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
                    color = MixinAppTheme.colors.background,
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
                                tint = MixinAppTheme.colors.iconGray
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