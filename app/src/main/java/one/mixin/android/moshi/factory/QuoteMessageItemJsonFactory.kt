package one.mixin.android.moshi.factory

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import one.mixin.android.moshi.adaptrer.QuoteMessageItemJsonAdapter
import java.lang.reflect.Type

class QuoteMessageItemJsonFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): QuoteMessageItemJsonAdapter {
        return QuoteMessageItemJsonAdapter(moshi)
    }
}
