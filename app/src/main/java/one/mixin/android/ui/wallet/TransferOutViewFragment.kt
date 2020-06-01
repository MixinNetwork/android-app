package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import kotlin.math.abs
import kotlinx.android.synthetic.main.fragment_transfer_out.view.*
import kotlinx.android.synthetic.main.layout_empty_transaction.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.hashForDate
import one.mixin.android.extension.inflate
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.address.AddressAddFragment
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.wallet.BaseTransactionsFragment.Companion.LIMIT
import one.mixin.android.ui.wallet.adapter.OnSnapshotListener
import one.mixin.android.ui.wallet.adapter.SnapshotHeaderViewHolder
import one.mixin.android.ui.wallet.adapter.SnapshotHolder
import one.mixin.android.util.ErrorHandler.Companion.errorHandler
import one.mixin.android.vo.Address
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.SnapshotItem.Companion.fromSnapshot
import one.mixin.android.widget.BottomSheet

class TransferOutViewFragment : MixinBottomSheetDialogFragment(), OnSnapshotListener {

    companion object {
        const val TAG = "TransferOutViewController"
        private const val ARGS_USER_AVATAR_URL = "args_user_avatar_url"
        private const val ARGS_SYMBOL = "args_symbol"
        fun newInstance(
            assetId: String,
            userId: String? = null,
            userAvatarUrl: String? = null,
            symbol: String? = null,
            address: Address? = null
        ) = TransferOutViewFragment().withArgs {
            putString(Constants.ARGS_ASSET_ID, assetId)
            userId?.let { putString(Constants.ARGS_USER_ID, it) }
            userAvatarUrl?.let { putString(ARGS_USER_AVATAR_URL, it) }
            symbol?.let { putString(ARGS_SYMBOL, it) }
            address?.let { putParcelable(AddressAddFragment.ARGS_ADDRESS, it) }
        }
    }

    private val assetId: String by lazy { requireArguments().getString(Constants.ARGS_ASSET_ID)!! }
    private val userId: String? by lazy { requireArguments().getString(Constants.ARGS_USER_ID) }
    private val avatarUrl: String? by lazy { requireArguments().getString(ARGS_USER_AVATAR_URL) }
    private val symbol: String? by lazy { requireArguments().getString(ARGS_SYMBOL) }
    private val address: Address? by lazy { requireArguments().getParcelable<Address>(AddressAddFragment.ARGS_ADDRESS) }
    private val adapter = SnapshotPagedAdapter()

    private val walletViewModel: WalletViewModel by viewModels { viewModelFactory }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_transfer_out, null)
        contentView.ph.updateLayoutParams<ViewGroup.LayoutParams> {
            height = requireContext().statusBarHeight()
        }
        contentView.title_view.left_ib.setOnClickListener { dismiss() }
        contentView.transactions_rv.adapter = adapter
        contentView.transactions_rv.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        contentView.transactions_rv.setOnScrollChangeListener { list, _, _, _, _ ->
            if (isAdded) {
                if (!list.canScrollVertically(1)) {
                    loadMore()
                }
            }
        }

        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }
        adapter.listener = this
        loadMore()
    }

    private var hasMore = true
    private var isLoading = false
    private fun loadMore() {
        if (isLoading || !hasMore) {
            return
        }
        isLoading = true

        walletViewModel.viewModelScope.launch(errorHandler) {
            val result = walletViewModel.getSnapshots(
                assetId,
                opponent = userId,
                destination = address?.destination,
                tag = address?.tag,
                offset = adapter.getLastSnapshotCreated(),
                limit = LIMIT
            )
            if (result.isSuccess && result.data?.isNotEmpty() == true) {
                if (result.data?.size!! < LIMIT) {
                    hasMore = false
                }
                adapter.list.addAll(result.data!!.map {
                    fromSnapshot(it, avatarUrl, symbol)
                })
                adapter.notifyDataSetChanged()
            } else {
                hasMore = false
            }
            showEmpty(adapter.list.isEmpty())
            isLoading = false
        }
    }

    class SnapshotPagedAdapter : RecyclerView.Adapter<SnapshotHolder>(),
        StickyRecyclerHeadersAdapter<SnapshotHeaderViewHolder> {

        var list: MutableList<SnapshotItem> = mutableListOf()

        var listener: OnSnapshotListener? = null

        fun getLastSnapshotCreated(): String? = if (list.isEmpty()) {
            null
        } else {
            list.last().createdAt
        }

        override fun getHeaderId(pos: Int): Long {
            val snapshot = getItem(pos)
            return abs(snapshot.createdAt.hashForDate())
        }

        override fun onCreateHeaderViewHolder(parent: ViewGroup) =
            SnapshotHeaderViewHolder(parent.inflate(R.layout.item_transaction_header, false))

        override fun onBindHeaderViewHolder(vh: SnapshotHeaderViewHolder, pos: Int) {
            getItem(pos).let {
                vh.bind(it.createdAt)
            }
        }

        override fun onBindViewHolder(holder: SnapshotHolder, position: Int) {
            getItem(position).let {
                holder.bind(it, listener)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SnapshotHolder {
            return SnapshotHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_wallet_transactions, parent, false)
            )
        }

        private fun getItem(position: Int): SnapshotItem = list[position]

        override fun getItemCount(): Int = list.notNullWithElse({ it.size }, 0)
    }

    private fun showEmpty(show: Boolean) {
        contentView.progress.visibility = GONE
        if (show) {
            if (contentView.empty_rl.visibility == GONE) {
                contentView.empty_rl.visibility = VISIBLE
            }
            if (contentView.transactions_rv.visibility == VISIBLE) {
                contentView.transactions_rv.visibility = GONE
            }
        } else {
            if (contentView.empty_rl.visibility == VISIBLE) {
                contentView.empty_rl.visibility = GONE
            }
            if (contentView.transactions_rv.visibility == GONE) {
                contentView.transactions_rv.visibility = VISIBLE
            }
        }
    }

    override fun <T> onNormalItemClick(item: T) {
                 lifecycleScope.launch(Dispatchers.IO) {
            val snapshot = item as SnapshotItem
            walletViewModel.simpleAssetItem(snapshot.assetId)?.let { assetItem ->
                TransactionBottomSheetDialogFragment.newInstance(snapshot, assetItem).show(parentFragmentManager, TransactionBottomSheetDialogFragment.TAG)
            }
        }
    }

    override fun onUserClick(userId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            walletViewModel.getUser(userId)?.let {
                withContext(Dispatchers.Main) {
                    val f = UserBottomSheetDialogFragment.newInstance(it)
                    f.show(parentFragmentManager, UserBottomSheetDialogFragment.TAG)
                }
            }
        }
    }
}
