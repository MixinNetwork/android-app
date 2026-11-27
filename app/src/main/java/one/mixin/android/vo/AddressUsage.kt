package one.mixin.android.vo

import androidx.room3.ColumnInfo
import androidx.room3.Entity

// Successful sends recorded by destination, so an address saved or synced later still gets its last used time.
@Entity(tableName = "address_usages", primaryKeys = ["chain_id", "destination", "tag"])
data class AddressUsage(
    @ColumnInfo(name = "chain_id")
    val chainId: String,
    @ColumnInfo(name = "destination")
    val destination: String,
    @ColumnInfo(name = "tag")
    val tag: String,
    @ColumnInfo(name = "used_at")
    val usedAt: Long,
)
