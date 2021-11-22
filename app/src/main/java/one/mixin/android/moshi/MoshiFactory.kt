package one.mixin.android.moshi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import one.mixin.android.moshi.adaptrer.IceCandidateJsonAdapter
import one.mixin.android.moshi.adaptrer.QuoteMessageItemJsonAdapter
import one.mixin.android.moshi.adaptrer.WebClipJsonAdapter
import one.mixin.android.ui.web.WebClip
import one.mixin.android.vo.QuoteMessageItem
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
            else -> null
        }
    }
}
