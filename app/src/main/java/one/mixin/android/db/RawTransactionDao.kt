package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.utxo.RawTransaction

@Dao
interface RawTransactionDao : BaseDao<RawTransaction> {
    @Query("DELETE FROM raw_transaction WHERE transaction_hash = :hash")
    fun deleteByHash(hash: String)
}
