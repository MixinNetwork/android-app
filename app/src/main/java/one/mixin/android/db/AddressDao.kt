package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.Address
import one.mixin.android.vo.AddressItem

@Dao
interface AddressDao : BaseDao<Address> {
    @Query("SELECT * FROM addresses WHERE asset_id = :id ORDER BY updated_at DESC")
    fun addresses(id: String): LiveData<List<Address>>

    @Query("SELECT a.address_id, t.icon_url, a.label, a.destination, a.tag FROM addresses a LEFT JOIN tokens t ON t.asset_id = a.asset_id ORDER BY updated_at DESC")
    fun allAddresses(): LiveData<List<AddressItem>>

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
