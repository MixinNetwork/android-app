package one.mixin.android.ui.landing

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import one.mixin.android.util.GsonHelper

data class GTCaptcha4Result(
    @SerializedName("lot_number")
    val lotNumber: String?,
    @SerializedName("captcha_output")
    val captchaOutput: String?,
    @SerializedName("pass_token")
    val passToken: String?,
    @SerializedName("gen_time")
    val genTime: String?,
)

object GTCaptcha4Utils {
    fun parseGTCaptchaResponse(json: String): GTCaptcha4Result? {
        return try {
            return GsonHelper.customGson.fromJson(json, GTCaptcha4Result::class.java)
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
            null
        }
    }
}

