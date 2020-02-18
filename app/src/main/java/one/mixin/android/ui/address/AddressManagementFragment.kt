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
import one.mixin.android.extension.navigate
import one.mixin.android.extension.toast
import one.mixin.android.ui.address.adapter.AddressAdapter
import one.mixin.android.ui.address.adapter.ItemCallback
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment.Companion.DELETE
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.Session
import one.mixin.android.vo.Address
import one.mixin.android.vo.AssetItem
import one.mixin.android.widget.SearchView

class AddressManagementFragment : BaseFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val addressViewModel: AddressViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(AddressViewModel::class.java)
    }

    private var deleteSuccess = false
    private val asset: AssetItem by lazy {
        arguments!!.getParcelable<AssetItem>(ARGS_ASSET)!!
    }
    private var addresses: List<Address>? = null

    private val adapter: AddressAdapter by lazy { AddressAdapter(asset, true) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_address_management, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        title_view.right_animator.setOnClickListener {
            view?.navigate(R.id.action_address_management_to_address_add,
                Bundle().apply {
                    putParcelable(ARGS_ASSET, asset)
                })
        }
        empty_tv.setOnClickListener {
            view?.navigate(R.id.action_address_management_to_address_add,
                Bundle().apply {
                    putParcelable(ARGS_ASSET, asset)
                })
        }
        addressViewModel.addresses(asset.assetId).observe(viewLifecycleOwner, Observer {
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
                    }
                    return@setOnMenuItemClickListener true
                }
                popMenu.show()
            }

            override fun onAddrClick(addr: Address) {
                if (Session.getAccount()?.hasPin == true) {
                    val transferFragment = TransferFragment.newInstance(asset = asset, address = addr)
                    transferFragment.showNow(parentFragmentManager, TransferFragment.TAG)
                    transferFragment.callback = object : TransferFragment.Callback {
                        override fun onSuccess() {
                            activity?.onBackPressed()
                        }
                    }
                } else {
                    toast(R.string.transfer_without_pin)
                }
            }
        }
        ItemTouchHelper(ItemCallback(object : ItemCallback.ItemCallbackListener {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val deletePos = viewHolder.adapterPosition
                val addr = adapter.addresses!![deletePos]
                val deleteItem = adapter.removeItem(viewHolder.adapterPosition)!!
                val bottomSheet = showBottomSheet(addr, asset)
                parentFragmentManager.executePendingTransactions()
                bottomSheet.dialog?.setOnDismissListener {
                    bottomSheet.dismiss()
                    if (!deleteSuccess) {
                        adapter.restoreItem(deleteItem, deletePos)
                    }
                }
            }
        })).apply { attachToRecyclerView(addr_rv) }
        addr_rv.adapter = adapter
        adapter.setAddrListener(addrListener)
        search_et.listener = object : SearchView.OnSearchViewListener {
            override fun afterTextChanged(s: Editable?) {
                adapter.addresses = addresses?.filter {
                    it.label.contains(s.toString(), ignoreCase = true)
                }?.sortedByDescending { it.label == s.toString() }?.toMutableList()
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
            destination = addr.destination,
            label = addr.label,
            tag = addr.tag,
            assetName = asset.name, type = DELETE)
        bottomSheet.showNow(parentFragmentManager, PinAddrBottomSheetDialogFragment.TAG)
        bottomSheet.callback = object : BiometricBottomSheetDialogFragment.Callback {
            override fun onSuccess() {
                deleteSuccess = true
            }
        }
        return bottomSheet
    }
}
