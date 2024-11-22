package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

class ExportRequest (
    @SerializedName("pin_base64")
    val pinBase64:String,
    @SerializedName("master_public_hex")
    val publicKey:String,
    @SerializedName("master_signature_hex")
    val signature:String,
)
