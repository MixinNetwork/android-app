package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.Editable
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAssetListBottomSheetBinding
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.navTo
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.friends.FriendsListener
import one.mixin.android.ui.common.friends.UserItemCallback
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.ui.conversation.adapter.FriendsAdapter
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SearchView

@AndroidEntryPoint
class TransferContactBottomSheetDialogFragment : MixinBottomSheetDialogFragment(), FriendsListener {
    companion object {
        const val TAG = "TransferContactBottomSheetDialogFragment"
        fun newInstance(token: TokenItem) =
            TransferContactBottomSheetDialogFragment().withArgs {
                putParcelable(TransactionsFragment.Companion.ARGS_ASSET, token)
            }
    }

    private val binding by viewBinding(FragmentAssetListBottomSheetBinding::inflate)

    private val adapter by lazy {
        FriendsAdapter(userCallback).apply {
            listener = this@TransferContactBottomSheetDialogFragment
        }
    }

    private val token: TokenItem by lazy {
        requireNotNull(
            requireArguments().getParcelableCompat(
                TransactionsFragment.Companion.ARGS_ASSET,
                TokenItem::class.java
            )
        )
    }

    private val userCallback = UserItemCallback("")

    private var users: List<User> = listOf()
        set(value) {
            field = value
            dataChange()
        }
    private var keyWord: String = ""
        set(value) {
            field = value
            dataChange()
        }

    private val viewModel by viewModels<ConversationViewModel>()

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root

        binding.ph.updateLayoutParams<ViewGroup.LayoutParams> {
            height =
                requireContext().statusBarHeight() + requireContext().appCompatActionBarHeight()
        }
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }

        binding.apply {
            searchEt.et.setHint(R.string.setting_auth_search_hint)
            assetRv.adapter = adapter
            closeIb.setOnClickListener {
                searchEt.hideKeyboard()
                dismiss()
            }
            lifecycleScope.launch {
                users = viewModel.findFriendsAndMyBot()
            }
            depositTv.setOnClickListener {
                dismiss()
            }
            searchEt.listener =
                object : SearchView.OnSearchViewListener {
                    override fun afterTextChanged(s: Editable?) {
                        filter(s.toString())
                    }

                    override fun onSearch() {}
                }
        }
    }

    private fun filter(s: String) {
        keyWord = s.toString()
        adapter.filter = keyWord
        userCallback.filter = keyWord
    }

    private fun dataChange() {
        adapter.submitList(
            if (keyWord.isNotBlank()) {
                users.filter {
                    it.fullName?.containsIgnoreCase(keyWord) == true ||
                        it.identityNumber.containsIgnoreCase(keyWord)
                }.sortedByDescending {
                    it.fullName.equalsIgnoreCase(keyWord) || it.identityNumber.equalsIgnoreCase(
                        keyWord
                    )
                }
            } else {
                users
            },
        )
    }

    override fun onItemClick(user: User) {
        onUserClick?.invoke(user)
        dismiss()
    }

    var onUserClick: ((User) -> Unit)? = null
}