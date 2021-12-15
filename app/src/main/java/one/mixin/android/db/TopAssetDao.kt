package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import one.mixin.android.vo.TopAsset
import one.mixin.android.vo.TopAssetItem

@Dao
interface TopAssetDao : BaseDao<TopAsset> {

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT ta.asset_id as asset_id, ta.symbol as symbol, ta.name as name, ta.icon_url as icon_url, ta.chain_id as chain_id, a.icon_url as chain_icon_url,
        ta.price_usd as priceUsd, ta.change_usd as changeUsd
        FROM top_assets ta
        INNER JOIN assets a ON a.asset_id = ta.chain_id AND (SELECT ta.asset_id FROM assets a WHERE a.asset_id = ta.asset_id) IS NULL
        ORDER BY ta.rowid
    """
    )
    fun topAssets(): LiveData<List<TopAssetItem>>

    @Query(
        """
        DELETE FROM top_assets WHERE asset_id NOT IN (:ids)
    """
    )
    suspend fun deleteNotInIds(ids: List<String>)
}
