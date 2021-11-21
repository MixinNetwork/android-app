package one.mixin.android.moshi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import one.mixin.android.moshi.adaptrer.QuoteMessageItemJsonAdapter
import one.mixin.android.moshi.adaptrer.WebClipJsonAdapter
import one.mixin.android.ui.web.WebClip
import one.mixin.android.vo.QuoteMessageItem
import java.lang.reflect.Type

object MoshiFactory : JsonAdapter.Factory {
    override fun create(
        type: Type,
        annotations: MutableSet<out Annotation>,
        moshi: Moshi
    ): JsonAdapter<*>? {
        if (Types.getRawType(type) == QuoteMessageItem::class.java) {
            return QuoteMessageItemJsonAdapter(moshi)
        } else if (Types.getRawType(type) == WebClip::class.java) {
            return WebClipJsonAdapter(moshi)
        }
        return null
    }
}
