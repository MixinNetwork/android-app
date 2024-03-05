package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants.ARGS_USER_ID
import one.mixin.android.databinding.FragmentTransactionsUserBottomSheetBinding
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.wallet.adapter.OnSnapshotListener
import one.mixin.android.ui.wallet.adapter.SnapshotPagedAdapter
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.widget.BottomSheet

@Suppress("DEPRECATION")
@AndroidEntryPoint
class UserTransactionBottomSheetFragment : BaseTransactionsBottomSheetFragment<PagedList<SnapshotItem>>(), OnSnapshotListener {
    companion object {
        const val TAG = "UserTransactionBottomSheetFragment"

        fun newInstance(userId: String) =
            UserTransactionBottomSheetFragment().withArgs {
                putString(ARGS_USER_ID, userId)
            }
    }

    private val binding by viewBinding(FragmentTransactionsUserBottomSheetBinding::inflate)

    private val adapter = SnapshotPagedAdapter()

    private val userId by lazy {
        requireArguments().getString(ARGS_USER_ID)!!
    }

    override fun initContentView() {
        contentView = binding.root
        binding.ph.updateLayoutParams<ViewGroup.LayoutParams> {
            height = requireContext().statusBarHeight()
        }
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        binding.transactionsRv.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        binding.titleView.rightAnimator.visibility = View.GONE
        binding.titleView.leftIb.setOnClickListener { dismiss() }
        adapter.listener = this
        binding.transactionsRv.adapter = adapter
        dataObserver =
            Observer {
                if (it.isNotEmpty()) {
                    showEmpty(false)
                } else {
                    showEmpty(true)
                }
                adapter.submitList(it)
            }
        bindLiveData(walletViewModel.snapshotsByUserId(userId, initialLoadKey))
    }

    override fun <T> onNormalItemClick(item: T) {
        val snapshot = item as SnapshotItem
        lifecycleScope.launch {
            val assetItem = walletViewModel.simpleAssetItem(snapshot.assetId)
            TransactionBottomSheetDialogFragment.newInstance(snapshot, assetItem)
                .showNow(parentFragmentManager, TransactionBottomSheetDialogFragment.TAG)
        }
    }

    override fun onUserClick(userId: String) {
        // Do nothing, avoid recursively calling this page.
    }

    private fun showEmpty(show: Boolean) {
        if (show) {
            if (binding.empty.emptyRl.visibility == View.GONE) {
                binding.empty.emptyRl.visibility = View.VISIBLE
            }
            if (binding.transactionsRv.visibility == View.VISIBLE) {
                binding.transactionsRv.visibility = View.GONE
            }
        } else {
            if (binding.empty.emptyRl.visibility == View.VISIBLE) {
                binding.empty.emptyRl.visibility = View.GONE
            }
            if (binding.transactionsRv.visibility == View.GONE) {
                binding.transactionsRv.visibility = View.VISIBLE
            }
        }
    }

    override fun onApplyClick() {
        // Left empty
    }
}
