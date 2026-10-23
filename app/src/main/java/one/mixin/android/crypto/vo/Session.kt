package one.mixin.android.crypto.vo

import androidx.room3.ColumnInfo
import androidx.room3.ColumnInfo.Companion.BLOB
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "sessions", indices = [(Index(value = ["address", "device"], unique = true))])
class Session(
    @ColumnInfo(name = "address")
    val address: String,
    @ColumnInfo(name = "device")
    var device: Int,
    @ColumnInfo(name = "record", typeAffinity = BLOB)
    val record: ByteArray,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
) {
    @PrimaryKey(autoGenerate = true)
    var _id: Int = 0
}
