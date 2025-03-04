package one.mixin.android.db.web3

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import kotlinx.coroutines.flow.Flow
import one.mixin.android.db.BaseDao
import one.mixin.android.db.TokenDao.Companion.POSTFIX_ASSET_ITEM
import one.mixin.android.db.TokenDao.Companion.PREFIX_ASSET_ITEM
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.vo.safe.TokenItem

@Dao
interface Web3TokenDao : BaseDao<Web3Token> {

    @Query("SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol FROM web3_tokens t LEFT JOIN web3_chains c ON c.chain_id = t.chain_id LEFT JOIN web3_tokens_extra ae ON ae.asset_id = t.asset_id WHERE ae.hidden != 1 OR ae.hidden IS NULL")
    fun web3TokenItems(): LiveData<List<Web3TokenItem>>
    
    @Query("SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol FROM web3_tokens t LEFT JOIN web3_chains c ON c.chain_id = t.chain_id LEFT JOIN web3_tokens_extra ae ON ae.asset_id = t.asset_id WHERE ae.hidden != 1 OR ae.hidden IS NULL")
    fun web3TokenItemsExcludeHidden(): LiveData<List<Web3TokenItem>>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol FROM web3_tokens t LEFT JOIN web3_chains c ON c.chain_id = t.chain_id LEFT JOIN web3_tokens_extra ae ON ae.asset_id = t.asset_id WHERE ae.hidden = 1")
    fun hiddenAssetItems(): LiveData<List<Web3TokenItem>>

    @Query("SELECT * FROM web3_tokens ORDER BY amount * price_usd DESC LIMIT 3")
    fun web3TokensFlow(): Flow<List<Web3Token>>

    @Query("SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol FROM web3_tokens t LEFT JOIN web3_chains c ON c.chain_id = t.chain_id WHERE t.chain_id = :chainId")
    fun web3TokenItemByChainId(chainId: String): Web3TokenItem
}
