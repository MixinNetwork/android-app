package one.mixin.android.db.web3

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.api.response.Web3Address
import one.mixin.android.db.BaseDao

@Dao
interface Web3AddressDao : BaseDao<Web3Address> {

    @Query("SELECT destination FROM web3_addresses")
    suspend fun getAddressIds(): List<String>
}
