package one.mixin.android.crypto.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import android.arch.persistence.room.Transaction
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