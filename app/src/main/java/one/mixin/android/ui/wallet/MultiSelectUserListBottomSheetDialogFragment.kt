package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding3.widget.textChanges
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSelectListBottomSheetBinding
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.adapter.SelectableAddressAdapter
import one.mixin.android.ui.wallet.adapter.SelectableUserAdapter
import one.mixin.android.ui.wallet.adapter.SelectedUserAdapter
import one.mixin.android.ui.wallet.adapter.WalletSearchAddressCallback
import one.mixin.android.ui.wallet.adapter.WalletSearchUserCallback
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AddressItem
import one.mixin.android.vo.Recipient
import one.mixin.android.vo.UserItem
import one.mixin.android.widget.BottomSheet
import java.util.concurrent.TimeUnit

@SuppressLint("NotifyDataSetChanged")
@AndroidEntryPoint
class MultiSelectUserListBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "MultiSelectUserListBottomSheetDialogFragment"
        const val POS_USER_RV = 0
        const val POS_ADDRESS_RV = 1
        const val POS_EMPTY = 2
        const val POS_EMPTY_USER = 3

        fun newInstance() = MultiSelectUserListBottomSheetDialogFragment()
    }

    private val binding by viewBinding(FragmentSelectListBottomSheetBinding::inflate)

    private val selectedRecipients = mutableListOf<Recipient>()
    private val userAdapter by lazy { SelectableUserAdapter(selectedRecipients) }
    private val addressesAdapter by lazy { SelectableAddressAdapter(selectedRecipients) }

    private var disposable: Disposable? = null
    private var currentSearch: Job? = null
    private var currentAddressSearch: Job? = null
    private var currentQuery: String = ""

    private var defaultAssets = emptyList<UserItem>()
    private var defaultAddress = emptyList<AddressItem>()

    private val groupAdapter: SelectedUserAdapter by lazy {
        SelectedUserAdapter { item ->
            selectedRecipients.remove(item)
            userAdapter.notifyItemChanged(userAdapter.currentList.indexOf(item))
            addressesAdapter.notifyItemChanged(addressesAdapter.currentList.indexOf(item))
            groupAdapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        binding.ph.updateLayoutParams<ViewGroup.LayoutParams> {
            height = requireContext().statusBarHeight() + requireContext().appCompatActionBarHeight()
        }
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }

        binding.apply {
            closeIb.setOnClickListener {
                searchEt.hideKeyboard()
                dismiss()
            }
            radio.isVisible = true
            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.radio_user -> {
                        rvVa.displayedChild = POS_USER_RV
                        searchEt.setHint(getString(R.string.search_placeholder_app))
                    }
                    R.id.radio_address -> {
                        rvVa.displayedChild = POS_ADDRESS_RV
                        searchEt.setHint(getString(R.string.search_placeholder_address))
                    }
                }
            }
            assetRv.adapter = userAdapter
            addressRv.adapter = addressesAdapter
            selectRv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            selectRv.adapter = groupAdapter
            userAdapter.callback =
                object : WalletSearchUserCallback {
                    override fun onUserClick(user: UserItem) {
                        binding.searchEt.hideKeyboard()
                        if (selectedRecipients.contains(user)) {
                            selectedRecipients.remove(user)
                        } else {
                            selectedRecipients.add(user)
                        }
                        userAdapter.notifyItemChanged(userAdapter.currentList.indexOf(user))
                        groupAdapter.checkedUsers = selectedRecipients
                        groupAdapter.notifyDataSetChanged()
                        selectRv.scrollToPosition(selectedRecipients.size - 1)
                    }
                }
            addressesAdapter.callback =
                object :WalletSearchAddressCallback {
                    override fun onAddressClick(address: AddressItem) {
                        binding.searchEt.hideKeyboard()
                        if (selectedRecipients.contains(address)) {
                            selectedRecipients.remove(address)
                        } else {
                            selectedRecipients.add(address)
                        }
                        addressesAdapter.notifyItemChanged(addressesAdapter.currentList.indexOf(address))
                        groupAdapter.checkedUsers = selectedRecipients
                        groupAdapter.notifyDataSetChanged()
                        selectRv.scrollToPosition(selectedRecipients.size - 1)
                    }
                }
            depositTitle.setText(R.string.No_users)
            depositTv.isVisible = false
            searchEt.setHint(getString(R.string.search_placeholder_app))
            disposable =
                searchEt.et.textChanges().debounce(500L, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(destroyScope)
                    .subscribe(
                        {
                            if (it.isNullOrBlank()) {
                                binding.rvVa.displayedChild = if(binding.radioGroup.checkedRadioButtonId == R.id.radio_user) POS_USER_RV else POS_ADDRESS_RV
                                userAdapter.submitList(defaultAssets)
                            } else {
                                if (it.toString() != currentQuery) {
                                    currentQuery = it.toString()
                                    search(it.toString())
                                }
                            }
                        },
                        {},
                    )
            resetButton.setOnClickListener {
                selectedRecipients.clear()
                onMultiSelectRecipientListener?.onRecipientSelect(null)
                dismiss()
            }
            applyButton.setOnClickListener {
                onMultiSelectRecipientListener?.onRecipientSelect(selectedRecipients)
                dismiss()
            }
        }

        bottomViewModel.allUser()
            .observe(this) {
                defaultAssets = it
                if (binding.searchEt.et.text.isNullOrBlank()) {
                    userAdapter.submitList(defaultAssets)
                }
                if (defaultAssets.isEmpty()) {
                    binding.rvVa.displayedChild = POS_EMPTY
                }
            }

        bottomViewModel.allAddresses()
            .observe(this) {
                defaultAddress = it
                if (binding.searchEt.et.text.isNullOrBlank()) {
                    addressesAdapter.submitList(defaultAddress)
                }
                if (defaultAddress.isEmpty()) {
                    binding.rvVa.displayedChild = POS_EMPTY
                }
            }
    }

    private fun search(query: String) {
        searchUser(query)
        searchAddress(query)
    }

    private fun searchUser(query: String) {
        currentSearch?.cancel()
        currentSearch =
            lifecycleScope.launch {
                if (!isAdded) return@launch

                binding.rvVa.displayedChild = if(binding.radioGroup.checkedRadioButtonId == R.id.radio_user) POS_USER_RV else POS_ADDRESS_RV
                binding.pb.isVisible = true

                val localAssets = defaultAssets.filter {
                    it.fullName?.contains(query) == true || it.identityNumber.contains(query)
                }
                userAdapter.submitList(localAssets) {
                    binding.assetRv.scrollToPosition(0)
                }
                binding.pb.isVisible = false

                if (localAssets.isEmpty() && binding.radioGroup.checkedRadioButtonId == R.id.radio_user) {
                    binding.rvVa.displayedChild = POS_EMPTY_USER
                }
            }
    }
    private fun searchAddress(query: String) {
        currentAddressSearch?.cancel()
        currentAddressSearch =
            lifecycleScope.launch {
                if (!isAdded) return@launch

                binding.rvVa.displayedChild = if(binding.radioGroup.checkedRadioButtonId == R.id.radio_user) POS_USER_RV else POS_ADDRESS_RV
                binding.pb.isVisible = true

                val localAddress = defaultAddress.filter {
                    it.label.contains(query) || it.destination.contains(query) || it.tag?.contains(query) == true
                }
                addressesAdapter.submitList(localAddress) {
                    binding.addressRv.scrollToPosition(0)
                }
                binding.pb.isVisible = false

                if (localAddress.isEmpty() && binding.radioGroup.checkedRadioButtonId == R.id.radio_address) {
                    binding.rvVa.displayedChild = POS_EMPTY_USER
                }
            }
    }

    fun setOnMultiSelectUserListener(onMultiSelectUserListener: OnMultiSelectRecipientListener): MultiSelectUserListBottomSheetDialogFragment {
        this.onMultiSelectRecipientListener = onMultiSelectUserListener
        return this
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onMultiSelectRecipientListener?.onDismiss()
    }

    private var onMultiSelectRecipientListener: OnMultiSelectRecipientListener? = null

    interface OnMultiSelectRecipientListener {
        fun onRecipientSelect(users: List<Recipient>?)
        fun onDismiss()
    }
}