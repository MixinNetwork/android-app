package one.mixin.android.db.web3

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.RoomWarnings
import kotlinx.coroutines.flow.Flow
import one.mixin.android.Constants
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
        ORDER BY (CASE WHEN t.icon_url = :defaultIconUrl THEN 1 ELSE 0 END) ASC, t.amount * t.price_usd DESC, cast(t.amount AS REAL) DESC, cast(t.price_usd AS REAL) DESC, t.name ASC, c.name ASC, t.rowid ASC
    """
    )
    fun web3TokenItems(walletId: String, defaultIconUrl: String = Constants.DEFAULT_ICON_URL): LiveData<List<Web3TokenItem>>

    @Query(
        """SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t
        LEFT JOIN chains c ON c.chain_id = t.chain_id LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id AND te.wallet_id = t.wallet_id
        WHERE t.wallet_id = :walletId AND t.level >= :level
        ORDER BY (CASE WHEN t.icon_url = :defaultIconUrl THEN 1 ELSE 0 END) ASC, t.amount * t.price_usd DESC, cast(t.amount AS REAL) DESC, cast(t.price_usd AS REAL) DESC, t.name ASC, c.name ASC, t.rowid ASC
    """
    )
    fun web3TokenItems(walletId: String, level:Int, defaultIconUrl: String = Constants.DEFAULT_ICON_URL): LiveData<List<Web3TokenItem>>

    @Query("SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t LEFT JOIN chains c ON c.chain_id = t.chain_id LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id AND te.wallet_id = t.wallet_id WHERE t.wallet_id = :walletId")
    suspend fun findWeb3TokenItems(walletId: String): List<Web3TokenItem>

    @Query("SELECT t.symbol, t.icon_url AS iconUrl, t.amount AS balance, t.price_usd AS priceUsd FROM tokens t LEFT JOIN tokens_extra te ON t.asset_id = te.asset_id AND t.wallet_id = te.wallet_id WHERE t.amount * t.price_usd > 0 AND t.wallet_id = :walletId AND (te.hidden IS NULL OR te.hidden = 0) ORDER BY t.amount * t.price_usd")
    suspend fun findUnifiedAssetItem(walletId: String): List<UnifiedAssetItem>

    @Query("SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t LEFT JOIN chains c ON c.chain_id = t.chain_id LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id AND te.wallet_id = t.wallet_id WHERE t.amount > 0 AND t.wallet_id = :walletId AND (te.hidden IS NULL OR te.hidden = 0)")
    suspend fun findAssetItemsWithBalance(walletId: String): List<Web3TokenItem>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("""SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t
        LEFT JOIN chains c ON c.chain_id = t.chain_id 
        LEFT JOIN tokens_extra te ON te.wallet_id = t.wallet_id AND te.asset_id = t.asset_id
        WHERE t.wallet_id = :walletId AND (te.hidden != 1 OR te.hidden IS NULL) 
        ORDER BY (CASE WHEN t.icon_url = :defaultIconUrl THEN 1 ELSE 0 END) ASC, t.amount * t.price_usd DESC, cast(t.amount AS REAL) DESC, cast(t.price_usd AS REAL) DESC, t.name ASC, c.name ASC, t.rowid ASC
    """)
    fun web3TokenItemsExcludeHidden(walletId: String, defaultIconUrl: String = Constants.DEFAULT_ICON_URL): LiveData<List<Web3TokenItem>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("""SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t
        LEFT JOIN chains c ON c.chain_id = t.chain_id 
        LEFT JOIN tokens_extra te ON te.wallet_id = t.wallet_id AND te.asset_id = t.asset_id
        WHERE t.wallet_id = :walletId AND t.amount > 0 AND (te.hidden != 1 OR te.hidden IS NULL) 
        ORDER BY (CASE WHEN t.icon_url = :defaultIconUrl THEN 1 ELSE 0 END) ASC, t.amount * t.price_usd DESC, cast(t.amount AS REAL) DESC, cast(t.price_usd AS REAL) DESC, t.name ASC, c.name ASC, t.rowid ASC
    """)
    fun web3TokenItemsExcludeHiddenWithBalance(walletId: String, defaultIconUrl: String = Constants.DEFAULT_ICON_URL): LiveData<List<Web3TokenItem>>


    @RawQuery
    fun web3TokenItemsExcludeHiddenRaw(query: RoomRawQuery): List<Web3TokenItem>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("""SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t LEFT JOIN chains c ON c.chain_id = t.chain_id LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id AND te.wallet_id = t.wallet_id WHERE te.hidden = 1 AND (:walletId IS NULL OR t.wallet_id = :walletId) ORDER BY (CASE WHEN t.icon_url = :defaultIconUrl THEN 1 ELSE 0 END) ASC, t.amount * t.price_usd DESC, cast(t.amount AS REAL) DESC, cast(t.price_usd AS REAL) DESC, t.name ASC, c.name ASC, t.rowid ASC""")
    fun hiddenAssetItems(walletId: String, defaultIconUrl: String = Constants.DEFAULT_ICON_URL): LiveData<List<Web3TokenItem>>

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

    @Query("SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t LEFT JOIN chains c ON c.chain_id = t.chain_id LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id AND te.wallet_id = t.wallet_id WHERE t.wallet_id = :walletId AND t.asset_id IN (:assetIds)")
    fun findWeb3TokenItemsByIdsSync(walletId: String, assetIds: List<String>): List<Web3TokenItem>

    @Query("SELECT amount FROM tokens WHERE asset_id = :assetId AND wallet_id = :walletId")
    fun tokenExtraFlow(walletId: String, assetId: String): Flow<String?>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query(
        """SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t 
        LEFT JOIN chains c ON c.chain_id = t.chain_id 
        LEFT JOIN tokens_extra te ON te.wallet_id = t.wallet_id AND te.asset_id = t.asset_id
        WHERE t.wallet_id = :walletId 
        AND (t.symbol LIKE '%' || :query || '%' $ESCAPE_SUFFIX OR t.name LIKE '%' || :query || '%' $ESCAPE_SUFFIX)
        AND (t.level >= 10 OR (t.level < 10 AND (te.hidden IS NULL OR te.hidden = 0)))
        ORDER BY t.symbol = :query COLLATE NOCASE OR t.name = :query COLLATE NOCASE DESC
        """,
    )
    suspend fun fuzzySearchAsset(
        walletId: String,
        query: String,
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

    @Query(
        """SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t
        LEFT JOIN chains c ON c.chain_id = t.chain_id LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id AND te.wallet_id = t.wallet_id
        WHERE t.wallet_id IN (:walletIds) 
        GROUP BY t.asset_id
        ORDER BY (CASE WHEN t.icon_url = :defaultIconUrl THEN 1 ELSE 0 END) ASC, t.amount * t.price_usd DESC, cast(t.amount AS REAL) DESC, cast(t.price_usd AS REAL) DESC, t.name ASC, c.name ASC, t.rowid ASC
    """
    )
    fun web3TokenItemsByWalletIds(walletIds: List<String>, defaultIconUrl: String = Constants.DEFAULT_ICON_URL): LiveData<List<Web3TokenItem>>

    @Query(
        """SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t
        LEFT JOIN chains c ON c.chain_id = t.chain_id 
        LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id AND te.wallet_id = t.wallet_id
        GROUP BY t.asset_id
        ORDER BY (CASE WHEN t.icon_url = :defaultIconUrl THEN 1 ELSE 0 END) ASC, t.amount * t.price_usd DESC, cast(t.amount AS REAL) DESC, cast(t.price_usd AS REAL) DESC, t.name ASC, c.name ASC, t.rowid ASC
    """
    )
    fun web3TokenItemsAll(defaultIconUrl: String = Constants.DEFAULT_ICON_URL): LiveData<List<Web3TokenItem>>

    @Query(
        """SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t
        LEFT JOIN chains c ON c.chain_id = t.chain_id 
        LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id AND te.wallet_id = t.wallet_id
        WHERE t.asset_id IN (
            SELECT DISTINCT pay_asset_id FROM orders
            UNION
            SELECT DISTINCT receive_asset_id FROM orders
        )
        GROUP BY t.asset_id
        ORDER BY (CASE WHEN t.icon_url = :defaultIconUrl THEN 1 ELSE 0 END) ASC, t.amount * t.price_usd DESC, cast(t.amount AS REAL) DESC, cast(t.price_usd AS REAL) DESC, t.name ASC, c.name ASC, t.rowid ASC
    """
    )
    fun web3TokenItemsFromAllOrders(defaultIconUrl: String = Constants.DEFAULT_ICON_URL): LiveData<List<Web3TokenItem>>

    @Query(
        """SELECT t.*, c.icon_url as chain_icon_url, c.name as chain_name, c.symbol as chain_symbol, te.hidden FROM tokens t
        LEFT JOIN chains c ON c.chain_id = t.chain_id 
        LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id AND te.wallet_id = t.wallet_id
        WHERE t.asset_id IN (
            SELECT DISTINCT pay_asset_id FROM orders WHERE wallet_id IN (:walletIds)
            UNION
            SELECT DISTINCT receive_asset_id FROM orders WHERE wallet_id IN (:walletIds)
        )
        GROUP BY t.asset_id
        ORDER BY (CASE WHEN t.icon_url = :defaultIconUrl THEN 1 ELSE 0 END) ASC, t.amount * t.price_usd DESC, cast(t.amount AS REAL) DESC, cast(t.price_usd AS REAL) DESC, t.name ASC, c.name ASC, t.rowid ASC
    """
    )
    fun web3TokenItemsFromOrdersByWalletIds(walletIds: List<String>, defaultIconUrl: String = Constants.DEFAULT_ICON_URL): LiveData<List<Web3TokenItem>>
}
