package one.mixin.android.db.provider

import android.annotation.SuppressLint
import android.database.Cursor
import androidx.paging.DataSource
import androidx.room.RoomSQLiteQuery
import one.mixin.android.db.WalletDatabase
import one.mixin.android.db.datasource.MixinLimitOffsetDataSource
import one.mixin.android.ui.wallet.OrderFilterParams
import one.mixin.android.vo.route.OrderItem

@SuppressLint("RestrictedApi")
class LimitOrderDataProvider {
    companion object {
        fun allOrders(
            database: WalletDatabase,
            filter: OrderFilterParams,
        ): DataSource.Factory<Int, OrderItem> {
            return object : DataSource.Factory<Int, OrderItem>() {
                override fun create(): DataSource<Int, OrderItem> {
                    val baseSelect = """
                        SELECT 
                            o.order_id,
                            o.user_id,
                            o.pay_asset_id,
                            ps.icon_url AS asset_icon_url,
                            ps.symbol AS asset_symbol,
                            o.receive_asset_id,
                            rs.icon_url AS receive_asset_icon_url,
                            rs.symbol AS receive_asset_symbol,
                            o.pay_amount,
                            o.receive_amount,
                            o.pay_trace_id,
                            o.receive_trace_id,
                            o.state,
                            o.created_at,
                            o.order_type,
                            o.fund_status,
                            o.price,
                            o.pending_amount,
                            o.filled_receive_amount,
                            o.expected_receive_amount,
                            o.updated_at,
                            o.expired_at
                        FROM orders o
                        LEFT JOIN tokens ps ON ps.asset_id = o.pay_asset_id
                        LEFT JOIN tokens rs ON rs.asset_id = o.receive_asset_id
                    """.trimIndent()

                    val query = filter.buildQuery()

                    val whereOrderSql = query.sql.substringAfter("FROM orders o")

                    val countSql = "SELECT count(1) FROM orders o $whereOrderSql"
                    val countStmt = RoomSQLiteQuery.acquire(countSql, 0)

                    val offsetSql = """
                        SELECT o.rowid FROM orders o
                        $whereOrderSql
                        LIMIT ? OFFSET ?
                    """.trimIndent()
                    val offsetStmt = RoomSQLiteQuery.acquire(offsetSql, 2)

                    val sqlGenerator = fun(ids: String): RoomSQLiteQuery {
                        val querySql = StringBuilder()
                            .append(baseSelect)
                            .append('\n')
                            .append("WHERE o.rowid IN ($ids)")
                            .append('\n')
                            .append(whereOrderSql.substringAfter("WHERE ", missingDelimiterValue = "").let { tail ->
                                if (tail.isNotEmpty() && !tail.startsWith("ORDER BY")) "AND $tail" else tail
                            })
                            .toString()
                        return RoomSQLiteQuery.acquire(querySql, 0)
                    }

                    return object : MixinLimitOffsetDataSource<OrderItem>(
                        database,
                        countStmt,
                        offsetStmt,
                        sqlGenerator,
                        arrayOf("orders"),
                    ) {
                        override fun convertRows(cursor: Cursor?): List<OrderItem> {
                            if (cursor == null) return emptyList()
                            val list = ArrayList<OrderItem>(cursor.count)
                            val idxOrderId = cursor.getColumnIndexOrThrow("order_id")
                            val idxUserId = cursor.getColumnIndexOrThrow("user_id")
                            val idxPayAssetId = cursor.getColumnIndexOrThrow("pay_asset_id")
                            val idxAssetIconUrl = cursor.getColumnIndexOrThrow("asset_icon_url")
                            val idxAssetSymbol = cursor.getColumnIndexOrThrow("asset_symbol")
                            val idxReceiveAssetId = cursor.getColumnIndexOrThrow("receive_asset_id")
                            val idxReceiveAssetIconUrl = cursor.getColumnIndexOrThrow("receive_asset_icon_url")
                            val idxReceiveAssetSymbol = cursor.getColumnIndexOrThrow("receive_asset_symbol")
                            val idxPayAmount = cursor.getColumnIndexOrThrow("pay_amount")
                            val idxReceiveAmount = cursor.getColumnIndexOrThrow("receive_amount")
                            val idxPayTraceId = cursor.getColumnIndexOrThrow("pay_trace_id")
                            val idxReceiveTraceId = cursor.getColumnIndexOrThrow("receive_trace_id")
                            val idxState = cursor.getColumnIndexOrThrow("state")
                            val idxCreatedAt = cursor.getColumnIndexOrThrow("created_at")
                            val idxOrderType = cursor.getColumnIndexOrThrow("order_type")
                            val idxFundStatus = cursor.getColumnIndexOrThrow("fund_status")
                            val idxPrice = cursor.getColumnIndexOrThrow("price")
                            val idxPendingAmount = cursor.getColumnIndexOrThrow("pending_amount")
                            val idxFilledRecvAmount = cursor.getColumnIndexOrThrow("filled_receive_amount")
                            val idxExpectedRecvAmount = cursor.getColumnIndexOrThrow("expected_receive_amount")
                            val idxUpdatedAt = cursor.getColumnIndexOrThrow("updated_at")
                            val idxExpiredAt = cursor.getColumnIndexOrThrow("expired_at")

                            while (cursor.moveToNext()) {
                                val item = OrderItem(
                                    orderId = cursor.getString(idxOrderId),
                                    userId = cursor.getString(idxUserId),
                                    payAssetId = cursor.getString(idxPayAssetId),
                                    assetIconUrl = cursor.getString(idxAssetIconUrl),
                                    assetSymbol = cursor.getString(idxAssetSymbol),
                                    receiveAssetId = cursor.getString(idxReceiveAssetId),
                                    receiveAssetIconUrl = cursor.getString(idxReceiveAssetIconUrl),
                                    receiveAssetSymbol = cursor.getString(idxReceiveAssetSymbol),
                                    payAmount = cursor.getString(idxPayAmount),
                                    receiveAmount = cursor.getString(idxReceiveAmount),
                                    payTraceId = cursor.getString(idxPayTraceId),
                                    receiveTraceId = cursor.getString(idxReceiveTraceId),
                                    state = cursor.getString(idxState),
                                    createdAt = cursor.getString(idxCreatedAt),
                                    orderType = cursor.getString(idxOrderType),
                                    fundStatus = cursor.getString(idxFundStatus),
                                    price = cursor.getString(idxPrice),
                                    pendingAmount = cursor.getString(idxPendingAmount),
                                    filledReceiveAmount = cursor.getString(idxFilledRecvAmount),
                                    expectedReceiveAmount = cursor.getString(idxExpectedRecvAmount),
                                    updatedAt = cursor.getString(idxUpdatedAt),
                                    expiredAt = cursor.getString(idxExpiredAt),
                                )
                                list.add(item)
                            }
                            return list
                        }
                    }
                }
            }
        }
    }
}
