package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow
import one.mixin.android.vo.Address
import one.mixin.android.vo.AddressItem

@Dao
interface AddressDao : BaseDao<Address> {
    @Query("SELECT * FROM addresses WHERE chain_id = :id ORDER BY last_used_at IS NULL, last_used_at DESC, updated_at DESC")
    fun addresses(id: String): LiveData<List<Address>>

    @Query("SELECT * FROM addresses WHERE chain_id = :id ORDER BY last_used_at IS NULL, last_used_at DESC, updated_at DESC")
    fun addressesFlow(id: String): Flow<List<Address>>

    @Query("SELECT * FROM addresses ORDER BY last_used_at IS NULL, last_used_at DESC, updated_at DESC")
    fun allAddressesFlow(): Flow<List<Address>>

    @Query("SELECT a.address_id, a.chain_id, COALESCE(t.icon_url, ch.icon_url) AS icon_url, COALESCE(c.icon_url, ch.icon_url) AS chain_icon_url, a.label, a.destination, a.tag FROM addresses a LEFT JOIN tokens t ON t.asset_id = a.asset_id LEFT JOIN tokens c ON c.asset_id = t.chain_id LEFT JOIN chains ch ON ch.chain_id = a.chain_id ORDER BY updated_at DESC")
    fun allAddresses(): LiveData<List<AddressItem>>

    @Query("SELECT * FROM addresses WHERE chain_id = :chainId ORDER BY updated_at DESC")
    fun getAddressByChainId(chainId: String): Address?

    @Query("DELETE FROM addresses WHERE address_id = :addressId")
    suspend fun deleteById(addressId: String)

    @Query("DELETE FROM addresses")
    fun deleteAll()

    @Query("SELECT * FROM addresses WHERE address_id = :addressId")
    fun observeById(addressId: String): LiveData<Address?>

    @Query("SELECT * FROM addresses WHERE address_id = :addressId AND chain_id = :chainId")
    suspend fun findAddressById(
        addressId: String,
        chainId: String,
    ): Address?

    @Query("SELECT label FROM addresses WHERE destination = :destination AND tag = :tag")
    fun findAddressByDestination(
        destination: String,
        tag: String,
    ): String?

    @Query("SELECT label FROM addresses WHERE destination = :destination AND tag = :tag AND chain_id = :chainId")
    fun findAddressByDestination(
        destination: String,
        tag: String,
        chainId: String
    ): String?

    @Query("SELECT * FROM addresses WHERE destination = :destination AND chain_id = :chainId")
    suspend fun matchAddress(destination: String, chainId: String): Address?

    @Query("SELECT * FROM addresses")
    suspend fun findAll(): List<Address>

    @Query("SELECT * FROM addresses WHERE address_id IN (:addressIds)")
    suspend fun findByIds(addressIds: List<String>): List<Address>

    // Server payloads never carry the local last used time, so keep it across REPLACE writes.
    @Transaction
    suspend fun insertKeepingLastUsed(addresses: List<Address>) {
        if (addresses.isEmpty()) return
        val lastUsed = findByIds(addresses.map { it.addressId }).associate { it.addressId to it.lastUsedAt }
        insertListSuspend(addresses.map { it.copy(lastUsedAt = lastUsed[it.addressId] ?: it.lastUsedAt) })
    }

    @Query("UPDATE addresses SET last_used_at = :usedAt WHERE address_id = :addressId AND (last_used_at IS NULL OR last_used_at < :usedAt)")
    suspend fun updateLastUsedAt(addressId: String, usedAt: Long)

    @Query(
        "UPDATE addresses SET last_used_at = :usedAt WHERE chain_id = :chainId AND COALESCE(tag, '') = :tag " +
            "AND (destination = :destination OR (:ignoreCase AND destination = :destination COLLATE NOCASE)) " +
            "AND (last_used_at IS NULL OR last_used_at < :usedAt)",
    )
    suspend fun markUsed(chainId: String, destination: String, tag: String, ignoreCase: Boolean, usedAt: Long)
}
