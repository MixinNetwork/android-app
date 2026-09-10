package one.mixin.android.db.web3

import androidx.room3.Dao
import androidx.room3.Query
import one.mixin.android.db.BaseDao
import one.mixin.android.db.web3.vo.Web3Chain

@Dao
interface Web3ChainDao : BaseDao<Web3Chain> {
    @Query("SELECT chain_id FROM chains WHERE chain_id = :chainId LIMIT 1")
    suspend fun chainExists(chainId: String): String?

    @Query("SELECT * FROM chains WHERE chain_id = :chainId")
    suspend fun findChainById(chainId: String): Web3Chain?
}