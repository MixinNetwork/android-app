package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import one.mixin.android.ui.wallet.alert.vo.Alert
import one.mixin.android.ui.wallet.alert.vo.AlertGroup

@Dao
interface AlertDao : BaseDao<Alert> {

    @Query("""
        SELECT a.coin_id, t.icon_url, t.name, t.price_usd 
        FROM market_alerts a 
        LEFT JOIN market_coins mc ON mc.coin_id = a.coin_id
        LEFT JOIN tokens t ON t.asset_id = mc.asset_id 
        GROUP BY a.coin_id 
        ORDER BY a.created_at ASC
    """)
    fun alertGroups(): Flow<List<AlertGroup>>

    @Query("""
        SELECT a.coin_id, t.icon_url, t.name, t.price_usd 
        FROM market_alerts a 
        LEFT JOIN market_coins mc ON mc.coin_id = a.coin_id
        LEFT JOIN tokens t ON t.asset_id = mc.asset_id
        WHERE a.coin_id in (:coinIds) 
        GROUP BY a.coin_id 
        ORDER BY a.created_at ASC
    """)
    fun alertGroups(coinIds: List<String>): Flow<List<AlertGroup>>

    @Query("SELECT * FROM market_alerts WHERE coin_id = :coinId ORDER BY created_at ASC")
    fun alertsByCoinId(coinId:String):Flow<List<Alert>>

    @Query("UPDATE market_alerts SET status = :status WHERE alert_id = :alertId")
    fun updateStatus(alertId: String, status: String)

    @Query("UPDATE market_alerts SET type=:type, value = :value, frequency =:frequency WHERE alert_id = :alertId")
    fun updateAlert(alertId: String, type: String, value: String, frequency: String)

    @Query("DELETE FROM market_alerts WHERE alert_id = :alertId")
    fun deleteAlertById(alertId: String)

    @Query("SELECT COUNT(*) FROM market_alerts")
    fun getTotalAlertCount(): Int
      
    @Query("SELECT COUNT(*) FROM market_alerts WHERE coin_id = :coinId")
    fun getAlertCountByCoinId(coinId: String): Int

    @Query("DELETE FROM market_alerts")
    fun deleteAll()
}