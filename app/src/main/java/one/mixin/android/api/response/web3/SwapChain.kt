package one.mixin.android.api.response.web3

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class SwapChain(
    @SerializedName("decimals") val decimals: Int,
    @SerializedName("name") val name: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("logoURI") val chainLogoURI: String,
    var price: String? = null,
) : Parcelable
