package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.db.TokenDao.Companion.PREFIX_ASSET_ITEM
import one.mixin.android.vo.market.MarketCoin
import one.mixin.android.vo.safe.TokenItem

@Dao
interface MarketCoinDao : BaseDao<MarketCoin> {
    @Query("$PREFIX_ASSET_ITEM LEFT JOIN market_coins mc on mc.asset_id = a1.asset_id WHERE mc.coin_id = :coinId")
    suspend fun findTokensByCoinId(coinId: String): List<TokenItem>

    @Query("SELECT asset_id FROM market_coins WHERE coin_id = :coinId")
    suspend fun findTokenIdsByCoinId(coinId: String): List<String>

    @Query("SELECT coin_id FROM market_coins WHERE asset_id = :assetId")
    suspend fun findCoinIdByTokenId(assetId: String): String?

    @Query("DELETE FROM market_coins WHERE coin_id = :coinId AND asset_id NOT IN (:assetIds)")
    suspend fun deleteMarketCoinsNotInAssetIds(coinId: String, assetIds: List<String>)

    @Query("DELETE FROM market_coins WHERE coin_id = :coinId")
    suspend fun deleteByCoinId(coinId: String)
}