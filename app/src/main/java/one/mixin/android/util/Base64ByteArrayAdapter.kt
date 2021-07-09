package one.mixin.android.util

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import one.mixin.android.crypto.Base64
import one.mixin.android.extension.base64Encode
import java.io.IOException

class Base64ByteArrayAdapter : JsonAdapter<ByteArray>() {
    @Throws(IOException::class)
    override fun fromJson(reader: JsonReader): ByteArray {
        return Base64.decode(reader.nextString())
    }

    @Throws(IOException::class)
    override fun toJson(writer: JsonWriter, value: ByteArray?) {
        writer.value(value?.base64Encode())
    }
}
