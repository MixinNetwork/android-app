package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentTransactionsBinding
import one.mixin.android.databinding.ViewBadgeCircleImageBinding
import one.mixin.android.databinding.ViewTransactionsFragmentHeaderBinding
import one.mixin.android.databinding.ViewWalletTransactionsBottomBinding
import one.mixin.android.extension.buildAmountSymbol
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.inflate
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.mainThreadDelayed
import one.mixin.android.extension.navTo
import one.mixin.android.extension.navigate
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.job.CheckBalanceJob
import one.mixin.android.tip.Tip
import one.mixin.android.tip.tipPrivToAddress
import one.mixin.android.ui.common.NonMessengerUserBottomSheetDialogFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.buildWithdrawalBiometricItem
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.wallet.adapter.OnSnapshotListener
import one.mixin.android.ui.wallet.adapter.SnapshotAdapter
import one.mixin.android.vo.Address
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.assetIdToAsset
import one.mixin.android.vo.notMessengerUser
import one.mixin.android.vo.safe.DepositEntry
import one.mixin.android.vo.safe.SafeSnapshotType
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.safe.toSnapshot
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.ConcatHeadersDecoration
import one.mixin.android.widget.DebugClickListener
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
@AndroidEntryPoint
class TransactionsFragment : BaseTransactionsFragment<PagingData<SnapshotItem>>(), OnSnapshotListener {
    companion object {
        const val TAG = "TransactionsFragment"
        const val ARGS_ASSET = "args_asset"
    }

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = requireNotNull(_binding) { "required _binding is null" }
    private var _bottomBinding: ViewWalletTransactionsBottomBinding? = null
    private val bottomBinding get() = requireNotNull(_bottomBinding) { "required _bottomBinding is null" }
    private val sendBottomSheet =
        SendBottomSheet(this, R.id.action_transactions_to_single_friend_select, R.id.action_transactions_to_address_management) {
            VerifyBottomSheetDialogFragment.newInstance().setOnPinSuccess { pin ->
                showTipWithdrawal(pin)
            }.showNow(parentFragmentManager, VerifyBottomSheetDialogFragment.TAG)
        }

    @Inject
    lateinit var tip: Tip

    private val adapter = SnapshotAdapter()
    private val headerAdapter = HeaderAdapter()
    lateinit var asset: TokenItem

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTransactionsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

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
        adapter.listener = this@TransactionsFragment
        adapter.addOnPagesUpdatedListener {
            val list = adapter.snapshot().items
            if (list.isNotEmpty()) {
                headerAdapter.show = false
                list.filter {
                    it.opponentId.isNotBlank()
                }.map {
                    it.opponentId
                }.let { ids ->
                    walletViewModel.checkAndRefreshUsers(ids)
                }
            } else {
                headerAdapter.show = true
            }
        }
        binding.transactionsRv.itemAnimator = null
        binding.transactionsRv.addItemDecoration(ConcatHeadersDecoration(adapter).apply { headerCount = 1 })
        val concatAdapter = ConcatAdapter(headerAdapter, adapter)
        binding.transactionsRv.adapter = concatAdapter

        dataObserver =
            Observer { pagedList ->
                lifecycleScope.launch {
                    adapter.submitData(pagedList)
                }
            }
        bindLiveData()
        walletViewModel.assetItem(asset.assetId).observe(
            viewLifecycleOwner,
        ) { assetItem ->
            assetItem?.let {
                asset = it
                headerAdapter.asset = it
            }
        }

        walletViewModel.refreshAsset(asset.assetId)
        lifecycleScope.launch {
            val (depositEntry, _) = walletViewModel.syncDepositEntry(asset.chainId)
            if (depositEntry != null && depositEntry.destination.isNotBlank()) {
                refreshPendingDeposits(asset, depositEntry)
            }
        }
    }

    override fun onDestroyView() {
        adapter.listener = null
        _binding = null
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
                        val pendingDeposits = list.data ?: return@withContext

                        pendingDeposits.chunked(100) { trunk ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                val ids = trunk.map { it.depositId }
                                val existIds = walletViewModel.findPendingSnapshotsByIds(asset.assetId, ids)
                                trunk.filter {
                                    it.depositId !in existIds
                                }.map {
                                    it.toSnapshot(asset.assetId)
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

    override fun refreshSnapshots() {
        // paging3 refresh by mediator
    }

    override fun onApplyClick() {
        bindLiveData()
        filtersSheet.dismiss()
    }

    private fun bindLiveData() =
        lifecycleScope.launch {
            if (viewDestroyed()) return@launch

            val orderByAmount = currentOrder == R.id.sort_amount
            when (currentType) {
                R.id.filters_radio_all -> {
                    bindLiveData(walletViewModel.snapshots(asset.assetId, initialLoadKey = initialLoadKey, orderByAmount = orderByAmount))
                }

                R.id.filters_radio_transfer -> {
                    bindLiveData(
                        walletViewModel.snapshots(
                            asset.assetId,
                            SafeSnapshotType.transfer.name,
                            SafeSnapshotType.pending.name,
                            initialLoadKey = initialLoadKey,
                            orderByAmount = orderByAmount,
                        ),
                    )
                }

                R.id.filters_radio_deposit -> {
                    bindLiveData(
                        walletViewModel.snapshots(
                            asset.assetId,
                            SafeSnapshotType.deposit.name,
                            initialLoadKey = initialLoadKey,
                            orderByAmount = orderByAmount,
                        ),
                    )
                }

                R.id.filters_radio_withdrawal -> {
                    bindLiveData(
                        walletViewModel.snapshots(
                            asset.assetId,
                            SafeSnapshotType.withdrawal.name,
                            initialLoadKey = initialLoadKey,
                            orderByAmount = orderByAmount,
                        ),
                    )
                }
            }
            headerAdapter.currentType = currentType
        }

    private fun showTipWithdrawal(pin: String) =
        lifecycleScope.launch {
            if (viewDestroyed()) return@launch

            val result = tip.getOrRecoverTipPriv(requireContext(), pin)
            if (result.isSuccess) {
                val destination = tipPrivToAddress(result.getOrThrow(), Constants.ChainId.ETHEREUM_CHAIN_ID)
                val addressFeeResponse =
                    handleMixinResponse(
                        invokeNetwork = {
                            walletViewModel.getExternalAddressFee(asset.assetId, destination, null)
                        },
                        successBlock = {
                            it.data
                        },
                    ) ?: return@launch

                val mockAddress = Address("", "address", asset.assetId, addressFeeResponse.destination, "TIP Wallet", nowInUtc(), "0", addressFeeResponse.fee, null, null, asset.chainId)
                val withdrawalBiometricItem = buildWithdrawalBiometricItem(mockAddress, asset)
                val transferFragment = TransferFragment.newInstance(withdrawalBiometricItem)
                transferFragment.showNow(parentFragmentManager, TransferFragment.TAG)
            }
        }

    inner class HeaderAdapter : RecyclerView.Adapter<HeaderAdapter.ViewHolder>() {
        var asset: TokenItem? = null
            set(value) {
                if (value == field) return

                field = value
                notifyItemChanged(0)
            }

        var show: Boolean = false
            set(value) {
                if (value == field) return

                field = value
                notifyItemChanged(0)
            }

        var currentType: Int = R.id.filters_radio_all
            set(value) {
                if (value == field) return

                field = value
                notifyItemChanged(0)
            }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val headerBinding = ViewTransactionsFragmentHeaderBinding.bind(itemView)

            fun bind(
                asset: TokenItem,
                show: Boolean,
                currentType: Int,
            ) {
                headerBinding.apply {
                    groupInfoMemberTitleSort.setOnClickListener {
                        showFiltersSheet()
                    }
                    topRl.setOnClickListener {
                        AssetKeyBottomSheetDialogFragment.newInstance(asset)
                            .showNow(parentFragmentManager, AssetKeyBottomSheetDialogFragment.TAG)
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

                        bottomRl.updateLayoutParams<ViewGroup.LayoutParams> {
                            height = requireContext().screenHeight() - this@TransactionsFragment.binding.titleView.height - topLl.height - groupInfoMemberTitleLayout.height
                        }
                    }
                    bottomRl.isVisible = show
                    when (currentType) {
                        R.id.filters_radio_all -> {
                            groupInfoMemberTitle.setText(R.string.Transactions)
                            walletTransactionsEmpty.setText(R.string.No_transactions)
                        }

                        R.id.filters_radio_transfer -> {
                            groupInfoMemberTitle.setText(R.string.Transfer)
                            walletTransactionsEmpty.setText(R.string.No_transactions)
                        }

                        R.id.filters_radio_deposit -> {
                            groupInfoMemberTitle.setText(R.string.Deposit)
                            walletTransactionsEmpty.setText(R.string.No_deposits)
                        }

                        R.id.filters_radio_withdrawal -> {
                            groupInfoMemberTitle.setText(R.string.Withdrawal)
                            walletTransactionsEmpty.setText(R.string.No_withdrawals)
                        }

                        R.id.filters_radio_fee -> {
                            groupInfoMemberTitle.setText(R.string.Fee)
                            walletTransactionsEmpty.setText(R.string.No_fees)
                        }

                        R.id.filters_radio_rebate -> {
                            groupInfoMemberTitle.setText(R.string.Rebate)
                            walletTransactionsEmpty.setText(R.string.No_rebates)
                        }

                        R.id.filters_radio_raw -> {
                            groupInfoMemberTitle.setText(R.string.Raw)
                            walletTransactionsEmpty.setText(R.string.No_raws)
                        }
                    }
                }
            }

            @SuppressLint("SetTextI18n")
            fun updateHeader(asset: TokenItem) {
                headerBinding.apply {
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
                    ViewBadgeCircleImageBinding.bind(avatar).apply {
                        bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
                        badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
                    }
                    avatar.setOnClickListener(
                        object : DebugClickListener() {
                            override fun onDebugClick() {
                                view?.navigate(
                                    R.id.action_transactions_to_utxo,
                                    Bundle().apply {
                                        putParcelable(ARGS_ASSET, asset)
                                    }
                                )
                            }

                            override fun onSingleClick() {
                            }
                        }
                    )
                }
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): ViewHolder {
            return ViewHolder(parent.inflate(R.layout.view_transactions_fragment_header))
        }

        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int,
        ) {
            asset?.let { holder.bind(it, show, currentType) }
        }

        override fun getItemCount(): Int {
            return 1
        }
    }
}
