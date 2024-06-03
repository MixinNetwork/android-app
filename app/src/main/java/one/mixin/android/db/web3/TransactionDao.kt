package one.mixin.android.db.web3

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.db.BaseDao
import one.mixin.android.vo.web3.Transaction

@Dao
interface TransactionDao : BaseDao<Transaction> {
    @Query("SELECT nonce FROM transactions WHERE chain_id = :chainId ORDER BY nonce DESC LIMIT 1")
    fun lastNonce(chainId: String): Long?
}