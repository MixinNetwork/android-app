package one.mixin.android.vo

import androidx.room.ColumnInfo

class AddressExt(
    @ColumnInfo(name = "address_id")
    val addressId: String,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "asset_id")
    val assetId: String,
    @ColumnInfo(name = "destination")
    val destination: String,
    @ColumnInfo(name = "label")
    val label: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "reserve")
    val reserve: String,
    @ColumnInfo(name = "fee")
    val fee: String,
    @ColumnInfo(name = "tag")
    val tag: String?,
    @ColumnInfo(name = "dust")
    val dust: String?,
    @ColumnInfo(name = "fee_asset_id")
    val feeAssetId: String?,
    @ColumnInfo(name = "symbol")
    val symbol: String?
)

fun AddressExt.displayAddress(): String {
    return if (!tag.isNullOrEmpty()) {
        "$destination:$tag"
    } else {
        destination
    }
}

fun AddressExt.feeSymbol(chainSymbol: String): String {
    return if (feeAssetId != null) requireNotNull(symbol)
    else chainSymbol
}

fun AddressExt.toAddress() = Address(
    addressId,
    type,
    assetId,
    destination,
    label,
    updatedAt,
    reserve,
    fee,
    tag,
    dust,
    feeAssetId
)
