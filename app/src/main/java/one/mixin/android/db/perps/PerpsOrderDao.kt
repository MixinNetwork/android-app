package one.mixin.android.db.perps

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import one.mixin.android.api.response.perps.PerpsOrder
import one.mixin.android.api.response.perps.PerpsOrderItem
import one.mixin.android.db.BaseDao

@Dao
interface PerpsOrderDao : BaseDao<PerpsOrder> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: PerpsOrder)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<PerpsOrder>)

    @Query("""
        SELECT o.*, m.display_symbol, m.icon_url, m.token_symbol
        FROM perps_orders o
        LEFT JOIN markets m ON m.market_id = o.market_id
        WHERE o.order_type IN ('open', 'increase_position', 'close')
        AND (:offset IS NULL OR o.created_at < :offset)
        ORDER BY o.created_at DESC
        LIMIT :limit
    """)
    suspend fun getOrders(limit: Int, offset: String? = null): List<PerpsOrderItem>

    @Query("""
        SELECT o.*, m.display_symbol, m.icon_url, m.token_symbol
        FROM perps_orders o
        LEFT JOIN markets m ON m.market_id = o.market_id
        WHERE o.order_type IN ('open', 'increase_position', 'close')
        ORDER BY o.created_at DESC
        LIMIT :limit
    """)
    fun observeOrders(limit: Int): Flow<List<PerpsOrderItem>>

    @Query("""
        SELECT o.*, m.display_symbol, m.icon_url, m.token_symbol
        FROM perps_orders o
        LEFT JOIN markets m ON m.market_id = o.market_id
        WHERE o.order_type IN ('open', 'increase_position', 'close')
        ORDER BY o.created_at DESC
    """)
    fun getOrdersPaged(): PagingSource<Int, PerpsOrderItem>

    @Query("""
        SELECT o.*, m.display_symbol, m.icon_url, m.token_symbol
        FROM perps_orders o
        LEFT JOIN markets m ON m.market_id = o.market_id
        WHERE o.order_type IN ('open', 'increase_position', 'close')
        AND o.market_id = :marketId
        ORDER BY o.created_at DESC
        LIMIT :limit
    """)
    suspend fun getOrdersByMarket(marketId: String, limit: Int = 100): List<PerpsOrderItem>

    @Query("""
        SELECT o.*, m.display_symbol, m.icon_url, m.token_symbol
        FROM perps_orders o
        LEFT JOIN markets m ON m.market_id = o.market_id
        WHERE o.order_id = :orderId
    """)
    suspend fun getOrder(orderId: String): PerpsOrderItem?

    @Query("DELETE FROM perps_orders")
    suspend fun deleteAll()

    @Query("SELECT MAX(updated_at) FROM perps_orders")
    suspend fun getLatestUpdatedAt(): String?

    @Query("SELECT SUM(CAST(realized_pnl AS REAL)) FROM perps_orders WHERE order_type = 'close'")
    suspend fun getTotalRealizedPnl(): Double?

    @Query("SELECT COALESCE(SUM(CAST(realized_pnl AS REAL)), 0) FROM perps_orders WHERE order_type = 'close'")
    fun observeTotalRealizedPnl(): Flow<Double>

    @Query("SELECT SUM(CAST(entry_price AS REAL) * ABS(CAST(quantity AS REAL))) FROM perps_orders WHERE order_type = 'close'")
    suspend fun getTotalClosedEntryValue(): Double?

    @Query("SELECT COALESCE(SUM(CAST(entry_price AS REAL) * ABS(CAST(quantity AS REAL))), 0) FROM perps_orders WHERE order_type = 'close'")
    fun observeTotalClosedEntryValue(): Flow<Double>
}
