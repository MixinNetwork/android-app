package one.mixin.android.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import android.arch.persistence.room.RoomWarnings
import one.mixin.android.vo.Asset
import one.mixin.android.vo.AssetItem

@Dao
interface AssetDao : BaseDao<Asset> {
    companion object {
        const val PREFIX_ASSET_ITEM = "SELECT a1.asset_id AS assetId, a1.symbol, a1.name, a1.icon_url AS iconUrl, " +
            "a1.balance, a1.public_key AS publicKey, a1.price_btc AS priceBtc, a1.price_usd AS priceUsd, " +
            "a1.chain_id AS chainId, a1.change_usd AS changeUsd, a1.change_btc AS changeBtc, a1.hidden, " +
            "a1.confirmations, a2.icon_url AS chainIconUrl, a2.symbol as chainSymbol, " +
            "a1.account_name AS accountName, a1.account_memo AS accountMemo " +
            "FROM assets a1 " +
            "LEFT JOIN assets a2 ON a1.chain_id = a2.asset_id "
        const val POSTFIX = " ORDER BY balance * price_usd DESC, price_usd DESC, cast(balance AS REAL) DESC, name DESC"
        const val POSTFIX_ASSET_ITEM = "NOT (a1.balance = 0 AND a1.asset_id != a1.chain_id) " +
            "ORDER BY a1.balance * a1.price_usd DESC, a1.price_usd DESC, cast(a1.balance AS REAL) DESC, a1.name DESC"
    }

    @Query("SELECT * FROM assets $POSTFIX")
    fun assets(): LiveData<List<Asset>>

    @Query("SELECT * FROM assets WHERE balance > 0 $POSTFIX")
    fun assetsWithBalance(): LiveData<List<Asset>>

    @Query("SELECT * FROM assets WHERE balance > 0 $POSTFIX")
    fun simpleAssetsWithBalance(): List<Asset>

    @Query("$PREFIX_ASSET_ITEM WHERE a1.symbol = 'XIN' AND $POSTFIX_ASSET_ITEM limit 1")
    fun getXIN(): AssetItem?

    @Query("SELECT * FROM assets WHERE asset_id = :id")
    fun asset(id: String): LiveData<Asset>

    @Query("SELECT * FROM assets WHERE asset_id = :id")
    fun simpleAsset(id: String): Asset?

    @Query("UPDATE assets SET hidden = :hidden WHERE asset_id = :id")
    fun updateHidden(id: String, hidden: Boolean)

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("$PREFIX_ASSET_ITEM WHERE a1.hidden = 1 AND $POSTFIX_ASSET_ITEM")
    fun hiddenAssetItems(): LiveData<List<AssetItem>>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("$PREFIX_ASSET_ITEM WHERE $POSTFIX_ASSET_ITEM")
    fun assetItems(): LiveData<List<AssetItem>>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("$PREFIX_ASSET_ITEM WHERE a1.name LIKE :name OR a1.symbol LIKE :symbol " +
        "ORDER BY a1.price_usd*a1.balance DESC")
    fun fuzzySearchAsset(name: String, symbol: String): List<AssetItem>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("$PREFIX_ASSET_ITEM WHERE a1.asset_id = :id")
    fun assetItem(id: String): LiveData<AssetItem>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("$PREFIX_ASSET_ITEM WHERE a1.asset_id = :id")
    fun simpleAssetItem(id: String): AssetItem?

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("$PREFIX_ASSET_ITEM WHERE a1.balance > 0 AND $POSTFIX_ASSET_ITEM")
    fun assetItemsWithBalance(): LiveData<List<AssetItem>>
}
