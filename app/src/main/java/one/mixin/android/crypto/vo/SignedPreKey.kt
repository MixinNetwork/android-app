package one.mixin.android.crypto.vo

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

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
