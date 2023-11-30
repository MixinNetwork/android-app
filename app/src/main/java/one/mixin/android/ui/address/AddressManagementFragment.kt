package one.mixin.android.ui.address

import android.os.Bundle
import android.text.Editable
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentAddressManagementBinding
import one.mixin.android.databinding.ItemAddressBinding
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.navigate
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.session.Session
import one.mixin.android.ui.address.adapter.AddressAdapter
import one.mixin.android.ui.address.adapter.ItemCallback
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.buildWithdrawalBiometricItem
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment.Companion.DELETE
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Address
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.SearchView

@AndroidEntryPoint
class AddressManagementFragment : BaseFragment(R.layout.fragment_address_management) {
    private val addressViewModel by viewModels<AddressViewModel>()

    private val asset: TokenItem by lazy {
        requireArguments().getParcelableCompat(ARGS_ASSET, TokenItem::class.java)!!
    }
    private var addresses: List<Address>? = null

    private val adapter: AddressAdapter by lazy { AddressAdapter() }

    private val binding by viewBinding(FragmentAddressManagementBinding::bind)

    private val walletViewModel by viewModels<WalletViewModel>()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        binding.titleView.rightAnimator.setOnClickListener {
            view.navigate(
                R.id.action_address_management_to_address_add,
                Bundle().apply {
                    putParcelable(ARGS_ASSET, asset)
                },
            )
        }
        binding.emptyTv.setOnClickListener {
            view.navigate(
                R.id.action_address_management_to_address_add,
                Bundle().apply {
                    putParcelable(ARGS_ASSET, asset)
                },
            )
        }
        addressViewModel.addresses(asset.assetId).observe(
            viewLifecycleOwner,
        ) {
            val list = it?.toMutableList()
            if (list.isNullOrEmpty()) {
                binding.emptyTv.isVisible = true
                binding.contentLl.isGone = true
            } else {
                binding.emptyTv.isVisible = false
                binding.contentLl.isGone = false
            }
            addresses = list
            adapter.addresses = list
        }
        val addrListener =
            object : AddressAdapter.SimpleAddressListener() {
                override fun onAddrLongClick(
                    view: View,
                    addr: Address,
                ) {
                    val popMenu = PopupMenu(requireActivity(), ItemAddressBinding.bind(view).addrTv)
                    popMenu.menuInflater.inflate(R.menu.address_management_item, popMenu.menu)
                    popMenu.setOnMenuItemClickListener {
                        if (it.itemId == R.id.delete) {
                            showBottomSheet(addr, asset)
                        }
                        return@setOnMenuItemClickListener true
                    }
                    popMenu.show()
                }

                override fun onAddrClick(addr: Address) {
                    if (Session.getAccount()?.hasPin == true) {
                        lifecycleScope.launch {
                            val dialog =
                                indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
                                    setCancelable(false)
                                }
                            dialog.show()
                            val addressFeeResponse =
                                handleMixinResponse(
                                    invokeNetwork = {
                                        walletViewModel.getExternalAddressFee(asset.assetId, addr.destination, addr.tag)
                                    },
                                    successBlock = {
                                        it
                                    }, endBlock = {
                                        dialog.dismiss()
                                    }
                                )
                            if (addressFeeResponse == null || addressFeeResponse.error != null) {
                                toast(R.string.verification_failed)
                                return@launch
                            }
                            val item = buildWithdrawalBiometricItem(addr, asset)
                            val transferFragment = TransferFragment.newInstance(item)
                            transferFragment.showNow(parentFragmentManager, TransferFragment.TAG)
                            transferFragment.callback =
                                object : TransferFragment.Callback {
                                    override fun onSuccess() {
                                        if (viewDestroyed()) return

                                        view.navigate(
                                            R.id.action_address_management_to_transactions,
                                            Bundle().apply { putParcelable(ARGS_ASSET, asset) },
                                        )
                                    }
                                }
                        }
                    } else {
                        toast(R.string.transfer_without_pin)
                    }
                }
            }
        ItemTouchHelper(
            ItemCallback(
                object : ItemCallback.ItemCallbackListener {
                    override fun onSwiped(
                        viewHolder: RecyclerView.ViewHolder,
                        direction: Int,
                    ) {
                        val deletePos = viewHolder.bindingAdapterPosition
                        val addr = adapter.addresses!![deletePos]
                        val deleteItem = adapter.removeItem(viewHolder.bindingAdapterPosition)!!
                        val bottomSheet = showBottomSheet(addr, asset)
                        parentFragmentManager.executePendingTransactions()
                        bottomSheet.setCallback(
                            object : BiometricBottomSheetDialogFragment.Callback() {
                                override fun onDismiss(success: Boolean) {
                                    if (!success) {
                                        adapter.restoreItem(deleteItem, deletePos)
                                    }
                                }
                            },
                        )
                    }
                },
            ),
        ).apply { attachToRecyclerView(binding.addrRv) }
        binding.addrRv.adapter = adapter
        adapter.setAddrListener(addrListener)
        binding.searchEt.listener =
            object : SearchView.OnSearchViewListener {
                override fun afterTextChanged(s: Editable?) {
                    adapter.addresses =
                        addresses?.filter {
                            it.label.containsIgnoreCase(s)
                        }?.sortedByDescending { it.label.equalsIgnoreCase(s) }?.toMutableList()
                }

                override fun onSearch() {
                }
            }
        addressViewModel.refreshAddressesByAssetId(asset.assetId)
    }

    private fun showBottomSheet(
        address: Address,
        asset: TokenItem,
    ): BiometricBottomSheetDialogFragment {
        val bottomSheet =
            PinAddrBottomSheetDialogFragment.newInstance(
                addressId = address.addressId,
                assetUrl = asset.iconUrl,
                assetSymbol = asset.symbol,
                assetKey = asset.assetKey,
                chainId = asset.chainId,
                chainName = asset.chainName,
                chainIconUrl = asset.chainIconUrl,
                destination = address.destination,
                label = address.label,
                tag = address.tag,
                assetName = asset.name,
                type = DELETE,
            )
        bottomSheet.showNow(parentFragmentManager, PinAddrBottomSheetDialogFragment.TAG)
        return bottomSheet
    }
}
