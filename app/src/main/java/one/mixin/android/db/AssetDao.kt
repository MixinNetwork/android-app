package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import one.mixin.android.db.BaseDao.Companion.ESCAPE_SUFFIX
import one.mixin.android.vo.Asset
import one.mixin.android.vo.AssetItem

@Dao
interface AssetDao : BaseDao<Asset> {
    companion object {
        const val PREFIX_ASSET_ITEM = "SELECT a1.asset_id AS assetId, a1.symbol, a1.name, a1.icon_url AS iconUrl, " +
            "a1.balance, a1.destination AS destination, a1.tag AS tag, a1.price_btc AS priceBtc, a1.price_usd AS priceUsd, " +
            "a1.chain_id AS chainId, a1.change_usd AS changeUsd, a1.change_btc AS changeBtc, ae.hidden, a2.price_usd as chainPriceUsd," +
            "a1.confirmations, a1.reserve as reserve, a2.icon_url AS chainIconUrl, a2.symbol as chainSymbol, a2.name as chainName, " +
            "a1.asset_key AS assetKey FROM assets a1 " +
            "LEFT JOIN assets a2 ON a1.chain_id = a2.asset_id " +
            "LEFT JOIN assets_extra ae ON ae.asset_id = a1.asset_id "
        const val POSTFIX = " ORDER BY balance * price_usd DESC, price_usd DESC, cast(balance AS REAL) DESC, name DESC, rowid DESC"
        const val POSTFIX_ASSET_ITEM = " ORDER BY a1.balance * a1.price_usd DESC, a1.price_usd DESC, cast(a1.balance AS REAL) DESC, a1.name DESC"
        const val POSTFIX_ASSET_ITEM_NOT_HIDDEN = " WHERE ae.hidden IS NULL OR NOT ae.hidden$POSTFIX_ASSET_ITEM"
    }

    @Query("SELECT * FROM assets $POSTFIX")
    fun assets(): LiveData<List<Asset>>

    @Query("SELECT * FROM assets WHERE balance > 0 $POSTFIX")
    fun assetsWithBalance(): LiveData<List<Asset>>

    @Query("SELECT * FROM assets WHERE balance > 0 $POSTFIX")
    suspend fun simpleAssetsWithBalance(): List<Asset>

    @Query("$PREFIX_ASSET_ITEM WHERE a1.symbol = 'XIN' $POSTFIX_ASSET_ITEM limit 1")
    fun getXIN(): AssetItem?

    @Query("SELECT * FROM assets WHERE asset_id = :id")
    fun asset(id: String): LiveData<Asset>

    @Query("SELECT * FROM assets WHERE asset_id = :id")
    suspend fun simpleAsset(id: String): Asset?

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("$PREFIX_ASSET_ITEM WHERE ae.hidden = 1 $POSTFIX_ASSET_ITEM")
    fun hiddenAssetItems(): LiveData<List<AssetItem>>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("$PREFIX_ASSET_ITEM $POSTFIX_ASSET_ITEM_NOT_HIDDEN")
    fun assetItemsNotHidden(): LiveData<List<AssetItem>>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """$PREFIX_ASSET_ITEM 
        WHERE a1.balance > 0 
        AND (a1.symbol LIKE '%' || :symbol || '%' $ESCAPE_SUFFIX OR a1.name LIKE '%' || :name || '%' $ESCAPE_SUFFIX)
        ORDER BY 
            a1.symbol = :symbol COLLATE NOCASE OR a1.name = :name COLLATE NOCASE DESC,
            a1.price_usd*a1.balance DESC
        """
    )
    suspend fun fuzzySearchAsset(name: String, symbol: String): List<AssetItem>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("$PREFIX_ASSET_ITEM WHERE a1.asset_id = :id")
    fun assetItem(id: String): LiveData<AssetItem>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("$PREFIX_ASSET_ITEM WHERE a1.asset_id = :assetId")
    suspend fun simpleAssetItem(assetId: String): AssetItem?

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("$PREFIX_ASSET_ITEM WHERE a1.balance > 0 $POSTFIX_ASSET_ITEM")
    fun assetItemsWithBalance(): LiveData<List<AssetItem>>

    @Query("SELECT icon_url FROM assets WHERE asset_id = :id")
    suspend fun getIconUrl(id: String): String?

    @Query("SELECT asset_id FROM assets WHERE asset_id = :id")
    fun checkExists(id: String): String?

    @Query("$PREFIX_ASSET_ITEM WHERE a1.asset_id = :assetId")
    suspend fun findAssetItemById(assetId: String): AssetItem?

    @Query("SELECT asset_id FROM assets WHERE balance > 0")
    suspend fun findAllAssetIdSuspend(): List<String>

    @Query("UPDATE assets SET balance = 0 WHERE asset_id IN (:assetIds)")
    suspend fun zeroClearSuspend(assetIds: List<String>)
}
