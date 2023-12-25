package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.safe.DepositEntry

@Dao
interface DepositDao : BaseDao<DepositEntry> {
    @Query("SELECT * FROM deposit_entries WHERE chain_id = :chainId AND is_primary = 1 ORDER BY rowid DESC LIMIT 1")
    suspend fun findDepositEntry(chainId: String): DepositEntry?

    @Query("SELECT destination FROM deposit_entries")
    suspend fun findDepositEntryDestinations(): List<String>

    @Query("DELETE FROM deposit_entries WHERE chain_id=:chainId")
    fun deleteByChainId(chainId: String)
}
