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

    @Query("SELECT w.transaction_id, w.transaction_type ,w.transaction_hash, w.block_number, w.sender, w.receiver, w.output_hash, w.chain_id, w.asset_id, w.amount, w.transaction_at, w.updated_at, t.symbol, t.icon_url, w.status FROM transactions w LEFT JOIN tokens t on t.asset_id = w.asset_id WHERE t.asset_id = :assetId ORDER BY w.transaction_at DESC LIMIT 21")
    fun web3Transactions(assetId: String): LiveData<List<Web3TransactionItem>>

    @RawQuery(observedEntities = [Web3Transaction::class])
    fun allTransactions(query: SupportSQLiteQuery): DataSource.Factory<Int, Web3TransactionItem>
    
    @Query("SELECT * FROM transactions WHERE sender = :address OR receiver = :address ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatestTransaction(address: String): Web3Transaction?
    
    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

    @Query("DELETE FROM transactions WHERE status = 'pending' AND transaction_hash = :hash AND chain_id = :chainId")
    fun deletePending(hash : String, chainId: String)

    @Query("UPDATE transactions SET status = :type WHERE transaction_hash = :hash")
    fun updateRawTransaction(type: String, hash: String)
}
