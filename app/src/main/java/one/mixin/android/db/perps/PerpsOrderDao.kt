package one.mixin.android.db.perps

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
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
        SELECT o.*, m.display_symbol, m.icon_url, m.token_symbol, m.price_scale
        FROM perps_orders o
        LEFT JOIN markets m ON m.market_id = o.market_id
        WHERE o.order_type IN ('open', 'increase_position', 'close')
        AND o.status != 'processing'
        AND (:offset IS NULL OR o.created_at < :offset)
        ORDER BY o.created_at DESC
        LIMIT :limit
    """)
    suspend fun getOrders(limit: Int, offset: String? = null): List<PerpsOrderItem>

    @Query("""
        SELECT o.*, m.display_symbol, m.icon_url, m.token_symbol, m.price_scale
        FROM perps_orders o
        LEFT JOIN markets m ON m.market_id = o.market_id
        WHERE o.order_type IN ('open', 'increase_position', 'close')
        AND o.status != 'processing'
        ORDER BY o.created_at DESC
        LIMIT :limit
    """)
    fun observeOrders(limit: Int): Flow<List<PerpsOrderItem>>

    @Query("""
        SELECT o.*, m.display_symbol, m.icon_url, m.token_symbol, m.price_scale
        FROM perps_orders o
        LEFT JOIN markets m ON m.market_id = o.market_id
        WHERE o.order_type IN ('open', 'increase_position', 'close')
        AND o.status != 'processing'
        ORDER BY o.created_at DESC
    """)
    fun getOrdersPaged(): PagingSource<Int, PerpsOrderItem>

    @Query("""
        SELECT o.*, m.display_symbol, m.icon_url, m.token_symbol, m.price_scale
        FROM perps_orders o
        LEFT JOIN markets m ON m.market_id = o.market_id
        WHERE o.order_type IN ('open', 'increase_position', 'close')
        AND o.status != 'processing'
        AND o.market_id = :marketId
        ORDER BY o.created_at DESC
        LIMIT :limit
    """)
    suspend fun getOrdersByMarket(marketId: String, limit: Int = 100): List<PerpsOrderItem>

    @Query("""
        SELECT o.*, m.display_symbol, m.icon_url, m.token_symbol, m.price_scale
        FROM perps_orders o
        LEFT JOIN markets m ON m.market_id = o.market_id
        WHERE o.order_id = :orderId
    """)
    suspend fun getOrder(orderId: String): PerpsOrderItem?

    @Query("""
        SELECT o.*, m.display_symbol, m.icon_url, m.token_symbol, m.price_scale
        FROM perps_orders o
        LEFT JOIN markets m ON m.market_id = o.market_id
        WHERE o.position_id = :positionId
        AND o.order_type = 'close'
        ORDER BY o.updated_at DESC
        LIMIT 1
    """)
    suspend fun getCloseOrderByPositionId(positionId: String): PerpsOrderItem?

    @Query("DELETE FROM perps_orders")
    suspend fun deleteAll()

    @Query(
        """
        SELECT leverage
        FROM perps_orders
        WHERE position_id = :positionId AND leverage > 0
        ORDER BY CASE WHEN order_id LIKE 'local_%' THEN 0 ELSE 1 END, updated_at DESC
        LIMIT 1
    """
    )
    suspend fun getCachedLeverage(positionId: String): Int?

    @Query("DELETE FROM perps_orders WHERE order_id LIKE 'local_%' AND position_id IN (:positionIds)")
    suspend fun deleteLocalByPositionIds(positionIds: List<String>)

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
