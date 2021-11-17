package one.mixin.android.moshi.adaptrer

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.internal.Util
import one.mixin.android.vo.MentionUser

class MentionUserJsonAdapter(
) : JsonAdapter<MentionUser>() {
    private val options: JsonReader.Options = JsonReader.Options.of("identity_number", "identityNumber", "fullName", "full_name")

    override fun toString(): String = buildString(33) {
        append("GeneratedJsonAdapter(").append("MentionUser").append(')')
    }

    override fun fromJson(reader: JsonReader): MentionUser {
        var identityNumber: String? = null
        var fullName: String? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.selectName(options)) {
                0, 1 -> reader.nextString()?.let {
                    identityNumber = it
                }
                2, 3 -> reader.nextString()?.let {
                    fullName = it
                }
                -1 -> {
                    // Unknown name, skip it.
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()
        return MentionUser(
            identityNumber = identityNumber ?: throw Util.missingProperty(
                "identityNumber",
                "identityNumber", reader
            ),
            fullName = fullName ?: throw Util.missingProperty("fullName", "fullName", reader)
        )
    }

    override fun toJson(writer: JsonWriter, value_: MentionUser?) {
        if (value_ == null) {
            throw NullPointerException("value_ was null! Wrap in .nullSafe() to write nullable values.")
        }
        writer.beginObject()
        writer.name("identityNumber")
        writer.value(value_.identityNumber)
        writer.name("fullName")
        writer.value(value_.fullName)
        writer.endObject()
    }
}
