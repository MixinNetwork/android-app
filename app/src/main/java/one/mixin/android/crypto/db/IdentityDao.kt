package one.mixin.android.crypto.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import android.arch.persistence.room.Transaction
import one.mixin.android.crypto.vo.Identity
import one.mixin.android.db.BaseDao

@Dao
interface IdentityDao : BaseDao<Identity> {

    @Transaction
    @Query("SELECT * FROM identities WHERE address = '-1'")
    fun getLocalIdentity(): Identity

    @Transaction
    @Query("SELECT * FROM identities WHERE address = :address")
    fun getIdentity(address: String): Identity?

    @Query("DELETE FROM identities WHERE address = :address")
    fun deleteIdentity(address: String)
}