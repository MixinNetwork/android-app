package one.mixin.android.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import one.mixin.android.vo.Address

@Dao
interface AddressDao : BaseDao<Address> {

    @Query("SELECT * FROM addresses WHERE asset_id = :id ORDER BY updated_at DESC")
    fun addresses(id: String): LiveData<List<Address>>

    @Query("DELETE FROM addresses WHERE address_id = :id")
    fun deleteById(id: String)

    @Query("DELETE FROM addresses")
    fun deleteAll()
}