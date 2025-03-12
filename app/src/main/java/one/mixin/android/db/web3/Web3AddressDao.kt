package one.mixin.android.db.web3

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.db.BaseDao

@Dao
interface Web3AddressDao : BaseDao<Web3Address> {

    @Query("SELECT * FROM addresses")
    suspend fun getAddress(): List<Web3Address>
    
    @Query("SELECT * FROM addresses WHERE chain_id = :chainId")
    suspend fun getAddressesByChainId(chainId: String): Web3Address?
}
