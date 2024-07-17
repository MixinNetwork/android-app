package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
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
import one.mixin.android.ui.wallet.adapter.SelectableUserAdapter
import one.mixin.android.ui.wallet.adapter.SelectedUserAdapter
import one.mixin.android.ui.wallet.adapter.WalletSearchUserCallback
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.User
import one.mixin.android.widget.BottomSheet
import java.util.concurrent.TimeUnit

@SuppressLint("NotifyDataSetChanged")
@AndroidEntryPoint
class MultiSelectUserListBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "UserListBottomSheetDialogFragment"
        const val POS_RV = 0
        const val POS_EMPTY = 1
        const val POS_EMPTY_USER = 2

        fun newInstance() = MultiSelectUserListBottomSheetDialogFragment()
    }

    private val binding by viewBinding(FragmentSelectListBottomSheetBinding::inflate)

    private val selectedUsers = mutableListOf<User>()
    private val adapter by lazy { SelectableUserAdapter(selectedUsers) }

    private var disposable: Disposable? = null
    private var currentSearch: Job? = null
    private var currentQuery: String = ""
    private var defaultAssets = emptyList<User>()

    private val groupAdapter: SelectedUserAdapter by lazy {
        SelectedUserAdapter { user ->
            selectedUsers.remove(user)
            adapter.notifyItemChanged(adapter.currentList.indexOf(user))
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
            assetRv.adapter = adapter
            selectRv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            selectRv.adapter = groupAdapter
            adapter.callback =
                object : WalletSearchUserCallback {
                    override fun onUserClick(user: User) {
                        binding.searchEt.hideKeyboard()
                        if (selectedUsers.contains(user)) {
                            selectedUsers.remove(user)
                        } else {
                            selectedUsers.add(user)
                        }
                        adapter.notifyItemChanged(adapter.currentList.indexOf(user))
                        groupAdapter.checkedUsers = selectedUsers
                        groupAdapter.notifyDataSetChanged()
                        selectRv.scrollToPosition(selectedUsers.size - 1)
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
                                binding.rvVa.displayedChild = POS_RV
                                adapter.submitList(defaultAssets)
                            } else {
                                if (it.toString() != currentQuery) {
                                    currentQuery = it.toString()
                                    search(it.toString())
                                }
                            }
                        },
                        {},
                    )
        }

        bottomViewModel.allUser()
            .observe(this) {
                defaultAssets = it
                if (binding.searchEt.et.text.isNullOrBlank()) {
                    adapter.submitList(defaultAssets)
                }
                if (defaultAssets.isEmpty()) {
                    binding.rvVa.displayedChild = POS_EMPTY
                }
            }
    }

    private fun search(query: String) {
        currentSearch?.cancel()
        currentSearch =
            lifecycleScope.launch {
                if (!isAdded) return@launch

                binding.rvVa.displayedChild = POS_RV
                binding.pb.isVisible = true

                val localAssets = defaultAssets.filter {
                    it.fullName?.contains(query) == true || it.identityNumber.contains(query)
                }
                adapter.submitList(localAssets) {
                    binding.assetRv.scrollToPosition(0)
                }
                binding.pb.isVisible = false

                if (localAssets.isEmpty()) {
                    binding.rvVa.displayedChild = POS_EMPTY_USER
                }
            }
    }

    fun setOnUserClick(callback: (User) -> Unit): MultiSelectUserListBottomSheetDialogFragment {
        this.onUser = callback
        return this
    }

    private var onUser: ((User) -> Unit)? = null
}