package one.mixin.android.ui.address

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.MixinTopAppBar
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.databinding.FragmentAddressInputBinding
import one.mixin.android.databinding.ViewWalletTransactionsBottomBinding
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.biometric.AddressManageBiometricItem
import one.mixin.android.ui.wallet.AllTransactionsFragment
import one.mixin.android.ui.wallet.AllWeb3TransactionsFragment
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.wallet.Web3FilterParams
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.ui.wallet.home.PrivacyTransactionRecycler
import one.mixin.android.ui.wallet.home.Web3TransactionRecycler
import one.mixin.android.ui.wallet.home.components.SectionCard
import one.mixin.android.ui.wallet.transfer.TransferBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Address
import one.mixin.android.vo.Chain
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class AddressBookFragment : BaseFragment(R.layout.fragment_address_input) {
    companion object {
        const val ARGS_ADDRESS = "args_address"
    }

    private val viewModel by viewModels<AddressViewModel>()
    private val binding by viewBinding(FragmentAddressInputBinding::bind)
    private val address by lazy {
        requireNotNull(requireArguments().getParcelableCompat(ARGS_ADDRESS, Address::class.java))
    }
    private val walletId by lazy { requireArguments().getString(WalletActivity.ARGS_WALLET_ID) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.compose.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        binding.compose.setContent {
            MixinAppTheme {
                var chains by remember { mutableStateOf<List<Chain>>(emptyList()) }
                var transactionError by remember { mutableStateOf(false) }
                var transactionRetry by remember { mutableStateOf(0) }
                var isDeleting by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                val currentWalletId = walletId
                val transactions: List<SnapshotItem>? = if (currentWalletId == null) {
                    val transactionsFlow = remember(address, transactionRetry) {
                        viewModel.recentAddressTransactions(address).catch { error ->
                            transactionError = true
                            ErrorHandler.handleError(error)
                        }
                    }
                    key(address.addressId, transactionRetry) {
                        transactionsFlow.collectAsStateWithLifecycle(initialValue = null).value
                    }
                } else {
                    null
                }
                val web3Transactions: List<Web3TransactionItem>? = if (currentWalletId != null) {
                    val transactionsFlow = remember(address, currentWalletId, transactionRetry) {
                        viewModel.recentWeb3AddressTransactions(address, currentWalletId).catch { error ->
                            transactionError = true
                            ErrorHandler.handleError(error)
                        }
                    }
                    key(address.addressId, currentWalletId, transactionRetry) {
                        transactionsFlow.collectAsStateWithLifecycle(initialValue = null).value
                    }
                } else {
                    null
                }
                val transactionCount = transactions?.size ?: web3Transactions?.size
                LaunchedEffect(address.addressId, currentWalletId) {
                    chains = viewModel.getChains()
                    viewModel.refreshAddressTransactions(currentWalletId)
                }
                Column(modifier = Modifier.fillMaxSize().background(MixinAppTheme.colors.background)) {
                    MixinTopAppBar(
                        title = { Text(stringResource(R.string.Address)) },
                        backgroundColor = MixinAppTheme.colors.background,
                        navigationIcon = {
                            IconButton(onClick = { requireActivity().onBackPressedDispatcher.onBackPressed() }) {
                                Icon(
                                    painterResource(R.drawable.ic_back),
                                    contentDescription = stringResource(R.string.Address_Book),
                                    tint = MixinAppTheme.colors.icon,
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                val menuBinding = ViewWalletTransactionsBottomBinding.bind(
                                    View.inflate(ContextThemeWrapper(requireActivity(), R.style.Custom), R.layout.view_wallet_transactions_bottom, null),
                                )
                                val bottomSheet = BottomSheet.Builder(requireActivity()).setCustomView(menuBinding.root).create()
                                menuBinding.hide.setText(R.string.Delete_address)
                                menuBinding.hide.setOnClickListener {
                                    bottomSheet.dismiss()
                                    isDeleting = true
                                    scope.launch(CoroutineExceptionHandler { _, error -> ErrorHandler.handleError(error) }) {
                                        try {
                                            val asset = withContext(Dispatchers.IO) { viewModel.findOrSyncAsset(address.assetId) }
                                            if (asset == null) {
                                                toast(R.string.Data_error)
                                                return@launch
                                            }
                                            TransferBottomSheetDialogFragment.newInstance(
                                                AddressManageBiometricItem(
                                                    asset = asset,
                                                    addressId = address.addressId,
                                                    label = address.label,
                                                    destination = address.destination,
                                                    tag = address.tag,
                                                    type = TransferBottomSheetDialogFragment.DELETE,
                                                ),
                                            ).apply {
                                                setCallback(object : TransferBottomSheetDialogFragment.Callback() {
                                                    override fun onDismiss(success: Boolean) {
                                                        if (success) requireActivity().finish()
                                                    }
                                                })
                                            }.showNow(parentFragmentManager, TransferBottomSheetDialogFragment.TAG)
                                        } finally {
                                            isDeleting = false
                                        }
                                    }
                                }
                                menuBinding.cancel.setOnClickListener { bottomSheet.dismiss() }
                                bottomSheet.show()
                            }, enabled = !isDeleting) {
                                Icon(painterResource(R.drawable.ic_more), contentDescription = stringResource(R.string.More), tint = MixinAppTheme.colors.icon)
                            }
                        },
                    )
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor).padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                            ) {
                                AddressBookField(stringResource(R.string.Label), address.label)
                                AddressBookField(stringResource(R.string.Address), address.destination)
                                AddressBookField(
                                    stringResource(R.string.network),
                                    addressBookNetworks.firstOrNull { it.chainId == address.chainId }?.name
                                        ?: chains.firstOrNull { it.chainId == address.chainId }?.name
                                        ?: stringResource(R.string.Unknown),
                                )
                                address.tag?.takeIf { it.isNotEmpty() }?.let { tag ->
                                    AddressBookField(stringResource(R.string.Memo), tag)
                                }
                            }
                        }
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor),
                            ) {
                                SectionCard(
                                    title = stringResource(R.string.Transactions),
                                    showViewAll = (transactionCount ?: 0) > 10,
                                    onClick = {
                                        if (currentWalletId == null) {
                                            findNavController().navigate(
                                                R.id.action_address_book_to_all_transactions,
                                                Bundle().apply {
                                                    putParcelable(AllTransactionsFragment.ARGS_ADDRESS, address)
                                                },
                                            )
                                        } else {
                                            findNavController().navigate(
                                                R.id.action_address_book_to_all_web3_transactions,
                                                Bundle().apply {
                                                    putParcelable(AllWeb3TransactionsFragment.ARGS_FILTER_PARAMS, Web3FilterParams(walletId = currentWalletId, address = address))
                                                },
                                            )
                                        }
                                    },
                                    contentUsesOwnPadding = true,
                                ) {
                                    when {
                                        transactionError -> TextButton(
                                            onClick = {
                                                transactionError = false
                                                transactionRetry++
                                            },
                                            modifier = Modifier.padding(horizontal = 20.dp),
                                        ) {
                                            Text(stringResource(R.string.Retry), color = MixinAppTheme.colors.textBlue)
                                        }
                                        transactionCount == null -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(color = MixinAppTheme.colors.accent)
                                        }
                                        transactionCount == 0 -> Text(
                                            stringResource(R.string.No_transactions),
                                            modifier = Modifier.padding(horizontal = 20.dp),
                                            color = MixinAppTheme.colors.textAssist,
                                        )
                                        currentWalletId == null -> PrivacyTransactionRecycler(
                                            transactions = transactions.orEmpty().take(10),
                                            onClick = { index -> transactions?.getOrNull(index)?.let { openAddressTransaction(it, viewModel) } },
                                            onUserClick = {},
                                            modifier = Modifier.fillMaxWidth(),
                                            contentHorizontalPadding = 20.dp,
                                        )
                                        else -> Web3TransactionRecycler(
                                            transactions = web3Transactions.orEmpty().take(10),
                                            onClick = { index -> web3Transactions?.getOrNull(index)?.let { openAddressTransaction(it, currentWalletId, viewModel) } },
                                            onTokenClick = { index -> web3Transactions?.getOrNull(index)?.let { openAddressTransaction(it, currentWalletId, viewModel) } },
                                            modifier = Modifier.fillMaxWidth(),
                                            contentHorizontalPadding = 20.dp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AddressBookNetwork(chainId: String, chain: Chain?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CoilImage(chain?.iconUrl, R.drawable.ic_avatar_place_holder, Modifier.size(16.dp).clip(CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(
            addressBookNetworks.firstOrNull { it.chainId == chainId }?.name ?: chain?.name ?: stringResource(R.string.Unknown),
            color = MixinAppTheme.colors.textAssist,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun AddressBookField(title: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = MixinAppTheme.colors.textAssist, fontSize = 14.sp)
        SelectionContainer {
            Text(value, color = MixinAppTheme.colors.textPrimary, fontSize = 14.sp)
        }
    }
}
