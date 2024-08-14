package one.mixin.android.util

import android.graphics.Bitmap
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
import one.mixin.android.vo.Plan
import one.mixin.android.vo.WithdrawalMemoPossibility
import java.lang.reflect.Type

object GsonHelper {
    val customGson: Gson =
        GsonBuilder()
            .registerTypeHierarchyAdapter(ByteArray::class.java, ByteArrayToBase64TypeAdapter())
            .registerTypeHierarchyAdapter(Bitmap::class.java, BitmapToBase64TypeAdapter())
            .registerTypeHierarchyAdapter(WithdrawalMemoPossibility::class.java, WithdrawalMemoPossibilityAdapter())
            .registerTypeHierarchyAdapter(Plan::class.java, PlanAdapter())
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

    class PlanAdapter : JsonDeserializer<Plan?> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?,
        ): Plan? {
            return when (json?.asString) {
                Plan.None.value -> Plan.None
                Plan.STANDARD.value -> Plan.STANDARD
                Plan.BASIC.value -> Plan.BASIC
                Plan.PREMIUM.value -> Plan.PREMIUM
                else -> null
            }
        }
    }


    class WithdrawalMemoPossibilityAdapter : JsonDeserializer<WithdrawalMemoPossibility?> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?,
        ): WithdrawalMemoPossibility? {
            return when (json?.asString) {
                WithdrawalMemoPossibility.NEGATIVE.value -> WithdrawalMemoPossibility.NEGATIVE
                WithdrawalMemoPossibility.POSSIBLE.value -> WithdrawalMemoPossibility.POSSIBLE
                WithdrawalMemoPossibility.POSITIVE.value -> WithdrawalMemoPossibility.POSITIVE
                else -> null
            }
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
}
