package one.mixin.android.db.web3

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.db.BaseDao
import one.mixin.android.db.web3.vo.Web3RawTransaction

@Dao
interface Web3RawTransactionDao : BaseDao<Web3RawTransaction> {
    
    @Query("SELECT * FROM raw_transactions WHERE state = 'pending'")
    suspend fun getPendingRawTransactions(): List<Web3RawTransaction>

    @Query("SELECT * FROM raw_transactions WHERE state = 'pending' AND chain_id = :chainId")
    suspend fun getPendingRawTransactions(chainId: String): List<Web3RawTransaction>

    @Query("SELECT nonce FROM raw_transactions WHERE chain_id = :chainId AND state = 'pending' ORDER BY nonce DESC LIMIT 1")
    suspend fun getNonce(chainId: String): String?
}
