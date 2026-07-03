package one.mixin.android.crypto.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import one.mixin.android.crypto.vo.RatchetSenderKey
import one.mixin.android.db.BaseDao

@Dao
interface RatchetSenderKeyDao : BaseDao<RatchetSenderKey> {
    @Transaction
    @Query("SELECT * FROM ratchet_sender_keys WHERE group_id = :groupId AND sender_id = :senderId")
    fun getRatchetSenderKey(
        groupId: String,
        senderId: String,
    ): RatchetSenderKey?

    @Query("DELETE FROM ratchet_sender_keys WHERE group_id = :groupId AND sender_id = :senderId")
    fun delete(
        groupId: String,
        senderId: String,
    )
}
