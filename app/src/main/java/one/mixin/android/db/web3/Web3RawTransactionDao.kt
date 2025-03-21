package one.mixin.android.db.web3

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.db.BaseDao
import one.mixin.android.db.web3.vo.Web3RawTransaction

@Dao
interface Web3RawTransactionDao : BaseDao<Web3RawTransaction> {
    
    @Query("SELECT * FROM raw_transactions WHERE state = 'pending'")
    suspend fun getPendingTransactions(): List<Web3RawTransaction>

    @Query("SELECT * FROM raw_transactions WHERE state = 'pending' AND chain_id = :chainId")
    suspend fun getPendingTransactions(chainId: String): List<Web3RawTransaction>

}
