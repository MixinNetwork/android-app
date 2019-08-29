package one.mixin.android.ui.address

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_address_management.*
import kotlinx.android.synthetic.main.item_address.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.ui.address.adapter.AddressAdapter
import one.mixin.android.ui.address.adapter.ItemCallback
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.PinBottomSheetDialogFragment
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment.Companion.DELETE
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment.Companion.MODIFY
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.Address
import one.mixin.android.vo.AssetItem
import one.mixin.android.widget.SearchView

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
        ViewModelProvider(this, viewModelFactory).get(AddressViewModel::class.java)
    }

    private var deleteSuccess = false
    private val asset: AssetItem by lazy {
        arguments!!.getParcelable(ARGS_ASSET) as AssetItem
    }
    private var addresses: List<Address>? = null

    private val adapter: AddressAdapter by lazy { AddressAdapter(asset, true) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_address_management, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        title_view.right_animator.setOnClickListener {
            activity?.addFragment(this@AddressManagementFragment,
                AddressAddFragment.newInstance(asset), AddressAddFragment.TAG)
        }
        empty_tv.setOnClickListener {
            activity?.addFragment(this@AddressManagementFragment,
                AddressAddFragment.newInstance(asset), AddressAddFragment.TAG)
        }
        addressViewModel.addresses(asset.assetId).observe(this, Observer {
            val list = it?.toMutableList()
            if (list.isNullOrEmpty()) {
                empty_tv.isVisible = true
                content_ll.isGone = true
            } else {
                empty_tv.isVisible = false
                content_ll.isGone = false
            }
            addresses = list
            adapter.addresses = list
        })
        val addrListener = object : AddressAdapter.SimpleAddressListener() {
            override fun onAddrLongClick(view: View, addr: Address) {
                val popMenu = PopupMenu(activity!!, view.addr_tv)
                popMenu.menuInflater.inflate(R.menu.address_mamangement_item, popMenu.menu)
                popMenu.setOnMenuItemClickListener {
                    if (it.itemId == R.id.delete) {
                        showBottomSheet(addr, asset)
                    } else if (it.itemId == R.id.edit) {
                        activity?.addFragment(this@AddressManagementFragment,
                            AddressAddFragment.newInstance(asset, addr, MODIFY), AddressAddFragment.TAG)
                    }
                    return@setOnMenuItemClickListener true
                }
                popMenu.show()
            }

            override fun onAddrClick(addr: Address) {
                TransferFragment.newInstance(asset = asset, address = addr)
                    .showNow(requireFragmentManager(), TransferFragment.TAG)
            }
        }
        ItemTouchHelper(ItemCallback(object : ItemCallback.ItemCallbackListener {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val deletePos = viewHolder.adapterPosition
                val addr = adapter.addresses!![deletePos]
                if (direction == ItemTouchHelper.START) {
                    adapter.notifyItemChanged(deletePos)
                    activity?.addFragment(this@AddressManagementFragment,
                        AddressAddFragment.newInstance(asset, addr, MODIFY), AddressAddFragment.TAG)
                } else if (direction == ItemTouchHelper.END) {
                    val deleteItem = adapter.removeItem(viewHolder.adapterPosition)!!
                    val bottomSheet = showBottomSheet(addr, asset)
                    fragmentManager?.executePendingTransactions()
                    bottomSheet.dialog.setOnDismissListener {
                        bottomSheet.dismiss()
                        if (!deleteSuccess) {
                            adapter.restoreItem(deleteItem, deletePos)
                        }
                    }
                }
            }
        })).apply { attachToRecyclerView(addr_rv) }
        addr_rv.adapter = adapter
        adapter.setAddrListener(addrListener)
        search_et.listener = object : SearchView.OnSearchViewListener {
            override fun afterTextChanged(s: Editable?) {
                adapter.addresses = addresses?.filter {
                    val name = if (asset.isAccountTagAsset()) it.accountName else it.label
                    name?.contains(s.toString(), ignoreCase = true) ?: false
                }?.toMutableList()
            }

            override fun onSearch() {
            }
        }
        addressViewModel.refreshAddressesByAssetId(asset.assetId)
    }

    private fun showBottomSheet(addr: Address, asset: AssetItem): MixinBottomSheetDialogFragment {
        val bottomSheet = PinAddrBottomSheetDialogFragment.newInstance(addressId = addr.addressId,
            assetUrl = asset.iconUrl,
            chainIconUrl = asset.chainIconUrl,
            publicKey = addr.publicKey,
            label = addr.label,
            accountTag = addr.accountTag,
            accountName = addr.accountName,
            assetName = asset.name, type = DELETE)
        bottomSheet.showNow(requireFragmentManager(), PinAddrBottomSheetDialogFragment.TAG)
        bottomSheet.callback = object : PinBottomSheetDialogFragment.Callback {
            override fun onSuccess() {
                deleteSuccess = true
            }
        }
        return bottomSheet
    }
}
