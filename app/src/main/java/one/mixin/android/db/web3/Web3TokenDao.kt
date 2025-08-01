package one.mixin.android.db.web3

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import kotlinx.coroutines.flow.Flow
import one.mixin.android.db.BaseDao
import one.mixin.android.db.BaseDao.Companion.ESCAPE_SUFFIX
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.vo.safe.UnifiedAssetItem

@Dao
interface Web3TokenDao : BaseDao<Web3Token> {

    @Query(
        """SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t
        LEFT JOIN chains c ON c.chain_id = t.chain_id LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id AND te.wallet_id = t.wallet_id
        WHERE t.wallet_id = :walletId 
        ORDER BY t.amount * t.price_usd DESC, cast(t.amount AS REAL) DESC, cast(t.price_usd AS REAL) DESC, t.name ASC, t.rowid ASC
    """
    )
    fun web3TokenItems(walletId: String): LiveData<List<Web3TokenItem>>

    @Query(
        """SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t
        LEFT JOIN chains c ON c.chain_id = t.chain_id LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id AND te.wallet_id = t.wallet_id
        WHERE t.wallet_id = :walletId AND t.level >= :level
        ORDER BY t.amount * t.price_usd DESC, cast(t.amount AS REAL) DESC, cast(t.price_usd AS REAL) DESC, t.name ASC, t.rowid ASC
    """
    )
    fun web3TokenItems(walletId: String, level:Int): LiveData<List<Web3TokenItem>>

    @Query("SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t LEFT JOIN chains c ON c.chain_id = t.chain_id LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id AND te.wallet_id = t.wallet_id WHERE t.wallet_id = :walletId")
    suspend fun findWeb3TokenItems(walletId: String): List<Web3TokenItem>

    @Query("SELECT t.symbol, t.icon_url AS iconUrl, t.amount AS balance, t.price_usd AS priceUsd FROM tokens t LEFT JOIN tokens_extra te ON t.asset_id = te.asset_id AND t.wallet_id = te.wallet_id WHERE t.amount * t.price_usd > 0 AND t.wallet_id = :walletId AND (te.hidden IS NULL OR te.hidden = 0) ORDER BY t.amount * t.price_usd")
    suspend fun findUnifiedAssetItem(walletId: String): List<UnifiedAssetItem>

    @Query("SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t LEFT JOIN chains c ON c.chain_id = t.chain_id LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id AND te.wallet_id = t.wallet_id WHERE t.amount > 0 AND t.wallet_id = :walletId")
    suspend fun findAssetItemsWithBalance(walletId: String): List<Web3TokenItem>
    
    @Query("""SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t LEFT JOIN chains c ON c.chain_id = t.chain_id LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id AND te.wallet_id = t.wallet_id WHERE (te.hidden != 1 OR te.hidden IS NULL) AND t.wallet_id = :walletId
        ORDER BY t.amount * t.price_usd DESC, cast(t.amount AS REAL) DESC, cast(t.price_usd AS REAL) DESC, t.name ASC, t.rowid ASC
    """)
    fun web3TokenItemsExcludeHidden(walletId: String): LiveData<List<Web3TokenItem>>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("""SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t LEFT JOIN chains c ON c.chain_id = t.chain_id LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id AND te.wallet_id = t.wallet_id WHERE te.hidden = 1 AND (:walletId IS NULL OR t.wallet_id = :walletId) ORDER BY t.amount * t.price_usd DESC, cast(t.amount AS REAL) DESC, cast(t.price_usd AS REAL) DESC, t.name ASC, t.rowid ASC""")
    fun hiddenAssetItems(walletId: String): LiveData<List<Web3TokenItem>>

    @Query("SELECT * FROM tokens WHERE amount * price_usd > 0 AND wallet_id = :walletId ORDER BY amount * price_usd")
    fun web3TokensFlow(walletId: String): Flow<List<Web3Token>>

    @Query("SELECT t.symbol, t.icon_url AS iconUrl, t.amount AS balance, t.price_usd AS priceUsd FROM tokens t LEFT JOIN tokens_extra te ON t.asset_id = te.asset_id AND t.wallet_id = te.wallet_id WHERE t.amount * t.price_usd > 0 AND t.wallet_id IN (:walletIds) AND (te.hidden IS NULL OR te.hidden = 0) ORDER BY t.amount * t.price_usd")
    suspend fun allWeb3Tokens(walletIds: List<String>): List<UnifiedAssetItem>

    @Query("SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t LEFT JOIN chains c ON c.chain_id = t.chain_id LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id AND te.wallet_id = t.wallet_id WHERE t.wallet_id = :walletId AND t.asset_id = :assetId")
    fun web3TokenItemById(walletId: String, assetId: String): Web3TokenItem?

    @Query("SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t LEFT JOIN chains c ON c.chain_id = t.chain_id LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id AND te.wallet_id = t.wallet_id WHERE t.asset_key = :address")
    suspend fun web3TokenItemByAddress(address: String): Web3TokenItem?

    @Query("SELECT * FROM tokens WHERE asset_id = :assetId AND wallet_id = :walletId")
    fun findTokenById(walletId: String, assetId: String): Web3Token?

    @Query("UPDATE tokens SET amount = '0' WHERE wallet_id = :walletId AND asset_id NOT IN (:assetIds)")
    suspend fun updateBalanceToZeroForMissingAssets(walletId: String, assetIds: List<String>)
    
    @Query("UPDATE tokens SET amount = '0' WHERE wallet_id = :walletId")
    suspend fun updateAllBalancesToZero(walletId: String)

    @Query("SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t LEFT JOIN chains c ON c.chain_id = t.chain_id LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id AND te.wallet_id = t.wallet_id WHERE t.wallet_id = :walletId AND t.asset_id IN (:assetIds)")
    suspend fun findWeb3TokenItemsByIds(walletId: String, assetIds: List<String>): List<Web3TokenItem>

    @Query("SELECT amount FROM tokens WHERE asset_id = :assetId AND wallet_id = :walletId")
    fun tokenExtraFlow(walletId: String, assetId: String): Flow<String?>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol FROM tokens t LEFT JOIN chains c ON c.chain_id = t.chain_id 
        WHERE wallet_id = :walletId AND (t.symbol LIKE '%' || :symbol || '%' $ESCAPE_SUFFIX OR t.name LIKE '%' || :name || '%' $ESCAPE_SUFFIX)
        ORDER BY t.symbol = :symbol COLLATE NOCASE OR t.name = :name COLLATE NOCASE DESC
        """,
    )
    suspend fun fuzzySearchAsset(
        walletId: String,
        name: String,
        symbol: String,
    ): List<Web3TokenItem>

    @Query(
        """SELECT * FROM tokens  
           WHERE asset_id IN (:usdIds) AND asset_id != :excludeId 
           ORDER BY COALESCE(amount * price_usd, 0) DESC 
           LIMIT 1"""
    )
    suspend fun findTopUsdBalanceAsset(usdIds: List<String>, excludeId: String): Web3Token?

    @Query("DELETE FROM tokens WHERE wallet_id = :walletId")
    suspend fun deleteByWalletId(walletId: String)
}
