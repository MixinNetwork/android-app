package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class MemberOrderRequest(
    val category: String ="SUB",
    val plan: String,
    val asset: String,
    val source: Strin="mixin"
)
