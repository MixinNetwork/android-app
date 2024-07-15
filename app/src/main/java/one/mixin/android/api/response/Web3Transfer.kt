package one.mixin.android.api.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Web3Transfer(
    @SerializedName("asset_key")
    val assetKey: String,
    @SerializedName("chain_id")
    val chainId: String,
    val name: String,
    val symbol: String,
    @SerializedName("icon_url")
    val iconUrl: String,
    val direction: String,
    val sender: String,
    val amount: String,
    val price: String,
) : Parcelable {
    val tokenId: String
        get() {
            return chainId + assetKey
        }
}