package one.mixin.android.api.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
class Web3Token(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("symbol")
    val symbol: String,
    @SerializedName("icon_url")
    val iconUrl: String,
    @SerializedName("chain_id")
    val chainId: String,
    @SerializedName("chain_icon_url")
    val chainIconUrl: String,
    @SerializedName("balance")
    val balance: String,
    @SerializedName("price")
    val price: String,
    @SerializedName("change_absolute")
    val changeAbsolute: String,
    @SerializedName("change_percent")
    val changePercent: String,
) : Parcelable