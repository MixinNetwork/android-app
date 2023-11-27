package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.safe.DepositEntry

@Dao
interface DepositDao : BaseDao<DepositEntry> {

    @Query("DELETE FROM deposit_entries WHERE chain_id=:chainId")
    fun deleteByChainId(chainId: String)
}