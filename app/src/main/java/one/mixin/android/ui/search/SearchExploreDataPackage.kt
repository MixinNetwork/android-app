package one.mixin.android.ui.search
import one.mixin.android.vo.Dapp
import one.mixin.android.vo.User
import one.mixin.android.vo.market.Market
import one.mixin.android.vo.market.MarketItem
import kotlin.math.min

class SearchExploreDataPackage(
    var marketList: List<Market>? = null,
    var dappList: List<Dapp>? = null,
    var botList: List<User>? = null,
    var url: String? = null,
) {
    companion object {
        const val LIMIT_COUNT = 3
    }

    var showTip = false

    private var marketLimit = true
    private var botLimit = true

    fun getHeaderFactor(position: Int) =
        when (getItem(position)) {
            is MarketItem -> if (marketShowMore()) 10 else 0
            is User -> {
                when {
                    position < marketCount() + dappCount() -> if (dappShowMore()) 10 else 0
                    else -> if (botShowMore()) 10 else 0
                }
            }
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
        return false
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

    fun getCount() = marketCount() + dappCount() + botCount()

    private fun marketItem(position: Int): Market? {
        return marketList?.get(position)
    }

    private fun dappItem(position: Int): Dapp? {
        return dappList?.get(position - marketCount())
    }

    private fun botItem(position: Int): User? {
        return botList?.get(position - marketCount() - dappCount())
    }

    fun getItem(position: Int): Any? {
        return when {
            position < marketCount() -> marketItem(position)
            position < marketCount() + dappCount() -> dappItem(position)
            else -> botItem(position)
        }
    }
}
