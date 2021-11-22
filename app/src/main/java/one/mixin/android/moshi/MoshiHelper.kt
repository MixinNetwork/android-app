package one.mixin.android.moshi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import one.mixin.android.moshi.adaptrer.BitmapJsonAdapter
import one.mixin.android.moshi.adaptrer.MentionUserJsonAdapter
import one.mixin.android.moshi.adaptrer.MoshiArrayListJsonAdapter
import one.mixin.android.util.Base64ByteArrayAdapter
import one.mixin.android.vo.MentionUser
import java.lang.reflect.Type

object MoshiHelper {
    val moshi: Moshi = Moshi.Builder()
        .add(MoshiFactory)
        .add(MentionUser::class.java, MentionUserJsonAdapter())
        .add(ByteArray::class.java, Base64ByteArrayAdapter())
        .add(BitmapJsonAdapter)
        .add(MoshiArrayListJsonAdapter.FACTORY)
        .add(KotlinJsonAdapterFactory())
        .build()

    fun <T> getTypeListAdapter(type: Type): JsonAdapter<T> {
        val listType = Types.newParameterizedType(List::class.java, type)
        return moshi.adapter(listType)
    }

    fun <T> getTypeAdapter(type: Type): JsonAdapter<T> {
        return moshi.adapter(type)
    }
}
