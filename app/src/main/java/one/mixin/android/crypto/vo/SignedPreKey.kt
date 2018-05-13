package one.mixin.android.crypto.vo

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "signed_prekeys", indices = [(Index(value = ["prekey_id"], unique = true))])
class SignedPreKey(
    @ColumnInfo(name = "prekey_id")
    val preKeyId: Int,
    @ColumnInfo(name = "record", typeAffinity = ColumnInfo.BLOB)
    val record: ByteArray,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long
) {
    @PrimaryKey(autoGenerate = true)
    var _id: Int = 0
}
