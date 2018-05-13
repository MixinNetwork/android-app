package one.mixin.android.crypto.vo

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.ColumnInfo.BLOB
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "prekeys", indices = [(Index(value = ["prekey_id"], unique = true))])
class PreKey(
    @ColumnInfo(name = "prekey_id")
    val preKeyId: Int,
    @ColumnInfo(name = "record", typeAffinity = BLOB)
    val record: ByteArray
) {
    @PrimaryKey(autoGenerate = true)
    var _id: Int = 0
}
