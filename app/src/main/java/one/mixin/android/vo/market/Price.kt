package one.mixin.android.vo.market

import com.google.gson.annotations.SerializedName

class Price(
    @SerializedName("price")
    val price: String,
    @SerializedName("unix")
    val unix: Long,
)