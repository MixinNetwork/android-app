package one.mixin.android.ui.search
import one.mixin.android.ui.search.holder.TipItem
import one.mixin.android.vo.Dapp
import one.mixin.android.vo.SearchBot
import one.mixin.android.vo.market.Market
import kotlin.math.min

class SearchExploreDataPackage(
    var marketList: List<Market>? = null,
    var dappList: List<Dapp>? = null,
    var botList: List<SearchBot>? = null,
    var url: String? = null,
) {
    companion object {
        const val LIMIT_COUNT = 3
    }

    var showTip = false

    private var marketLimit = true
    private var botLimit = true
    private var dappLimit = true

    fun getHeaderFactor(position: Int) =
        when (getItem(position)) {
            is Market -> if (marketShowMore()) 10 else 0
            is Dapp -> if (dappShowMore()) 10 else 0
            is SearchBot -> if (botShowMore()) 10 else 0
            else -> 0
        }

    fun marketShowMore(): Boolean {
        val marketList = this.marketList
        return if (marketList == null || !marketLimit) {
            false
        } else {
            marketLimit && marketList.size > LIMIT_COUNT
        }
    }

    fun dappShowMore(): Boolean {
        val dappList = this.dappList
        return if (dappList == null || !dappLimit) {
            false
        } else {
            dappLimit && dappList.size > LIMIT_COUNT
        }
    }

    fun botShowMore(): Boolean {
        val botList = this.botList
        return if (botList == null || !botLimit) {
            false
        } else {
            botLimit && botList.size > LIMIT_COUNT
        }
    }

    private fun marketCount() =
        if (marketLimit) {
            min(marketList?.size ?: 0, LIMIT_COUNT)
        } else {
            marketList?.size ?: 0
        }

    private fun dappCount() = dappList?.size ?: 0

    private fun botCount() =
        if (botLimit) {
            min(botList?.size ?: 0, LIMIT_COUNT)
        } else {
            botList?.size ?: 0
        }

    fun getCount() = marketCount() + dappCount() + botCount().incTip()

    private fun marketItem(position: Int): Market? {
        return marketList?.get(position.decTip())
    }

    private fun dappItem(position: Int): Dapp? {
        return dappList?.get(position.decTip() - marketCount())
    }

    private fun botItem(position: Int): SearchBot? {
        return botList?.get(position.decTip() - marketCount() - dappCount())
    }

    fun getItem(position: Int): Any? {
        return when {
            showTip && position < 1 -> TipItem()
            position < marketCount().incTip() -> marketItem(position)
            position < marketCount().incTip() + dappCount() -> dappItem(position)
            else -> botItem(position)
        }
    }

    private fun Int.incTip() = this + if (showTip) 1 else 0

    private fun Int.decTip() = this - if (showTip) 1 else 0
}
