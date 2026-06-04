package one.mixin.android.ui.wallet.home

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.databinding.ItemWeb3TransactionsBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.extension.dp
import one.mixin.android.ui.common.recyclerview.HeaderAdapter
import one.mixin.android.ui.home.web3.trade.perps.OpenPositionItem
import one.mixin.android.ui.wallet.adapter.OnSnapshotListener
import one.mixin.android.ui.wallet.adapter.SnapshotAdapter
import one.mixin.android.ui.wallet.adapter.WalletAssetAdapter
import one.mixin.android.ui.wallet.adapter.WalletWeb3TokenAdapter
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.web3.details.Web3TransactionHolder

@Composable
fun PrivacyTokenRecycler(
    tokens: List<TokenItem>,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemSpacing: Int = 0.dp,
) {
    val currentTokens = rememberUpdatedState(tokens)
    val currentOnClick = rememberUpdatedState(onClick)
    AndroidView(
        modifier = modifier,
        factory = { context ->
            RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context)
                itemAnimator = null
                isNestedScrollingEnabled = false
                if (itemSpacing > 0) {
                    addItemDecoration(TokenItemSpacingDecoration(itemSpacing))
                }
                adapter = WalletAssetAdapter(false, compact = true).apply {
                    onItemListener = object : HeaderAdapter.OnItemListener {
                        override fun <T> onNormalItemClick(item: T) {
                            currentOnClick.value(currentTokens.value.indexOf(item as TokenItem))
                        }
                    }
                }
            }
        },
        update = { recyclerView ->
            (recyclerView.adapter as WalletAssetAdapter).apply {
                onItemListener = object : HeaderAdapter.OnItemListener {
                    override fun <T> onNormalItemClick(item: T) {
                        currentOnClick.value(currentTokens.value.indexOf(item as TokenItem))
                    }
                }
                setAssetList(tokens)
            }
        },
    )
}

@Composable
fun Web3TokenRecycler(
    tokens: List<Web3TokenItem>,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemSpacing: Int = 0.dp,
) {
    val currentTokens = rememberUpdatedState(tokens)
    val currentOnClick = rememberUpdatedState(onClick)
    AndroidView(
        modifier = modifier,
        factory = { context ->
            RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context)
                itemAnimator = null
                isNestedScrollingEnabled = false
                if (itemSpacing > 0) {
                    addItemDecoration(TokenItemSpacingDecoration(itemSpacing))
                }
                adapter = WalletWeb3TokenAdapter(false, compact = true).apply {
                    onItemListener = object : HeaderAdapter.OnItemListener {
                        override fun <T> onNormalItemClick(item: T) {
                            currentOnClick.value(currentTokens.value.indexOf(item as Web3TokenItem))
                        }
                    }
                }
            }
        },
        update = { recyclerView ->
            (recyclerView.adapter as WalletWeb3TokenAdapter).apply {
                onItemListener = object : HeaderAdapter.OnItemListener {
                    override fun <T> onNormalItemClick(item: T) {
                        currentOnClick.value(currentTokens.value.indexOf(item as Web3TokenItem))
                    }
                }
                setAssetList(tokens)
            }
        },
    )
}

@Composable
fun PrivacyTransactionRecycler(
    transactions: List<SnapshotItem>,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemSpacing: Int = 20.dp,
) {
    val currentTransactions = rememberUpdatedState(transactions)
    val currentOnClick = rememberUpdatedState(onClick)
    AndroidView(
        modifier = modifier,
        factory = { context ->
            RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context)
                itemAnimator = null
                isNestedScrollingEnabled = false
                if (itemSpacing > 0) {
                    addItemDecoration(TokenItemSpacingDecoration(itemSpacing))
                }
                adapter = SnapshotAdapter(compact = true).apply {
                    listener = object : OnSnapshotListener {
                        override fun <T> onNormalItemClick(item: T) {
                            currentOnClick.value(currentTransactions.value.indexOf(item as SnapshotItem))
                        }

                        override fun onUserClick(userId: String) = Unit

                        override fun onMoreClick() = Unit
                    }
                }
            }
        },
        update = { recyclerView ->
            (recyclerView.adapter as SnapshotAdapter).apply {
                listener = object : OnSnapshotListener {
                    override fun <T> onNormalItemClick(item: T) {
                        currentOnClick.value(currentTransactions.value.indexOf(item as SnapshotItem))
                    }

                    override fun onUserClick(userId: String) = Unit

                    override fun onMoreClick() = Unit
                }
                list = transactions
            }
        },
    )
}

@Composable
fun Web3TransactionRecycler(
    transactions: List<Web3TransactionItem>,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemSpacing: Int = 20.dp,
) {
    val currentOnClick = rememberUpdatedState(onClick)
    AndroidView(
        modifier = modifier,
        factory = { context ->
            RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context)
                itemAnimator = null
                isNestedScrollingEnabled = false
                if (itemSpacing > 0) {
                    addItemDecoration(TokenItemSpacingDecoration(itemSpacing))
                }
                adapter = Web3TransactionListAdapter(compact = true)
            }
        },
        update = { recyclerView ->
            (recyclerView.adapter as Web3TransactionListAdapter).apply {
                updateOnClick { currentOnClick.value(it) }
                submitList(transactions)
            }
        },
    )
}

@Composable
fun PositionRecycler(
    positions: List<PerpsPositionItem>,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    itemSpacing: Int = 0.dp,
) {
    val currentOnClick = rememberUpdatedState(onClick)
    AndroidView(
        modifier = modifier,
        factory = { context ->
            RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context)
                itemAnimator = null
                isNestedScrollingEnabled = false
                if (itemSpacing > 0) {
                    addItemDecoration(TokenItemSpacingDecoration(itemSpacing))
                }
                adapter = PositionListAdapter(compact)
            }
        },
        update = { recyclerView ->
            (recyclerView.adapter as PositionListAdapter).apply {
                updateOnClick { currentOnClick.value(it) }
                submitList(positions)
            }
        },
    )
}

private class TokenItemSpacingDecoration(
    private val spacing: Int,
) : ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position != RecyclerView.NO_POSITION && position < state.itemCount - 1) {
            outRect.bottom = spacing
        }
    }
}

private class Web3TransactionListAdapter(
    private val compact: Boolean,
) : RecyclerView.Adapter<Web3TransactionHolder>() {
    private var items: List<Web3TransactionItem> = emptyList()
    private var onClick: (Int) -> Unit = {}

    fun submitList(newItems: List<Web3TransactionItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateOnClick(onClick: (Int) -> Unit) {
        this.onClick = onClick
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Web3TransactionHolder {
        val binding = ItemWeb3TransactionsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Web3TransactionHolder(binding, compact)
    }

    override fun onBindViewHolder(holder: Web3TransactionHolder, position: Int) {
        holder.bind(items[position])
        holder.itemView.setOnClickListener { onClick(position) }
    }

    override fun getItemCount(): Int = items.size
}

private class PositionListAdapter(
    private val compact: Boolean,
) : RecyclerView.Adapter<PositionListAdapter.PositionHolder>() {
    private var items: List<PerpsPositionItem> = emptyList()
    private var onClick: (Int) -> Unit = {}

    fun submitList(newItems: List<PerpsPositionItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateOnClick(onClick: (Int) -> Unit) {
        this.onClick = onClick
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PositionHolder {
        return PositionHolder(
            ComposeView(parent.context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            }
        )
    }

    override fun onBindViewHolder(holder: PositionHolder, position: Int) {
        holder.bind(items[position], compact) { onClick(position) }
    }

    override fun getItemCount(): Int = items.size

    class PositionHolder(
        private val composeView: ComposeView,
    ) : RecyclerView.ViewHolder(composeView) {
        fun bind(
            position: PerpsPositionItem,
            compact: Boolean,
            onClick: () -> Unit,
        ) {
            composeView.setContent {
                MixinAppTheme {
                    OpenPositionItem(position = position, onClick = onClick, compact = compact)
                }
            }
        }
    }
}
