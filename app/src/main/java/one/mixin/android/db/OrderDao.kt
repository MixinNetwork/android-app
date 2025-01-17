package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import one.mixin.android.ui.home.web3.components.InscriptionState
import one.mixin.android.vo.UtxoItem
import one.mixin.android.vo.route.SwapOrder
import one.mixin.android.vo.route.SwapOrderItem
import one.mixin.android.vo.safe.Output
import one.mixin.android.vo.safe.SafeCollectible
import one.mixin.android.vo.safe.SafeCollection
import timber.log.Timber

@Dao
interface OrderDao : BaseDao<SwapOrder> {

    @Query("""
        SELECT o.*, t.icon_url as asset_icon_url, rt.icon_url as receive_asset_icon_url, rt.symbol as receive_asset_symbol, t.symbol as asset_symbol FROM swap_orders o
        LEFT JOIN tokens t ON o.pay_asset_id = t.asset_id
        LEFT JOIN tokens rt ON o.receive_asset_id = rt.asset_id
        ORDER BY o.created_at DESC
    """)
    suspend fun orders(): List<SwapOrderItem>

    @Query("""
        SELECT o.*, t.icon_url as asset_icon_url, rt.icon_url as receive_asset_icon_url, rt.symbol as receive_asset_symbol, t.symbol as asset_symbol,
        rt.chain_id as receive_chain_id, t.chain_id as pay_chain_id 
        FROM swap_orders o
        LEFT JOIN tokens t ON o.pay_asset_id = t.asset_id
        LEFT JOIN tokens rt ON o.receive_asset_id = rt.asset_id
        WHERE o.order_id = :orderId
    """)
    suspend fun getOrderById(orderId: String): SwapOrderItem?

    @Query("""
        SELECT created_at FROM swap_orders ORDER BY created_at DESC LIMIT 1
    """)
    suspend fun lastOrderCreatedAt(): String?
}