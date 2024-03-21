package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.Address

@Dao
interface AddressDao : BaseDao<Address> {
    @Query("SELECT * FROM addresses WHERE asset_id = :id ORDER BY updated_at DESC")
    fun addresses(id: String): LiveData<List<Address>>

    @Query("DELETE FROM addresses WHERE address_id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM addresses")
    fun deleteAll()

    @Query("SELECT * FROM addresses WHERE address_id = :id")
    fun observeById(id: String): LiveData<Address>

    @Query("SELECT * FROM addresses WHERE address_id = :addressId AND asset_id = :assetId")
    suspend fun findAddressById(
        addressId: String,
        assetId: String,
    ): Address?

    @Query("SELECT label FROM addresses WHERE destination = :destination AND tag = :tag")
    fun findAddressByReceiver(
        destination: String,
        tag: String,
    ): String?
}
