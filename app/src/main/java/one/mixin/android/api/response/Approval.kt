package one.mixin.android.api.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Approval(
    @SerializedName("asset_key")
    val assetKey: String,
    @SerializedName("chain_id")
    val chainId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("symbol")
    val symbol: String,
    @SerializedName("icon_url") val iconUrl: String,
    @SerializedName("sender")
    val sender: String,
    @SerializedName("amount")
    val amount: String,
) : Parcelable
