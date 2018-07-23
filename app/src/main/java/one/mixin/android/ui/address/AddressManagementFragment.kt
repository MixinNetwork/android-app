package one.mixin.android.ui.address

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_address_management.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.itemdecoration.SpaceItemDecoration
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment.Companion.DELETE
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment.Companion.MODIFY
import one.mixin.android.ui.wallet.TransactionsFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.adapter.AddressAdapter
import one.mixin.android.ui.wallet.adapter.AddressItemCallback
import one.mixin.android.vo.Address
import one.mixin.android.vo.AssetItem
import javax.inject.Inject

class AddressManagementFragment : BaseFragment() {

    companion object {
        const val TAG = "AddressManagementFragment"

        fun newInstance(asset: AssetItem) = AddressManagementFragment().apply {
            val b = Bundle().apply {
                putParcelable(ARGS_ASSET, asset)
            }
            arguments = b
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val addressViewModel: AddressViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(AddressViewModel::class.java)
    }

    private var deleteSuccess = false
    private val asset: AssetItem by lazy {
        arguments!!.getParcelable(TransactionsFragment.ARGS_ASSET) as AssetItem
    }

    private val adapter: AddressAdapter by lazy { AddressAdapter(asset, true, true) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_address_management, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        title_view.right_animator.setOnClickListener {
            activity?.addFragment(this@AddressManagementFragment,
                AddressAddFragment.newInstance(asset, fromManagement = true), AddressAddFragment.TAG)
        }
        addressViewModel.addresses(asset.assetId).observe(this, Observer {
            adapter.addresses = it?.toMutableList()
        })
        addr_rv.addItemDecoration(SpaceItemDecoration())
        val addrListener = object : AddressAdapter.SimpleAddressListener() {
            override fun onAddrDelete(viewHolder: RecyclerView.ViewHolder) {
                val deletePos = viewHolder.adapterPosition
                val addr = adapter.addresses!![deletePos]
                val deleteItem = adapter.removeItem(viewHolder.adapterPosition)!!
                val bottomSheet = showBottomSheet(addr)
                fragmentManager?.executePendingTransactions()
                bottomSheet.dialog.setOnDismissListener {
                    bottomSheet.dismiss()
                    if (!deleteSuccess) {
                        adapter.restoreItem(deleteItem, deletePos)
                    }
                }
            }

            override fun onAddrLongClick(view: View, addr: Address) {
                val popMenu = PopupMenu(activity!!, view)
                popMenu.menuInflater.inflate(R.menu.address_mamangement_item, popMenu.menu)
                popMenu.setOnMenuItemClickListener {
                    showBottomSheet(addr)
                    return@setOnMenuItemClickListener true
                }
                popMenu.show()
            }

            override fun onAddrClick(addr: Address) {
                activity?.addFragment(this@AddressManagementFragment,
                    AddressAddFragment.newInstance(asset, addr, MODIFY, true), AddressAddFragment.TAG)
            }
        }
        ItemTouchHelper(AddressItemCallback(addrListener)).apply { attachToRecyclerView(addr_rv) }
        addr_rv.adapter = adapter
        adapter.setAddrListener(addrListener)
    }

    private fun showBottomSheet(addr: Address): MixinBottomSheetDialogFragment {
        val bottomSheet = PinAddrBottomSheetDialogFragment.newInstance(addressId = addr.addressId, type = DELETE)
        bottomSheet.showNow(requireFragmentManager(), PinAddrBottomSheetDialogFragment.TAG)
        bottomSheet.setCallback(object : PinAddrBottomSheetDialogFragment.Callback {
            override fun onSuccess() {
                deleteSuccess = true
            }
        })
        return bottomSheet
    }
}