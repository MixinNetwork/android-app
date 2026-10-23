package one.mixin.android.crypto.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
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
