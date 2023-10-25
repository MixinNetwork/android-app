package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Update
import one.mixin.android.db.BaseDao.Companion.ESCAPE_SUFFIX
import one.mixin.android.vo.Asset
import one.mixin.android.vo.PriceAndChange
import one.mixin.android.vo.safe.Token
import one.mixin.android.vo.safe.TokenItem

@Dao
interface TokenDao : BaseDao<Token> {
    companion object {
        const val PREFIX_ASSET_ITEM =
           """
            SELECT a1.asset_id AS assetId, a1.symbol, a1.name, a1.icon_url AS iconUrl, COALESCE(ae.balance,'0') as balance,
            d.destination as destination, d.tag as tag, a1.price_btc AS priceBtc, 
            a1.chain_id AS chainId , a1.price_usd AS priceUsd, a1.change_usd AS changeUsd, a1.change_btc AS changeBtc, ae.hidden,
            a1.confirmations,c.icon_url AS chainIconUrl, c.symbol as chainSymbol, c.name as chainName, a2.price_usd as chainPriceUsd,
            a1.asset_key AS assetKey, c.withdrawal_memo_possibility AS withdrawalMemoPossibility 
            FROM tokens a1 
            LEFT JOIN tokens a2 ON a1.chain_id = a2.asset_id
            LEFT JOIN deposit_entries d ON a1.chain_id = d.chain_id 
            LEFT JOIN chains c ON a1.chain_id = c.chain_id
            LEFT JOIN tokens_extra ae ON ae.asset_id = a1.asset_id 
           """
        const val POSTFIX =
            " ORDER BY ae.balance * a1.price_usd DESC, cast(ae.balance AS REAL) DESC, cast(a1.price_usd AS REAL) DESC, a1.name ASC, a1.rowid DESC"
        const val POSTFIX_ASSET_ITEM =
            " ORDER BY ae.balance * a1.price_usd DESC, cast(ae.balance AS REAL) DESC, cast(a1.price_usd AS REAL) DESC, a1.name ASC"
        const val POSTFIX_ASSET_ITEM_NOT_HIDDEN =
            " WHERE ae.hidden IS NULL OR NOT ae.hidden$POSTFIX_ASSET_ITEM"
    }
    @Query("SELECT * FROM tokens a1 LEFT JOIN tokens_extra ae ON ae.asset_id = a1.asset_id $POSTFIX")
    fun assets(): LiveData<List<Token>>

    @Query("SELECT a1.* FROM tokens a1 LEFT JOIN tokens_extra ae ON ae.asset_id = a1.asset_id WHERE balance > 0 $POSTFIX")
    fun assetsWithBalance(): LiveData<List<Token>>

    @Query("SELECT a1.* FROM tokens a1 LEFT JOIN tokens_extra ae ON ae.asset_id = a1.asset_id WHERE balance > 0 $POSTFIX")
    suspend fun simpleAssetsWithBalance(): List<Token>

    @Query("SELECT asset_id FROM tokens WHERE kernel_asset_id = :asset")
    suspend fun checkAssetExists(asset: String): String?

    @Query("SELECT * FROM tokens WHERE kernel_asset_id = :asset")
    suspend fun findTokenByAsset(asset: String): Token?

    @Query("$PREFIX_ASSET_ITEM WHERE a1.symbol = 'XIN' $POSTFIX_ASSET_ITEM limit 1")
    fun getXIN(): TokenItem?

    @Query("SELECT * FROM tokens WHERE asset_id = :id")
    fun asset(id: String): LiveData<Token>

    @Query("SELECT * FROM tokens WHERE asset_id = :id")
    suspend fun simpleAsset(id: String): Token?

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("$PREFIX_ASSET_ITEM WHERE ae.hidden = 1 $POSTFIX_ASSET_ITEM")
    fun hiddenAssetItems(): LiveData<List<TokenItem>>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("$PREFIX_ASSET_ITEM $POSTFIX_ASSET_ITEM_NOT_HIDDEN")
    fun assetItemsNotHidden(): LiveData<List<TokenItem>>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("$PREFIX_ASSET_ITEM $POSTFIX_ASSET_ITEM")
    fun assetItems(): LiveData<List<TokenItem>>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("""$PREFIX_ASSET_ITEM WHERE a1.asset_id IN (:assetIds) $POSTFIX_ASSET_ITEM """)
    fun assetItems(assetIds: List<String>): LiveData<List<TokenItem>>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """$PREFIX_ASSET_ITEM 
        WHERE ae.balance > 0 
        AND (a1.symbol LIKE '%' || :symbol || '%' $ESCAPE_SUFFIX OR a1.name LIKE '%' || :name || '%' $ESCAPE_SUFFIX)
        ORDER BY 
            a1.symbol = :symbol COLLATE NOCASE OR a1.name = :name COLLATE NOCASE DESC,
            a1.price_usd*ae.balance DESC
        """,
    )
    suspend fun fuzzySearchAsset(name: String, symbol: String): List<TokenItem>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """$PREFIX_ASSET_ITEM 
        WHERE (a1.symbol LIKE '%' || :symbol || '%' $ESCAPE_SUFFIX OR a1.name LIKE '%' || :name || '%' $ESCAPE_SUFFIX)
        ORDER BY 
            a1.symbol = :symbol COLLATE NOCASE OR a1.name = :name COLLATE NOCASE DESC,
            a1.price_usd*ae.balance DESC
        """,
    )
    suspend fun fuzzySearchAssetIgnoreAmount(name: String, symbol: String): List<TokenItem>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("$PREFIX_ASSET_ITEM WHERE a1.asset_id = :id")
    fun assetItem(id: String): LiveData<TokenItem>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("$PREFIX_ASSET_ITEM WHERE a1.asset_id = :assetId")
    suspend fun simpleAssetItem(assetId: String): TokenItem?

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("$PREFIX_ASSET_ITEM WHERE ae.balance > 0 $POSTFIX_ASSET_ITEM")
    fun assetItemsWithBalance(): LiveData<List<TokenItem>>

    @Query("SELECT icon_url FROM tokens WHERE asset_id = :id")
    suspend fun getIconUrl(id: String): String?

    @Query("SELECT asset_id FROM tokens WHERE asset_id = :id")
    fun checkExists(id: String): String?

    @Query("$PREFIX_ASSET_ITEM WHERE a1.asset_id = :assetId")
    suspend fun findAssetItemById(assetId: String): TokenItem?

    @Query("SELECT t.asset_id FROM tokens t LEFT JOIN tokens_extra te ON te.asset_id = t.asset_id WHERE te.balance > 0")
    suspend fun findAllAssetIdSuspend(): List<String>

    @Query("UPDATE tokens_extra SET balance = 0 WHERE asset_id IN (:assetIds)")
    suspend fun zeroClearSuspend(assetIds: List<String>)

    @Query("$PREFIX_ASSET_ITEM WHERE a1.asset_id IN (:assetIds)")
    suspend fun suspendFindAssetsByIds(assetIds: List<String>): List<TokenItem>

    @Update(entity = Asset::class)
    suspend fun suspendUpdatePrices(priceAndChanges: List<PriceAndChange>)

    @Query("SELECT SUM(balance * price_usd) FROM tokens a1 LEFT JOIN tokens_extra ae ON ae.asset_id = a1.asset_id")
    suspend fun findTotalUSDBalance(): Int?

    @Query("SELECT asset_id FROM tokens WHERE asset_key = :assetKey COLLATE NOCASE")
    suspend fun findAssetIdByAssetKey(assetKey: String): String?

    // Todo replace
    @Query("SELECT a.* FROM assets a WHERE a.rowid > :rowId ORDER BY a.rowid ASC LIMIT :limit")
    fun getAssetByLimitAndRowId(limit: Int, rowId: Long): List<Asset>

    @Query("SELECT rowid FROM assets WHERE asset_id = :assetId")
    fun getAssetRowId(assetId: String): Long?

    @Query("SELECT count(1) FROM tokens")
    fun countAssets(): Long

    @Query("SELECT count(1) FROM assets WHERE rowid > :rowId")
    fun countAssets(rowId: Long): Long
}
