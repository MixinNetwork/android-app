package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.safe.RawTransaction

@Dao
interface RawTransactionDao : BaseDao<RawTransaction> {
    @Query("DELETE FROM raw_transactions WHERE request_id = :id")
    fun deleteById(id: String)

    @Query("SELECT * FROM raw_transactions ORDER BY created_at ASC")
    fun findTransactions(): List<RawTransaction>
}
