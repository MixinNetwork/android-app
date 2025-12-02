package one.mixin.android.db.provider

import android.annotation.SuppressLint
import android.database.Cursor
import android.util.Log
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
                    """.trimIndent()

                    val query = filter.buildQuery()

                    val whereOrderSql = query.sql.substringAfter("FROM orders o")
                    val whereBody = whereOrderSql.substringAfter("WHERE ", missingDelimiterValue = "").substringBefore("ORDER BY", missingDelimiterValue = "").trim()
                    val orderByBody = whereOrderSql.substringAfter("ORDER BY", missingDelimiterValue = "").trim()

                    val countSql = "SELECT COUNT(DISTINCT o.rowid) FROM orders o $whereOrderSql"
                    val countStmt = RoomSQLiteQuery.acquire(countSql, 0)

                    val offsetSql = """
                        SELECT DISTINCT o.rowid FROM orders o
                        $whereOrderSql
                        LIMIT ? OFFSET ?
                    """.trimIndent()
                    val offsetStmt = RoomSQLiteQuery.acquire(offsetSql, 2)

                    val sqlGenerator = fun(ids: String): RoomSQLiteQuery {
                        val querySql = StringBuilder()
                            .append(baseSelect)
                            .append('\n')
                            .append("WHERE o.rowid IN ($ids)")
                            .apply {
                                if (whereBody.isNotEmpty()) {
                                    append('\n').append("AND ").append(whereBody)
                                }
                                append('\n').append("ORDER BY ")
                                if (orderByBody.isNotEmpty()) {
                                    append(orderByBody)
                                } else {
                                    append("o.created_at DESC")
                                }
                            }
                            .toString()
                        Log.e("LimitOrderDataProvider", querySql)
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
                            val idxWalletId = cursor.getColumnIndexOrThrow("wallet_id")
                            val idxUserId = cursor.getColumnIndexOrThrow("user_id")
                            val idxPayAssetId = cursor.getColumnIndexOrThrow("pay_asset_id")
                            val idxAssetIconUrl = cursor.getColumnIndexOrThrow("asset_icon_url")
                            val idxAssetSymbol = cursor.getColumnIndexOrThrow("asset_symbol")
                            val idxReceiveAssetId = cursor.getColumnIndexOrThrow("receive_asset_id")
                            val idxReceiveAssetIconUrl = cursor.getColumnIndexOrThrow("receive_asset_icon_url")
                            val idxReceiveAssetSymbol = cursor.getColumnIndexOrThrow("receive_asset_symbol")
                            val idxPayAmount = cursor.getColumnIndexOrThrow("pay_amount")
                            val idxReceiveAmount = cursor.getColumnIndexOrThrow("receive_amount")
                            val idxState = cursor.getColumnIndexOrThrow("state")
                            val idxOrderType = cursor.getColumnIndexOrThrow("order_type")
                            val idxPayChainName = cursor.getColumnIndexOrThrow("pay_chain_name")
                            val idxReceiveChainName = cursor.getColumnIndexOrThrow("receive_chain_name")
                            val idxCreatedAt = cursor.getColumnIndexOrThrow("created_at")
                            val idxExpectedReceiveAmount = cursor.getColumnIndexOrThrow("expected_receive_amount")
                            val idxFilledReceiveAmount = cursor.getColumnIndexOrThrow("filled_receive_amount")
                            val idxPrice = cursor.getColumnIndexOrThrow("price")
                            val idxExpiredAt = cursor.getColumnIndexOrThrow("expired_at")
                            val idxPendingAmount = cursor.getColumnIndexOrThrow("pending_amount")
                            val idxReceiveChainId = cursor.getColumnIndexOrThrow("receive_chain_id")
                            val idxPayChainId = cursor.getColumnIndexOrThrow("pay_chain_id")

                            while (cursor.moveToNext()) {
                                val item = OrderItem(
                                    orderId = cursor.getString(idxOrderId),
                                    walletId = cursor.getString(idxWalletId),
                                    userId = cursor.getString(idxUserId),
                                    payAssetId = cursor.getString(idxPayAssetId),
                                    payChainId = cursor.getString(idxPayChainId),
                                    assetIconUrl = cursor.getString(idxAssetIconUrl),
                                    assetSymbol = cursor.getString(idxAssetSymbol),
                                    receiveAssetId = cursor.getString(idxReceiveAssetId),
                                    receiveChainId = cursor.getString(idxReceiveChainId),
                                    receiveAssetIconUrl = cursor.getString(idxReceiveAssetIconUrl),
                                    receiveAssetSymbol = cursor.getString(idxReceiveAssetSymbol),
                                    payAmount = cursor.getString(idxPayAmount),
                                    receiveAmount = cursor.getString(idxReceiveAmount),
                                    state = cursor.getString(idxState),
                                    type = cursor.getString(idxOrderType),
                                    payChainName = cursor.getString(idxPayChainName),
                                    receiveChainName = cursor.getString(idxReceiveChainName),
                                    createdAt = cursor.getString(idxCreatedAt),
                                    expectedReceiveAmount = cursor.getString(idxExpectedReceiveAmount),
                                    filledReceiveAmount = cursor.getString(idxFilledReceiveAmount),
                                    price = cursor.getString(idxPrice),
                                    expiredAt = cursor.getString(idxExpiredAt),
                                    pendingAmount = cursor.getString(idxPendingAmount),
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
