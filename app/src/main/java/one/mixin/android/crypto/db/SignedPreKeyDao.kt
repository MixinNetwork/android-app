package one.mixin.android.crypto.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import one.mixin.android.crypto.vo.SignedPreKey
import one.mixin.android.db.BaseDao

@Dao
interface SignedPreKeyDao : BaseDao<SignedPreKey> {
    @Transaction
    @Query("SELECT * FROM signed_prekeys WHERE prekey_id = :signedPreKeyId")
    fun getSignedPreKey(signedPreKeyId: Int): SignedPreKey?

    @Transaction
    @Query("SELECT * FROM signed_prekeys")
    fun getSignedPreKeyList(): List<SignedPreKey>

    @Query("DELETE FROM signed_prekeys WHERE prekey_id = :signedPreKeyId")
    fun delete(signedPreKeyId: Int)
}
