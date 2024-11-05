package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

class ExportRequest (
    @SerializedName("pin_base64")
    val pinBase64:String,
    @SerializedName("public_key_hex")
    val publicKey:String,
    @SerializedName("signature_hex")
    val signature:String,
)
