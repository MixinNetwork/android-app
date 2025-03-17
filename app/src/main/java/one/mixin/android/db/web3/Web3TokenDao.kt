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

    @Query("""SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t LEFT JOIN chains c ON c.chain_id = t.chain_id LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id
        ORDER BY t.amount * t.price_usd DESC, cast(t.amount AS REAL) DESC, cast(t.price_usd AS REAL) DESC, t.name ASC
    """)
    fun web3TokenItems(): LiveData<List<Web3TokenItem>>

    @Query("SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t LEFT JOIN chains c ON c.chain_id = t.chain_id LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id")
    suspend fun findWeb3TokenItems(): List<Web3TokenItem>
    
    @Query("""SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t LEFT JOIN chains c ON c.chain_id = t.chain_id LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id WHERE te.hidden != 1 OR te.hidden IS NULL
        ORDER BY t.amount * t.price_usd DESC, cast(t.amount AS REAL) DESC, cast(t.price_usd AS REAL) DESC, t.name ASC
    """)
    fun web3TokenItemsExcludeHidden(): LiveData<List<Web3TokenItem>>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("""SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t LEFT JOIN chains c ON c.chain_id = t.chain_id LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id WHERE te.hidden = 1
        ORDER BY t.amount * t.price_usd DESC, cast(t.amount AS REAL) DESC, cast(t.price_usd AS REAL) DESC, t.name ASC
    """)
    fun hiddenAssetItems(): LiveData<List<Web3TokenItem>>

    @Query("SELECT * FROM tokens WHERE amount * price_usd > 0 ORDER BY amount * price_usd")
    fun web3TokensFlow(): Flow<List<Web3Token>>

    @Query("SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t LEFT JOIN chains c ON c.chain_id = t.chain_id LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id WHERE t.asset_id = :assetId")
    fun web3TokenItemById(assetId: String): Web3TokenItem?

    @Query("SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t LEFT JOIN chains c ON c.chain_id = t.chain_id LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id WHERE t.asset_key = :address")
    suspend fun web3TokenItemByAddress(address: String): Web3TokenItem?
}
