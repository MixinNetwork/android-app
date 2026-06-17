package one.mixin.android.db.provider

import android.annotation.SuppressLint
import android.database.Cursor
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.InvalidationTracker
import androidx.room.RoomSQLiteQuery
import androidx.room.paging.util.INITIAL_ITEM_COUNT
import androidx.room.paging.util.INVALID
import androidx.room.paging.util.getClippedRefreshKey
import androidx.room.paging.util.getLimit
import androidx.room.paging.util.getOffset
import androidx.room.withTransaction
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import one.mixin.android.db.WalletDatabase
import one.mixin.android.ui.wallet.OrderFilterParams
import one.mixin.android.vo.route.OrderItem
import java.util.concurrent.atomic.AtomicInteger

@SuppressLint("RestrictedApi")
class LimitOrderDataProvider {
    companion object {
        fun allOrders(
            database: WalletDatabase,
            filter: OrderFilterParams,
        ): PagingSource<Int, OrderItem> {
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

            val offsetSql = """
                SELECT DISTINCT o.rowid FROM orders o
                $whereOrderSql
                LIMIT ? OFFSET ?
            """.trimIndent()

            val querySqlGenerator = fun(ids: String): RoomSQLiteQuery {
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
                return RoomSQLiteQuery.acquire(querySql, 0)
            }

            return object : PagingSource<Int, OrderItem>() {
                private val itemCount = AtomicInteger(INITIAL_ITEM_COUNT)

                private val observer = object : InvalidationTracker.Observer(arrayOf("orders")) {
                    override fun onInvalidated(tables: Set<String>) {
                        invalidate()
                    }
                }

                init {
                    database.invalidationTracker.addWeakObserver(observer)
                }

                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, OrderItem> {
                    return withContext(database.queryExecutor.asCoroutineDispatcher()) {
                        val tempCount = itemCount.get()
                        if (tempCount == INITIAL_ITEM_COUNT) {
                            database.withTransaction {
                                val count = countItems()
                                itemCount.set(count)
                                queryData(params, count)
                            }
                        } else {
                            val loadResult = queryData(params, tempCount)
                            database.invalidationTracker.refreshVersionsSync()
                            @Suppress("UNCHECKED_CAST")
                            if (invalid) INVALID as LoadResult.Invalid<Int, OrderItem> else loadResult
                        }
                    }
                }

                private fun countItems(): Int {
                    val countStmt = RoomSQLiteQuery.acquire(countSql, 0)
                    val cursor = database.query(countStmt)
                    return try {
                        if (cursor.moveToFirst()) cursor.getInt(0) else 0
                    } finally {
                        cursor.close()
                        countStmt.release()
                    }
                }

                private fun queryData(params: LoadParams<Int>, itemCount: Int): LoadResult.Page<Int, OrderItem> {
                    val key = params.key ?: 0
                    val limit = getLimit(params, key)
                    val offset = getOffset(params, key, itemCount)

                    val offsetStmt = RoomSQLiteQuery.acquire(offsetSql, 2)
                    offsetStmt.bindLong(1, limit.toLong())
                    offsetStmt.bindLong(2, offset.toLong())
                    val offsetCursor = database.query(offsetStmt)
                    val ids = mutableListOf<String>()
                    try {
                        while (offsetCursor.moveToNext()) {
                            ids.add("'${offsetCursor.getLong(0)}'")
                        }
                    } finally {
                        offsetCursor.close()
                        offsetStmt.release()
                    }

                    val data = if (ids.isEmpty()) {
                        emptyList()
                    } else {
                        val sqLiteQuery = querySqlGenerator(ids.joinToString())
                        val cursor = database.query(sqLiteQuery)
                        try {
                            convertRows(cursor)
                        } finally {
                            cursor.close()
                            sqLiteQuery.release()
                        }
                    }

                    val nextPosToLoad = offset + data.size
                    val nextKey = if (ids.isEmpty() || ids.size < limit || nextPosToLoad >= itemCount) null else nextPosToLoad
                    val prevKey = if (offset <= 0 || ids.isEmpty()) null else offset
                    return LoadResult.Page(
                        data = data,
                        prevKey = prevKey,
                        nextKey = nextKey,
                        itemsBefore = offset,
                        itemsAfter = maxOf(0, itemCount - nextPosToLoad),
                    )
                }

                private fun convertRows(cursor: Cursor): List<OrderItem> {
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
                        list.add(
                            OrderItem(
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
                        )
                    }
                    return list
                }

                override fun getRefreshKey(state: PagingState<Int, OrderItem>): Int? {
                    return state.getClippedRefreshKey()
                }

                override val jumpingSupported: Boolean
                    get() = true
            }
        }
    }
}
