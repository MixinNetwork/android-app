package one.mixin.android.vo

import com.google.gson.annotations.SerializedName

class Dapp (
    @SerializedName("name")
    val name:String,
    @SerializedName("home_url")
    val homeUrl:String,
    @SerializedName("chains")
    val chains:List<String>,
    @SerializedName("icon_url")
    val iconUrl:String,
    @SerializedName("category")
    val category:String
)