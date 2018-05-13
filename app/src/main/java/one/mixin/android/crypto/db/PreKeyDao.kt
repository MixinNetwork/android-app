package one.mixin.android.crypto.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import android.arch.persistence.room.Transaction
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