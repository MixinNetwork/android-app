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
        SELECT DISTINCT w.transaction_hash, w.transaction_type, w.status, w.block_number, w.chain_id, w.address, w.fee, w.senders, w.receivers, w.approvals, w.send_asset_id, w.receive_asset_id, w.transaction_at, w.updated_at, w.level, 
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
        WHERE (w.send_asset_id = :assetId OR w.receive_asset_id = :assetId) AND (s.wallet_id = :walletId OR c.wallet_id = :walletId) AND w.level >= (SELECT level FROM tokens WHERE asset_id = :assetId)
        AND w.address in (SELECT destination FROM addresses WHERE wallet_id = :walletId)
        ORDER BY w.transaction_at DESC 
        LIMIT 21
    """)
    fun web3Transactions(walletId: String, assetId: String): LiveData<List<Web3TransactionItem>>

    @RawQuery(observedEntities = [Web3Transaction::class])
    fun allTransactions(query: SupportSQLiteQuery): DataSource.Factory<Int, Web3TransactionItem>

    @Query("SELECT DISTINCT transaction_hash, * FROM transactions WHERE transaction_hash = :hash AND chain_id = :chainId LIMIT 1")
    suspend fun getLatestTransaction(hash: String, chainId: String): Web3Transaction?

    @Query("DELETE FROM transactions WHERE status = 'pending' AND transaction_hash = :hash AND chain_id = :chainId")
    fun deletePending(hash: String, chainId: String)

    @Query("UPDATE transactions SET status = :status WHERE transaction_hash = :hash AND chain_id = :chainId")
    fun updateTransaction(hash: String, status: String, chainId: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT COUNT(*) FROM transactions WHERE status = 'pending' AND address in (SELECT destination FROM addresses WHERE wallet_id = :walletId)")
    fun getPendingTransactionCount(walletId: String): LiveData<Int>

    @Query("SELECT DISTINCT transaction_hash, * FROM transactions WHERE status = 'pending' AND address in (SELECT destination FROM addresses WHERE wallet_id = :walletId)")
    suspend fun getPendingTransactions(walletId: String): List<Web3Transaction>

    @Query("DELETE FROM transactions WHERE address IN (SELECT destination FROM addresses WHERE wallet_id = :walletId)")
    suspend fun deleteByWalletId(walletId: String)
}
