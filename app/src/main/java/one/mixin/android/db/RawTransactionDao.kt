package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.safe.RawTransaction

@Dao
interface RawTransactionDao : BaseDao<RawTransaction> {
    @Query("SELECT * FROM raw_transactions WHERE state = 'unspent' AND (type = 0 OR type = 1) ORDER BY rowid ASC LIMIT 1")
    fun findUnspentTransaction(): RawTransaction?

    @Query("SELECT count(1) FROM raw_transactions WHERE state = 'unspent' AND (type = 0 OR type = 1)")
    suspend fun countUnspentTransaction(): Int

    @Query("SELECT * FROM raw_transactions WHERE request_id = :requestId")
    fun findRawTransaction(requestId: String): RawTransaction?

    @Query("SELECT * FROM raw_transactions WHERE request_id = :requestId AND type = :type")
    fun findRawTransaction(
        requestId: String,
        type: Int,
    ): RawTransaction?

    @Query("UPDATE raw_transactions SET state = :state WHERE request_id = :requestId")
    fun updateRawTransaction(
        requestId: String,
        state: String,
    )

}
