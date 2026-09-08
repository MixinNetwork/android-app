package one.mixin.android.db.web3

import androidx.room3.Dao
import androidx.room3.Query
import one.mixin.android.db.BaseDao
import one.mixin.android.vo.market.MarketCoin

@Dao
interface Web3MarketCoinDao : BaseDao<MarketCoin> {
    @Query("SELECT * FROM market_coins WHERE asset_id IN (:assetIds)")
    suspend fun findByAssetIds(assetIds: List<String>): List<MarketCoin>

    @Query("DELETE FROM market_coins")
    suspend fun deleteAll()
}
