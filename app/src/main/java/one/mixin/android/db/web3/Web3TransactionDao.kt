package one.mixin.android.db.web3

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import one.mixin.android.db.web3.vo.Web3Transaction
import one.mixin.android.db.BaseDao

@Dao
interface Web3TransactionDao : BaseDao<Web3Transaction> {

    @Query("SELECT * FROM web3_transactions")
    fun web3Transactions(): LiveData<List<Web3Transaction>>

    @RawQuery(observedEntities = [Web3Transaction::class])
    fun allTransactions(query: SupportSQLiteQuery): DataSource.Factory<Int, Web3Transaction>
    
    @Query("SELECT * FROM web3_transactions ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatestTransaction(): Web3Transaction?
    
    @Query("SELECT COUNT(*) FROM web3_transactions")
    suspend fun getTransactionCount(): Int
}
