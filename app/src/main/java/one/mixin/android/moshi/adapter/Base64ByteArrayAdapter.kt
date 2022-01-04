package one.mixin.android.moshi.adapter

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import one.mixin.android.crypto.Base64
import one.mixin.android.extension.base64Encode
import java.io.IOException

class Base64ByteArrayAdapter : JsonAdapter<ByteArray>() {
    @Throws(IOException::class)
    override fun fromJson(reader: JsonReader): ByteArray? {
        return if (reader.peek() == JsonReader.Token.NULL) {
            reader.nextNull<ByteArray?>()
        } else {
            Base64.decode(reader.nextString())
        }
    }

    @Throws(IOException::class)
    override fun toJson(writer: JsonWriter, value: ByteArray?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.base64Encode())
        }
    }
}
