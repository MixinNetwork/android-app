package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.safe.RawTransaction

@Dao
interface RawTransactionDao : BaseDao<RawTransaction> {

    @Query("SELECT * FROM raw_transactions WHERE state = 'unspent' ORDER BY created_at ASC, rowid ASC LIMIT 1")
    fun findUnspentTransaction(): RawTransaction?

    @Query("SELECT * FROM raw_transactions WHERE request_id = :requestId")
    fun findTransactionById(requestId: String): RawTransaction?

    @Query("SELECT * FROM raw_transactions WHERE request_id = :requestId")
    fun findRawTransaction(requestId: String): RawTransaction?

    @Query("UPDATE raw_transactions SET state = :state WHERE request_id = :requestId")
    fun updateRawTransaction(requestId: String, state: String)
}
