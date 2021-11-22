package one.mixin.android.moshi.adaptrer

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.internal.Util
import one.mixin.android.api.SignedPreKey
import kotlin.Int
import kotlin.String

class SignedPreKeyJsonAdapter(
    moshi: Moshi
) : JsonAdapter<SignedPreKey>() {
    private val options: JsonReader.Options = JsonReader.Options.of("keyId", "pubKey", "signature")

    private val intAdapter: JsonAdapter<Int> = moshi.adapter(Int::class.java, emptySet(), "keyId")

    private val stringAdapter: JsonAdapter<String> = moshi.adapter(
        String::class.java, emptySet(),
        "signature"
    )

    override fun toString(): String = buildString(34) {
        append("GeneratedJsonAdapter(").append("SignedPreKey").append(')')
    }

    override fun fromJson(reader: JsonReader): SignedPreKey {
        var keyId: Int? = null
        var pubKey: String? = null
        var signature: String? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.selectName(options)) {
                0 -> keyId = intAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "keyId", "keyId",
                    reader
                )
                1 -> pubKey = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "pubKey",
                    "pubKey", reader
                )
                2 -> signature = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "signature",
                    "signature", reader
                )
                -1 -> {
                    // Unknown name, skip it.
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()
        return SignedPreKey(
            keyId = keyId ?: throw Util.missingProperty("keyId", "keyId", reader),
            pubKey = pubKey ?: throw Util.unexpectedNull(
                "pubKey",
                "pubKey", reader
            ),
            signature = signature ?: throw Util.missingProperty("signature", "signature", reader)
        )
    }

    override fun toJson(writer: JsonWriter, value_: SignedPreKey?) {
        if (value_ == null) {
            throw NullPointerException("value_ was null! Wrap in .nullSafe() to write nullable values.")
        }
        writer.beginObject()
        writer.name("keyId")
        intAdapter.toJson(writer, value_.keyId)
        writer.name("pubKey")
        stringAdapter.toJson(writer, value_.pubKey)
        writer.name("signature")
        stringAdapter.toJson(writer, value_.signature)
        writer.endObject()
    }
}
