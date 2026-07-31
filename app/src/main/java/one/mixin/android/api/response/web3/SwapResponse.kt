package one.mixin.android.api.response.web3

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class SwapResponse(
    @SerializedName("tx")
    val tx: String?,
    @SerializedName("source")
    val source: String,
    @SerializedName("displayUserId")
    val displayUserId: String?,
    @SerializedName("depositDestination")
    val depositDestination: String?,
    @SerializedName("quote")
    val quote: QuoteResult,
): Parcelable
