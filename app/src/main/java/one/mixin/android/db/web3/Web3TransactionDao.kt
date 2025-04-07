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
        SELECT w.transaction_hash, w.transaction_type, w.status, w.block_number, w.chain_id, w.fee, w.senders, w.receivers, w.approvals, w.send_asset_id, w.receive_asset_id, w.transaction_at, w.updated_at, 
            CASE 
                WHEN w.transaction_type = 'transfer_in' THEN r.symbol
                WHEN w.transaction_type = 'transfer_out' THEN s.symbol
                WHEN w.transaction_type = 'swap' THEN r.symbol
                WHEN w.transaction_type = 'approval' THEN a.symbol
                ELSE c.symbol
            END as chain_symbol,
            CASE 
                WHEN w.transaction_type = 'transfer_in' THEN r.icon_url
                WHEN w.transaction_type = 'transfer_out' THEN s.icon_url
                WHEN w.transaction_type = 'swap' THEN r.icon_url
                WHEN w.transaction_type = 'approval' THEN a.icon_url
                ELSE c.icon_url
            END as chain_icon_url,
            COALESCE(
                CASE 
                    WHEN w.transaction_type = 'transfer_in' THEN w.receive_asset_id
                    WHEN w.transaction_type = 'transfer_out' THEN w.send_asset_id
                    WHEN w.transaction_type = 'swap' THEN w.receive_asset_id
                    WHEN w.transaction_type = 'approval' THEN w.chain_id
                    ELSE w.chain_id
                END,
                w.chain_id
            ) as asset_id,
            CASE 
                WHEN w.transaction_type = 'transfer_in' THEN r.icon_url
                WHEN w.transaction_type = 'transfer_out' THEN s.icon_url
                WHEN w.transaction_type = 'swap' THEN r.icon_url
                WHEN w.transaction_type = 'approval' THEN a.icon_url
                ELSE c.icon_url
            END as icon_url,
            CASE 
                WHEN w.transaction_type = 'transfer_in' THEN r.symbol
                WHEN w.transaction_type = 'transfer_out' THEN s.symbol
                WHEN w.transaction_type = 'swap' THEN r.symbol
                WHEN w.transaction_type = 'approval' THEN a.symbol
                ELSE c.symbol
            END as symbol
        FROM transactions w 
        LEFT JOIN tokens c ON c.asset_id = w.chain_id
        LEFT JOIN tokens s ON s.asset_id = w.send_asset_id
        LEFT JOIN tokens r ON r.asset_id = w.receive_asset_id
        LEFT JOIN tokens a ON a.asset_id = w.chain_id
        WHERE w.chain_id = :chainId 
        ORDER BY w.transaction_at DESC 
        LIMIT 21
    """)
    fun web3Transactions(chainId: String): LiveData<List<Web3TransactionItem>>

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
