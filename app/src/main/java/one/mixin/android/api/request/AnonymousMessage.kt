package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName
import one.mixin.android.extension.sha256
import one.mixin.android.extension.toHex
import one.mixin.android.util.GsonHelper
import timber.log.Timber
import kotlin.random.Random

class AnonymousMessage(
    @SerializedName("random")
    var random: String = "",
    @SerializedName("created_at")
    val createdAt: String
)

fun AnonymousMessage.doAnonymousPOW(anonymousNumberDifficulty: Int = 3): AnonymousMessage {
    val prefix = "0".repeat(anonymousNumberDifficulty)
    val data = ByteArray(32)

    while (true) {
        Random.nextBytes(data)
        this.random = data.toHex()
        val messageBuf = GsonHelper.customGson.toJson(this).toByteArray()
        val hash = messageBuf.sha256()
        val hashHex = hash.toHex()
        if (hashHex.startsWith(prefix)) {
            return this
        }
    }
}
