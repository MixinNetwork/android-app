package one.mixin.android.api.response
import com.google.gson.annotations.SerializedName
data class Web3Transfer(
    @SerializedName("fungible_id")
    val fungibleId: String,
    val name: String,
    val symbol: String,
    @SerializedName("icon_url")
    val iconUrl: String,
    val direction: String,
    val sender: String,
    val amount: String,
    val price: String
)

