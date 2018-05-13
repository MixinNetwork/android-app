package one.mixin.android.crypto.vo

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity

@Entity(tableName = "sender_keys", primaryKeys = ["group_id", "sender_id"])
class SenderKey(
    @ColumnInfo(name = "group_id")
    val groupId: String,
    @ColumnInfo(name = "sender_id")
    val senderId: String,
    @ColumnInfo(name = "record", typeAffinity = ColumnInfo.BLOB)
    val record: ByteArray
)