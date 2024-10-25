package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import one.mixin.android.ui.wallet.alert.vo.Alert
import one.mixin.android.ui.wallet.alert.vo.AlertGroup

@Dao
interface AlertDao : BaseDao<Alert> {

    @Query("""
        SELECT a.coin_id, m.icon_url, m.name, m.current_price 
        FROM market_alerts a 
        LEFT JOIN markets m ON m.coin_id = a.coin_id
        GROUP BY a.coin_id 
        ORDER BY a.created_at ASC
    """)
    fun alertGroups(): Flow<List<AlertGroup>>

    @Query("""
        SELECT a.coin_id, m.icon_url, m.name, m.current_price 
        FROM market_alerts a 
        LEFT JOIN markets m ON m.coin_id = a.coin_id
        WHERE a.coin_id in (:coinIds) 
        GROUP BY a.coin_id 
        ORDER BY a.created_at ASC
    """)
    fun alertGroups(coinIds: List<String>): Flow<List<AlertGroup>>

    @Query("""
        SELECT a.coin_id, m.icon_url, m.name, m.current_price 
        FROM market_alerts a 
        LEFT JOIN markets m ON m.coin_id = a.coin_id
        WHERE a.coin_id = :coinId 
    """)
    fun alertGroup(coinId: String): Flow<AlertGroup?>

    @Query("SELECT * FROM market_alerts WHERE coin_id = :coinId ORDER BY created_at ASC")
    fun alertsByCoinId(coinId:String):Flow<List<Alert>>

    @Query("DELETE FROM market_alerts WHERE alert_id = :alertId")
    fun deleteAlertById(alertId: String)

    @Query("SELECT COUNT(*) FROM market_alerts")
    fun getTotalAlertCount(): Int
      
    @Query("SELECT COUNT(*) FROM market_alerts WHERE coin_id = :coinId")
    fun getAlertCountByCoinId(coinId: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM market_alerts WHERE coin_id = :coinId)")
    fun anyAlertByCoinId(coinId: String): LiveData<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM market_alerts ma LEFT JOIN market_coins mc ON mc.coin_id = ma.coin_id WHERE mc.asset_id = :assetId)")
    fun anyAlertByAssetId(assetId: String): LiveData<Boolean>

    @Query("DELETE FROM market_alerts")
    fun deleteAll()
}