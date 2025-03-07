package one.mixin.android.db.web3

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.db.BaseDao
import one.mixin.android.db.web3.vo.Web3Chain
import one.mixin.android.vo.Chain

@Dao
interface Web3ChainDao : BaseDao<Web3Chain> {
    @Query("SELECT chain_id FROM chains WHERE chain_id = :chainId LIMIT 1")
    suspend fun chainExists(chainId: String): String
}