package one.mixin.android.db.web3

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import one.mixin.android.db.BaseDao
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.db.web3.vo.Web3TokenItem

@Dao
interface Web3TokenDao : BaseDao<Web3Token> {

    @Query("SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol FROM web3_tokens t LEFT JOIN web3_chains c ON c.chain_id = t.chain_id")
    fun web3TokenItems(): LiveData<List<Web3TokenItem>>

    @Query("SELECT * FROM web3_tokens ORDER BY amount * price_usd DESC LIMIT 3")
    fun web3TokensFlow(): Flow<List<Web3Token>>
}
