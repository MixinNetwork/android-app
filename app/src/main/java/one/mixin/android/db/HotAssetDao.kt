package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import one.mixin.android.vo.HotAsset

@Dao
interface HotAssetDao: BaseDao<HotAsset> {

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT * FROM hot_assets ha WHERE (SELECT a.asset_id FROM assets a WHERE a.asset_id == ha.asset_id) IS NULL ORDER BY rowid")
    fun hotAssets(): LiveData<List<HotAsset>>
}