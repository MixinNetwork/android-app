package one.mixin.android.ui.search

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import one.mixin.android.R
import one.mixin.android.databinding.ItemSearchContactBinding
import one.mixin.android.databinding.ItemSearchDappBinding
import one.mixin.android.databinding.ItemSearchHeaderBinding
import one.mixin.android.databinding.ItemSearchMarketBinding
import one.mixin.android.databinding.ItemSearchTipBinding
import one.mixin.android.ui.search.holder.BotHolder
import one.mixin.android.ui.search.holder.DappHolder
import one.mixin.android.ui.search.holder.HeaderHolder
import one.mixin.android.ui.search.holder.MarketHolder
import one.mixin.android.ui.search.holder.UrlHolder
import one.mixin.android.vo.Dapp
import one.mixin.android.vo.SearchBot
import one.mixin.android.vo.market.Market

class SearchExploreAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(), StickyRecyclerHeadersAdapter<HeaderHolder> {
    var onItemClickListener: SearchFragment.OnSearchClickListener? = null
    var query: String = ""
        set(value) {
            field = value
            data.showTip = shouldTips()
        }

    private var data = SearchExploreDataPackage()

    override fun getHeaderId(position: Int): Long =
        if (position == 0 && data.showTip) {
            -1
        } else {
            getItemViewType(position).toLong() + data.getHeaderFactor(position)
        }

    override fun onBindHeaderViewHolder(holder: HeaderHolder, position: Int) {
        val context = holder.itemView.context
        when (getItemViewType(position)) {
            TypeMarket.index -> holder.bind(context.getText(R.string.ASSETS).toString(), data.marketShowMore())
            TypeDapp.index -> holder.bind(context.getText(R.string.DAPPS).toString(), data.dappShowMore())
            TypeBot.index -> holder.bind(context.getText(R.string.BOTS).toString(), data.botShowMore())
        }
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderHolder {
        return HeaderHolder(ItemSearchHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        data = SearchExploreDataPackage()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(marketList: List<Market>?, dappList: List<Dapp>?, botList: List<SearchBot>?, url: String?) {
        data = SearchExploreDataPackage(marketList, dappList, botList, url)
        data.showTip = shouldTips()
        notifyDataSetChanged()
    }

    private fun shouldTips(): Boolean {
        return !data.url.isNullOrBlank()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setDapps(dapps: List<Dapp>?) {
        data.dappList = dapps
        data.showTip = shouldTips()
        notifyDataSetChanged()
    }
    @SuppressLint("NotifyDataSetChanged")
    fun setMarkets(markets: List<Market>?) {
        data.marketList = markets
        data.showTip = shouldTips()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setBots(users:List<SearchBot>?) {
        data.botList = users
        data.showTip = shouldTips()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setUrlData(url: String?) {
        data.url = url
        data.showTip = shouldTips()
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            0 -> (holder as UrlHolder).bind(query, onItemClickListener)
            TypeMarket.index -> (holder as MarketHolder).bind(data.getItem(position) as Market, query, onItemClickListener)
            TypeDapp.index -> (holder as DappHolder).bind(data.getItem(position) as Dapp, query, onItemClickListener)
            TypeBot.index -> (holder as BotHolder).bind(data.getItem(position) as SearchBot, query, onItemClickListener)
        }
    }

    override fun getItemCount(): Int = data.getCount()

    fun getTypeData(position: Int) =
        when (getItemViewType(position)) {
            TypeMarket.index -> if (data.marketShowMore()) data.marketList else null
            TypeDapp.index -> if (data.dappShowMore()) data.dappList else null
            TypeBot.index -> if (data.botShowMore()) data.botList else null
            else -> if (data.botShowMore()) data.botList else null
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            0 -> UrlHolder(ItemSearchTipBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            TypeMarket.index -> MarketHolder(ItemSearchMarketBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            TypeDapp.index -> DappHolder(ItemSearchDappBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            TypeBot.index -> BotHolder(ItemSearchContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> BotHolder(ItemSearchContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

    override fun getItemViewType(position: Int) =
        when (data.getItem(position)) {
            is Market -> TypeMarket.index
            is Dapp -> TypeDapp.index
            is SearchBot -> TypeBot.index
            else -> 0
        }
}

