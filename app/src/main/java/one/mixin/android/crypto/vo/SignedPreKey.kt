package one.mixin.android.crypto.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "signed_prekeys", indices = [(Index(value = ["prekey_id"], unique = true))])
class SignedPreKey(
    @ColumnInfo(name = "prekey_id")
    val preKeyId: Int,
    @ColumnInfo(name = "record", typeAffinity = ColumnInfo.BLOB)
    val record: ByteArray,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
) {
    @PrimaryKey(autoGenerate = true)
    var _id: Int = 0
}
