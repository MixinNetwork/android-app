package one.mixin.android.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.RoomWarnings
import kotlinx.coroutines.flow.Flow
import one.mixin.android.db.TokenDao.Companion.PREFIX_ASSET_ITEM
import one.mixin.android.vo.market.MarketCoin
import one.mixin.android.vo.safe.TokenItem

@Dao
@SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
interface MarketCoinDao : BaseDao<MarketCoin> {
    @Query("SELECT * FROM market_coins ORDER BY asset_id")
    fun observeAll(): Flow<List<MarketCoin>>

    @Query("SELECT * FROM market_coins WHERE asset_id IN (:assetIds)")
    suspend fun findByAssetIds(assetIds: List<String>): List<MarketCoin>

    @Query("""SELECT t.asset_id FROM tokens t
        WHERE (t.collection_hash IS NULL OR t.collection_hash = '')
        AND NOT EXISTS (SELECT 1 FROM market_coins mc WHERE mc.asset_id = t.asset_id AND mc.coin_id != '')""")
    suspend fun findAssetsWithoutCoin(): List<String>

    @Query("$PREFIX_ASSET_ITEM LEFT JOIN market_coins mc on mc.asset_id = a1.asset_id WHERE mc.coin_id = :coinId")
    suspend fun findTokensByCoinId(coinId: String): List<TokenItem>

    @Query("SELECT asset_id FROM market_coins WHERE coin_id = :coinId")
    suspend fun findTokenIdsByCoinId(coinId: String): List<String>

    @Query("DELETE FROM market_coins WHERE coin_id IN (:coinIds)")
    suspend fun deleteByCoinIds(coinIds: List<String>)

    @Query("DELETE FROM market_coins WHERE coin_id = :coinId AND asset_id IN (:assetIds)")
    suspend fun deleteByCoinIdAndAssetIds(coinId: String, assetIds: List<String>)
}
