package one.mixin.android.moshi.adapter

import android.graphics.Bitmap
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.decodeBitmapFromBase64

object BitmapJsonAdapter {
    @ToJson
    fun toJson(value: Bitmap): String? {
        return value.base64Encode(Bitmap.CompressFormat.PNG)
    }

    @FromJson
    fun fromJson(value: String): Bitmap {
        return decodeBitmapFromBase64(value)
    }
}
