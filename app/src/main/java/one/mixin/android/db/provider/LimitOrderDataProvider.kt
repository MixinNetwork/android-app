package one.mixin.android.db.provider

import android.database.Cursor
import androidx.paging.PagingSource
import one.mixin.android.db.WalletDatabase
import one.mixin.android.ui.wallet.OrderFilterParams
import one.mixin.android.vo.route.OrderItem

class LimitOrderDataProvider {
    companion object {
        fun allOrders(
            database: WalletDatabase,
            filter: OrderFilterParams,
        ): PagingSource<Int, OrderItem> {
            val parts = filter.buildQueryParts()
            return LimitOrderDataProviderGenerated.allOrders(
                database,
                parts.whereOrderSql,
                parts.whereClauseSql,
                parts.orderBySql,
            )
        }
    }
}

fun convertToOrderItems(cursor: Cursor): List<OrderItem> {
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
