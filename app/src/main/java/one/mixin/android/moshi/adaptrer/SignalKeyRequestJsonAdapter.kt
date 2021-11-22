package one.mixin.android.moshi.adaptrer

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import one.mixin.android.api.OneTimePreKey
import one.mixin.android.api.SignedPreKey
import one.mixin.android.api.request.SignalKeyRequest
import java.util.ArrayList
import kotlin.String

class SignalKeyRequestJsonAdapter(
    moshi: Moshi
) : JsonAdapter<SignalKeyRequest>() {
    private val stringAdapter: JsonAdapter<String> = moshi.adapter(
        String::class.java, emptySet(),
        "identityKey"
    )

    private val signedPreKeyAdapter: JsonAdapter<SignedPreKey> =
        moshi.adapter(SignedPreKey::class.java, emptySet(), "signedPreKey")

    private val arrayListOfOneTimePreKeyAdapter: JsonAdapter<ArrayList<OneTimePreKey>> =
        moshi.adapter(
            Types.newParameterizedType(ArrayList::class.java, OneTimePreKey::class.java),
            emptySet(), "oneTimePreKeys"
        )

    override fun toString(): String = buildString(38) {
        append("GeneratedJsonAdapter(").append("SignalKeyRequest").append(')')
    }

    override fun fromJson(reader: JsonReader): SignalKeyRequest {
        throw JsonDataException("Does not allow deserialization")
    }

    override fun toJson(writer: JsonWriter, value_: SignalKeyRequest?) {
        if (value_ == null) {
            throw NullPointerException("value_ was null! Wrap in .nullSafe() to write nullable values.")
        }
        writer.beginObject()
        writer.name("identity_key")
        stringAdapter.toJson(writer, value_.identityKey)
        writer.name("signed_pre_key")
        signedPreKeyAdapter.toJson(writer, value_.signedPreKey)
        writer.name("one_time_pre_keys")
        arrayListOfOneTimePreKeyAdapter.toJson(writer, value_.oneTimePreKeys)
        writer.endObject()
    }
}
