package one.mixin.android.crypto.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import one.mixin.android.crypto.vo.PreKey
import one.mixin.android.db.BaseDao

@Dao
interface PreKeyDao : BaseDao<PreKey> {
    @Transaction
    @Query("SELECT * FROM prekeys WHERE prekey_id = :preKeyId")
    fun getPreKey(preKeyId: Int): PreKey?

    @Query("DELETE FROM prekeys WHERE prekey_id = :preKeyId")
    fun delete(preKeyId: Int)
}
