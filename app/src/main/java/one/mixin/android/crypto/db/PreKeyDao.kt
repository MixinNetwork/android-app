package one.mixin.android.crypto.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
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
