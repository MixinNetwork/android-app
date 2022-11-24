package one.mixin.android.crypto.vo

import androidx.room.ColumnInfo
import androidx.room.ColumnInfo.BLOB
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "prekeys", indices = [(Index(value = ["prekey_id"], unique = true))])
class PreKey(
    @ColumnInfo(name = "prekey_id")
    val preKeyId: Int,
    @ColumnInfo(name = "record", typeAffinity = BLOB)
    val record: ByteArray,
) {
    @PrimaryKey(autoGenerate = true)
    var _id: Int = 0
}
