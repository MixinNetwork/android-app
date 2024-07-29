package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.ui.graphics.Color
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentTransactionsBinding
import one.mixin.android.databinding.ViewWalletTransactionsBottomBinding
import one.mixin.android.extension.buildAmountSymbol
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.mainThreadDelayed
import one.mixin.android.extension.navigate
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.job.CheckBalanceJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.tip.Tip
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.NonMessengerUserBottomSheetDialogFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.home.market.LineChart
import one.mixin.android.ui.wallet.adapter.OnSnapshotListener
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.assetIdToAsset
import one.mixin.android.vo.notMessengerUser
import one.mixin.android.vo.safe.DepositEntry
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.safe.toSnapshot
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.DebugClickListener
import javax.inject.Inject

@AndroidEntryPoint
class TransactionsFragment : BaseFragment(R.layout.fragment_transactions), OnSnapshotListener {
    companion object {
        const val TAG = "TransactionsFragment"
        const val ARGS_ASSET = "args_asset"
    }

    private val binding by viewBinding(FragmentTransactionsBinding::bind)
    private var _bottomBinding: ViewWalletTransactionsBottomBinding? = null
    private val bottomBinding get() = requireNotNull(_bottomBinding) { "required _bottomBinding is null" }
    private val sendBottomSheet = SendBottomSheet(this, R.id.action_transactions_to_single_friend_select, R.id.action_transactions_to_address_management)

    @Inject
    lateinit var tip: Tip

    @Inject
    lateinit var jobManager: MixinJobManager

    private val walletViewModel by viewModels<WalletViewModel>()

    lateinit var asset: TokenItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        asset = requireArguments().getParcelableCompat(ARGS_ASSET, TokenItem::class.java)!!
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        jobManager.addJobInBackground(CheckBalanceJob(arrayListOf(assetIdToAsset(asset.assetId))))
        binding.titleView.apply {
            titleTv.text = asset.name
            leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
            rightAnimator.setOnClickListener {
                showBottom()
            }
        }


        walletViewModel.snapshotsLimit(asset.assetId).observe(viewLifecycleOwner) { list ->
            binding.va.displayedChild = if (list.isEmpty()) {
                1
            } else {
                0
            }
            binding.transactionsRv.list = list
        }

        walletViewModel.assetItem(asset.assetId).observe(
            viewLifecycleOwner,
        ) { assetItem ->
            assetItem?.let {
                asset = it
                bindHeader()
            }
        }

        walletViewModel.refreshAsset(asset.assetId)
        lifecycleScope.launch {
            val depositEntry = walletViewModel.findDepositEntry(asset.chainId)
            if (depositEntry != null && depositEntry.destination.isNotBlank()) {
                refreshPendingDeposits(asset, depositEntry)
            }
        }
    }

    override fun onDestroyView() {
        _bottomBinding = null
        sendBottomSheet.release()
        super.onDestroyView()
    }

    private fun refreshPendingDeposits(
        asset: TokenItem,
        depositEntry: DepositEntry,
    ) {
        if (viewDestroyed()) return
        lifecycleScope.launch {
            handleMixinResponse(
                invokeNetwork = {
                    walletViewModel.refreshPendingDeposits(asset.assetId, depositEntry)
                },
                successBlock = { list ->
                    withContext(Dispatchers.IO) {
                        walletViewModel.clearPendingDepositsByAssetId(asset.assetId)
                        val pendingDeposits = list.data
                        if (pendingDeposits.isNullOrEmpty()) {
                            return@withContext
                        }

                        pendingDeposits.chunked(100) { chunk ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                chunk.map {
                                    it.toSnapshot()
                                }.let {
                                    walletViewModel.insertPendingDeposit(it)
                                }
                            }
                        }
                    }
                },
            )
        }
    }

    @SuppressLint("InflateParams")
    private fun showBottom() {
        val builder = BottomSheet.Builder(requireActivity())
        _bottomBinding = ViewWalletTransactionsBottomBinding.bind(View.inflate(ContextThemeWrapper(requireActivity(), R.style.Custom), R.layout.view_wallet_transactions_bottom, null))
        builder.setCustomView(bottomBinding.root)
        val bottomSheet = builder.create()
        bottomBinding.apply {
            hide.setText(if (asset.hidden == true) R.string.Show else R.string.Hide)
            hide.setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    walletViewModel.updateAssetHidden(asset.assetId, asset.hidden != true)
                }
                bottomSheet.dismiss()
                mainThreadDelayed({ activity?.onBackPressedDispatcher?.onBackPressed() }, 200)
            }
            cancel.setOnClickListener { bottomSheet.dismiss() }
        }

        bottomSheet.show()
    }

    override fun <T> onNormalItemClick(item: T) {
        view?.navigate(
            R.id.action_transactions_fragment_to_transaction_fragment,
            Bundle().apply {
                putParcelable(TransactionFragment.ARGS_SNAPSHOT, item as SnapshotItem)
                putParcelable(ARGS_ASSET, asset)
            },
        )
    }

    override fun onUserClick(userId: String) {
        lifecycleScope.launch {
            val user =
                withContext(Dispatchers.IO) {
                    walletViewModel.getUser(userId)
                } ?: return@launch

            if (user.notMessengerUser()) {
                NonMessengerUserBottomSheetDialogFragment.newInstance(user)
                    .showNow(parentFragmentManager, NonMessengerUserBottomSheetDialogFragment.TAG)
            } else {
                val f = UserBottomSheetDialogFragment.newInstance(user)
                f?.show(parentFragmentManager, UserBottomSheetDialogFragment.TAG)
            }
        }
    }

    private fun bindHeader(){
        binding.apply {
            if (asset.collectionHash.isNullOrEmpty()) {
                topRl.setOnClickListener {
                    AssetKeyBottomSheetDialogFragment.newInstance(asset)
                        .showNow(parentFragmentManager, AssetKeyBottomSheetDialogFragment.TAG)
                }
            }
            updateHeader(asset)
            sendReceiveView.send.setOnClickListener {
                sendBottomSheet.show(asset)
            }
            sendReceiveView.receive.setOnClickListener {
                sendReceiveView.navigate(
                    R.id.action_transactions_to_deposit,
                    Bundle().apply { putParcelable(ARGS_ASSET, asset) },
                )
            }
            root.post {
                if (viewDestroyed()) return@post
                va.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = requireContext().screenHeight() - this@TransactionsFragment.binding.titleView.height - topLl.height - marketRl.height - 60.dp
                }
            }
            marketView.setContent {
                LineChart(listOf(1.0f, 2.3f, 2.0f, 6.2f, 7.8f, 5.2f, 4.5f, 5.5f, 5.0f, 4.2f, 3.5f, 4.5f, 4.0f), Color(0xFF50BD5CL), false)
            }
        }
    }

    private fun updateHeader(asset: TokenItem) {
        binding.apply {
            val amountText =
                try {
                    if (asset.balance.toFloat() == 0f) {
                        "0.00"
                    } else {
                        asset.balance.numberFormat()
                    }
                } catch (ignored: NumberFormatException) {
                    asset.balance.numberFormat()
                }
            val color = requireContext().colorFromAttribute(R.attr.text_primary)
            balance.text = buildAmountSymbol(requireContext(), amountText, asset.symbol, color, color)
            balanceAs.text =
                try {
                    if (asset.fiat().toFloat() == 0f) {
                        "≈ ${Fiats.getSymbol()}0.00"
                    } else {
                        "≈ ${Fiats.getSymbol()}${asset.fiat().numberFormat2()}"
                    }
                } catch (ignored: NumberFormatException) {
                    "≈ ${Fiats.getSymbol()}${asset.fiat().numberFormat2()}"
                }
            avatar.loadToken(asset)
            avatar.setOnClickListener(
                object : DebugClickListener() {
                    override fun onDebugClick() {
                        view?.navigate(
                            R.id.action_transactions_to_utxo,
                            Bundle().apply {
                                putParcelable(ARGS_ASSET, asset)
                            },
                        )
                    }

                    override fun onSingleClick() {
                    }
                },
            )
        }
    }
}
