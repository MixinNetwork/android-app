package one.mixin.android.util

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import one.mixin.android.moshi.adaptrer.MentionUserJsonAdapter
import one.mixin.android.moshi.adaptrer.QuoteMessageItemJsonAdapter
import one.mixin.android.vo.MentionUser
import java.lang.reflect.Type

object MoshiHelper {
    private val moshi = Moshi.Builder()
        .add(MentionUser::class.java, MentionUserJsonAdapter())
        .add(ByteArray::class.java, Base64ByteArrayAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    fun <T> getTypeListAdapter(type: Type): JsonAdapter<T> {
        val listType = Types.newParameterizedType(List::class.java, type)
        return moshi.adapter(listType)
    }

    fun <T> getTypeAdapter(type: Type): JsonAdapter<T> {
        return moshi.adapter(type)
    }

    fun getQuoteMessageItemJsonAdapter(): QuoteMessageItemJsonAdapter {
        return QuoteMessageItemJsonAdapter(moshi)
    }
}
