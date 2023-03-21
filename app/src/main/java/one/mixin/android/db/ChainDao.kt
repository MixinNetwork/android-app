package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.Chain

@Dao
interface ChainDao : BaseDao<Chain> {
    @Query("SELECT chain_id FROM chains WHERE chain_id = :id")
    suspend fun checkExistsById(id: String): String?

    @Query("SELECT chain_id FROM chains WHERE chain_id = :chainId AND name = :name AND symbol = :symbol AND icon_url = :iconUrl AND threshold = :threshold")
    suspend fun existSame(chainId: String, name: String, symbol: String, iconUrl:String, threshold: Int):String?

    @Query("SELECT * FROM chains")
    suspend fun getAllChains(): List<Chain>
}
