package one.mixin.android.crypto.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
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
