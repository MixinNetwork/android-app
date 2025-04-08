package one.mixin.android.db.web3

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import one.mixin.android.db.BaseDao
import one.mixin.android.db.web3.vo.Web3Transaction
import one.mixin.android.db.web3.vo.Web3TransactionItem

@Dao
interface Web3TransactionDao : BaseDao<Web3Transaction> {

    @Query("""
        SELECT w.transaction_hash, w.transaction_type, w.status, w.block_number, w.chain_id, w.address, w.fee, w.senders, w.receivers, w.approvals, w.send_asset_id, w.receive_asset_id, w.transaction_at, w.updated_at, 
            c.symbol as chain_symbol,
            c.icon_url as chain_icon_url,
            s.icon_url as send_asset_icon_url,
            s.symbol as send_asset_symbol,
            r.icon_url as receive_asset_icon_url,
            r.symbol as receive_asset_symbol
        FROM transactions w 
        LEFT JOIN tokens c ON c.asset_id = w.chain_id
        LEFT JOIN tokens s ON s.asset_id = w.send_asset_id
        LEFT JOIN tokens r ON r.asset_id = w.receive_asset_id
        WHERE w.send_asset_id = :assetId OR w.receive_asset_id = :assetId
        ORDER BY w.transaction_at DESC 
        LIMIT 21
    """)
    fun web3Transactions(assetId: String): LiveData<List<Web3TransactionItem>>

    @RawQuery(observedEntities = [Web3Transaction::class])
    fun allTransactions(query: SupportSQLiteQuery): DataSource.Factory<Int, Web3TransactionItem>

    @Query("SELECT * FROM transactions WHERE transaction_hash = :hash AND chain_id = :chainId LIMIT 1")
    suspend fun getLatestTransaction(hash: String, chainId: String): Web3Transaction?

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

    @Query("DELETE FROM transactions WHERE status = 'pending' AND transaction_hash = :hash AND chain_id = :chainId")
    fun deletePending(hash: String, chainId: String)

    @Query("UPDATE transactions SET status = :type WHERE transaction_hash = :hash")
    fun updateRawTransaction(type: String, hash: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
