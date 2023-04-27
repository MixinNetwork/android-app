package one.mixin.android.util

import android.graphics.Bitmap
import android.util.LruCache
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import one.mixin.android.crypto.Base64
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.decodeBitmapFromBase64
import one.mixin.android.extension.isUUID
import timber.log.Timber
import java.lang.reflect.Type


object GsonHelper {
    val customGson: Gson = GsonBuilder()
        .registerTypeHierarchyAdapter(ByteArray::class.java, ByteArrayToBase64TypeAdapter())
        .registerTypeHierarchyAdapter(Bitmap::class.java, BitmapToBase64TypeAdapter())
        .registerTypeHierarchyAdapter(String::class.java, StringTypeAdapter())
        .create()

    private class BitmapToBase64TypeAdapter : JsonSerializer<Bitmap>, JsonDeserializer<Bitmap> {
        override fun serialize(
            src: Bitmap,
            typeOfSrc: Type,
            context: JsonSerializationContext,
        ): JsonElement {
            return JsonPrimitive(src.base64Encode(Bitmap.CompressFormat.PNG))
        }

        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext,
        ): Bitmap {
            return decodeBitmapFromBase64(json.asString)
        }
    }

    private class ByteArrayToBase64TypeAdapter :
        JsonSerializer<ByteArray>,
        JsonDeserializer<ByteArray> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext,
        ): ByteArray {
            return Base64.decode(json.asString)
        }

        override fun serialize(
            src: ByteArray,
            typeOfSrc: Type,
            context: JsonSerializationContext,
        ): JsonElement {
            return JsonPrimitive(src.base64Encode())
        }
    }

    private class StringTypeAdapter :
        JsonSerializer<String>,
        JsonDeserializer<String>{
        val cache = StringCache(1024 * 1024 * 10)
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext?,
        ): String {
            return cache.getCachedString(json.asString)
        }

        override fun serialize(
            src: String,
            typeOfSrc: Type,
            context: JsonSerializationContext?,
        ): JsonElement {
            return JsonPrimitive(src)
        }

    }

    class StringCache(maxSize: Int) : LruCache<Int, String>(maxSize) {
        fun getCachedString(value: String): String {
            val result = get(value.hashCode())
            if (result != null) {
                return result
            }
            if (value.isUUID()) {
                value.intern()
                put(value.hashCode(), value)
            }
            return value
        }
    }

}
