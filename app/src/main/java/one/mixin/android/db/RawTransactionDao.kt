package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.utxo.RawTransaction

@Dao
interface RawTransactionDao : BaseDao<RawTransaction> {
    @Query("DELETE FROM raw_transactions WHERE transaction_hash = :hash")
    fun deleteByHash(hash: String)

    @Query("SELECT * FROM raw_transactions")
    fun findTransactions(): List<RawTransaction>
}
