package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import one.mixin.android.ui.wallet.alert.vo.Alert
import one.mixin.android.ui.wallet.alert.vo.AlertGroup

@Dao
interface AlertDao : BaseDao<Alert> {

    @Query("SELECT a.asset_id, t.icon_url, t.name, t.price_usd FROM alerts a LEFT JOIN tokens t on t.asset_id = a.asset_id GROUP BY a.asset_id")
    fun alertGroups(): Flow<List<AlertGroup>>

    @Query("SELECT a.asset_id, t.icon_url, t.name, t.price_usd FROM alerts a LEFT JOIN tokens t on t.asset_id = a.asset_id WHERE a.asset_id IN (:assetIds) GROUP BY a.asset_id")
    fun alertGroups(assetIds: List<String>): Flow<List<AlertGroup>>

    @Query("SELECT * FROM alerts WHERE asset_id = :assetId")
    fun alertsByAssetId(assetId:String):Flow<List<Alert>>
}