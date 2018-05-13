package one.mixin.android.crypto.vo

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.ColumnInfo.BLOB
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "sessions", indices = [(Index(value = ["address", "device"], unique = true))])
class Session(
    @ColumnInfo(name = "address")
    val address: String,
    @ColumnInfo(name = "device")
    val device: Int,
    @ColumnInfo(name = "record", typeAffinity = BLOB)
    val record: ByteArray,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long
) {
    @PrimaryKey(autoGenerate = true)
    var _id: Int = 0
}
