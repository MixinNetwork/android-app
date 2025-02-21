package one.mixin.android.db.web3

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.api.response.Web3Token
import one.mixin.android.db.BaseDao

@Dao
interface Web3TokenDao : BaseDao<Web3Token> {

    @Query("SELECT * FROM web3_tokens")
    fun web3Tokens(): LiveData<List<Web3Token>>
}
