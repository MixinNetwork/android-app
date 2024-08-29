package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.db.TokenDao.Companion.PREFIX_ASSET_ITEM
import one.mixin.android.vo.market.MarketCoin
import one.mixin.android.vo.safe.TokenItem

@Dao
interface MarketCoinDao : BaseDao<MarketCoin> {
    @Query("$PREFIX_ASSET_ITEM LEFT JOIN market_coins mc on mc.asset_id = a1.asset_id WHERE mc.coin_id = :coinId LIMIT 1")
    suspend fun findTokenByCoinId(coinId:String):TokenItem?
}