package one.mixin.android.moshi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import one.mixin.android.api.SignedPreKey
import one.mixin.android.api.request.SignalKeyRequest
import one.mixin.android.moshi.adaptrer.BlazeMessageParamJsonAdapter
import one.mixin.android.moshi.adaptrer.CodeResponseJsonAdapter
import one.mixin.android.moshi.adaptrer.IceCandidateJsonAdapter
import one.mixin.android.moshi.adaptrer.MessageJsonAdapter
import one.mixin.android.moshi.adaptrer.QuoteMessageItemJsonAdapter
import one.mixin.android.moshi.adaptrer.SignalKeyRequestJsonAdapter
import one.mixin.android.moshi.adaptrer.SignedPreKeyJsonAdapter
import one.mixin.android.moshi.adaptrer.WebClipJsonAdapter
import one.mixin.android.ui.web.WebClip
import one.mixin.android.vo.CodeResponse
import one.mixin.android.vo.Message
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.websocket.BlazeMessageParam
import org.webrtc.IceCandidate
import java.lang.reflect.Type

object MoshiFactory : JsonAdapter.Factory {
    override fun create(
        type: Type,
        annotations: MutableSet<out Annotation>,
        moshi: Moshi
    ): JsonAdapter<*>? {
        return when {
            Types.getRawType(type) == QuoteMessageItem::class.java -> {
                QuoteMessageItemJsonAdapter(moshi)
            }
            Types.getRawType(type) == WebClip::class.java -> {
                WebClipJsonAdapter(moshi)
            }
            Types.getRawType(type) == IceCandidate::class.java -> {
                IceCandidateJsonAdapter(moshi)
            }
            Types.getRawType(type) == Message::class.java -> {
                MessageJsonAdapter(moshi)
            }
            Types.getRawType(type) == SignedPreKey::class.java -> {
                SignedPreKeyJsonAdapter(moshi)
            }
            Types.getRawType(type) == SignalKeyRequest::class.java -> {
                SignalKeyRequestJsonAdapter(moshi)
            }
            Types.getRawType(type) == BlazeMessageParam::class.java -> {
                BlazeMessageParamJsonAdapter(moshi)
            }
            Types.getRawType(type) == CodeResponse::class.java -> {
                CodeResponseJsonAdapter(moshi)
            }
            else -> null
        }
    }
}
