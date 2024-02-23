package one.mixin.android.vo

import com.google.gson.annotations.SerializedName
import one.mixin.android.extension.decodeBase64
import one.mixin.android.util.GsonHelper
import org.webrtc.SessionDescription

data class KrakenData(
    val jsep: String,
    @SerializedName("track_id") val trackId: String,
) {
    fun getSessionDescription(): SessionDescription? {
        val jsep = jsep.decodeBase64()
        return getSdp(jsep)
    }
}

data class Sdp(val sdp: String, val type: String)

fun getSdp(json: ByteArray): SessionDescription? {
    val sdp = GsonHelper.customGson.fromJson(String(json), Sdp::class.java) ?: return null
    return SessionDescription(getType(sdp.type), sdp.sdp)
}

fun getType(type: String): SessionDescription.Type {
    return when (type) {
        SessionDescription.Type.OFFER.canonicalForm() -> SessionDescription.Type.OFFER
        SessionDescription.Type.ANSWER.canonicalForm() -> SessionDescription.Type.ANSWER
        SessionDescription.Type.PRANSWER.canonicalForm() -> SessionDescription.Type.PRANSWER
        else -> SessionDescription.Type.PRANSWER
    }
}
