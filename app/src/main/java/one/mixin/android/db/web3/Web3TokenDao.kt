package one.mixin.android.db.web3

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import one.mixin.android.db.BaseDao
import one.mixin.android.db.web3.vo.Web3Token

@Dao
interface Web3TokenDao : BaseDao<Web3Token> {

    @Query("SELECT * FROM web3_tokens")
    fun web3Tokens(): LiveData<List<Web3Token>>

    @Query("SELECT * FROM web3_tokens ORDER BY balance * price DESC LIMIT 3")
    fun web3TokensFlow(): Flow<List<Web3Token>>
}
