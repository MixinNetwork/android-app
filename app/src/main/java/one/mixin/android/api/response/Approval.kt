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
    val name: String,
    val symbol: String,
    @SerializedName("icon_url") val iconUrl: String,
    val sender: String,
    val amount: String,
) : Parcelable
