package one.mixin.android.db.provider

import androidx.paging.PagingSource
import one.mixin.android.codegen.annotation.GeneratedPagingSourceQuery
import one.mixin.android.codegen.annotation.GeneratedQueryProvider
import one.mixin.android.db.WalletDatabase
import one.mixin.android.vo.route.OrderItem

private const val ORDER_ITEM_SELECT_SQL =
    """
    SELECT DISTINCT
        o.order_id,
        o.wallet_id,
        o.user_id,
        o.pay_asset_id,
        t.icon_url AS asset_icon_url,
        t.symbol AS asset_symbol,
        o.receive_asset_id,
        rt.icon_url AS receive_asset_icon_url,
        rt.symbol AS receive_asset_symbol,
        o.pay_amount,
        o.receive_amount,
        o.state,
        o.order_type,
        pc.name AS pay_chain_name,
        rc.name AS receive_chain_name,
        o.created_at,
        o.expected_receive_amount,
        o.filled_receive_amount,
        o.price,
        o.expired_at,
        o.pending_amount,
        rt.chain_id AS receive_chain_id,
        t.chain_id AS pay_chain_id
    FROM orders o
    LEFT JOIN tokens t ON o.pay_asset_id = t.asset_id
    LEFT JOIN tokens rt ON o.receive_asset_id = rt.asset_id
    LEFT JOIN chains pc ON t.chain_id = pc.chain_id
    LEFT JOIN chains rc ON rt.chain_id = rc.chain_id
    """

private const val ALL_ORDERS_COUNT_SQL =
    "SELECT COUNT(DISTINCT o.rowid) FROM orders o {{whereOrderSql}}"

private const val ALL_ORDERS_OFFSET_SQL =
    """
    SELECT DISTINCT o.rowid FROM orders o
    {{whereOrderSql}}
    LIMIT ? OFFSET ?
    """

private const val ALL_ORDERS_QUERY_SQL =
    ORDER_ITEM_SELECT_SQL +
        """
        WHERE o.rowid IN ({{ids}})
        {{whereClauseSql}}
        ORDER BY {{orderBySql}}
        """

@GeneratedQueryProvider(generatedName = "LimitOrderDataProviderGenerated")
interface LimitOrderDataProviderQuerySpec {
    @GeneratedPagingSourceQuery(
        countSql = ALL_ORDERS_COUNT_SQL,
        offsetSql = ALL_ORDERS_OFFSET_SQL,
        querySql = ALL_ORDERS_QUERY_SQL,
        tables = ["orders"],
        converter = "convertToOrderItems",
    )
    fun allOrders(
        database: WalletDatabase,
        whereOrderSql: String,
        whereClauseSql: String,
        orderBySql: String,
    ): PagingSource<Int, OrderItem>
}
