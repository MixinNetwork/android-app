package one.mixin.android.moshi.adaptrer

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.`internal`.Util
import one.mixin.android.api.request.SignalKeyRequest
import one.mixin.android.websocket.BlazeMessageParam
import one.mixin.android.websocket.BlazeMessageParamSession
import one.mixin.android.websocket.BlazeSignalKeyMessage
import java.lang.reflect.Constructor
import java.util.ArrayList
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.emptySet
import kotlin.jvm.Volatile
import kotlin.text.buildString

class BlazeMessageParamJsonAdapter(
    moshi: Moshi
) : JsonAdapter<BlazeMessageParam>() {
    private val options: JsonReader.Options = JsonReader.Options.of(
        "conversation_id", "recipient_id",
        "message_id", "category", "data", "status", "recipients", "keys", "messages",
        "quote_message_id", "session_id", "representative_id", "conversation_checksum", "mentions",
        "jsep", "candidate", "track_id", "recipient_ids", "offset", "silent"
    )

    private val nullableStringAdapter: JsonAdapter<String?> = moshi.adapter(
        String::class.java,
        emptySet(), "conversation_id"
    )

    private val nullableArrayListOfBlazeMessageParamSessionAdapter:
        JsonAdapter<ArrayList<BlazeMessageParamSession>?> =
            moshi.adapter(
                Types.newParameterizedType(
                    ArrayList::class.java,
                    BlazeMessageParamSession::class.java
                ),
                emptySet(), "recipients"
            )

    private val nullableSignalKeyRequestAdapter: JsonAdapter<SignalKeyRequest?> =
        moshi.adapter(SignalKeyRequest::class.java, emptySet(), "keys")

    private val nullableListOfBlazeSignalKeyMessageAdapter: JsonAdapter<List<BlazeSignalKeyMessage>?> =
        moshi.adapter(
            Types.newParameterizedType(
                List::class.java,
                BlazeSignalKeyMessage::class.java
            ),
            emptySet(), "messages"
        )

    private val nullableListOfStringAdapter: JsonAdapter<List<String>?> =
        moshi.adapter(
            Types.newParameterizedType(List::class.java, String::class.java), emptySet(),
            "mentions"
        )

    private val nullableBooleanAdapter: JsonAdapter<Boolean?> =
        moshi.adapter(Boolean::class.javaObjectType, emptySet(), "silent")

    @Volatile
    private var constructorRef: Constructor<BlazeMessageParam>? = null

    public override fun toString(): String = buildString(39) {
        append("GeneratedJsonAdapter(").append("BlazeMessageParam").append(')')
    }

    public override fun fromJson(reader: JsonReader): BlazeMessageParam {
        var conversation_id: String? = null
        var recipient_id: String? = null
        var message_id: String? = null
        var category: String? = null
        var data_: String? = null
        var status: String? = null
        var recipients: ArrayList<BlazeMessageParamSession>? = null
        var keys: SignalKeyRequest? = null
        var messages: List<BlazeSignalKeyMessage>? = null
        var quote_message_id: String? = null
        var session_id: String? = null
        var representative_id: String? = null
        var conversation_checksum: String? = null
        var mentions: List<String>? = null
        var jsep: String? = null
        var candidate: String? = null
        var track_id: String? = null
        var recipient_ids: List<String>? = null
        var offset: String? = null
        var silent: Boolean? = null
        var mask0 = -1
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.selectName(options)) {
                0 -> {
                    conversation_id = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 0).inv()
                    mask0 = mask0 and 0xfffffffe.toInt()
                }
                1 -> {
                    recipient_id = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 1).inv()
                    mask0 = mask0 and 0xfffffffd.toInt()
                }
                2 -> {
                    message_id = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 2).inv()
                    mask0 = mask0 and 0xfffffffb.toInt()
                }
                3 -> {
                    category = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 3).inv()
                    mask0 = mask0 and 0xfffffff7.toInt()
                }
                4 -> {
                    data_ = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 4).inv()
                    mask0 = mask0 and 0xffffffef.toInt()
                }
                5 -> {
                    status = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 5).inv()
                    mask0 = mask0 and 0xffffffdf.toInt()
                }
                6 -> {
                    recipients = nullableArrayListOfBlazeMessageParamSessionAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 6).inv()
                    mask0 = mask0 and 0xffffffbf.toInt()
                }
                7 -> {
                    keys = nullableSignalKeyRequestAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 7).inv()
                    mask0 = mask0 and 0xffffff7f.toInt()
                }
                8 -> {
                    messages = nullableListOfBlazeSignalKeyMessageAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 8).inv()
                    mask0 = mask0 and 0xfffffeff.toInt()
                }
                9 -> {
                    quote_message_id = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 9).inv()
                    mask0 = mask0 and 0xfffffdff.toInt()
                }
                10 -> {
                    session_id = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 10).inv()
                    mask0 = mask0 and 0xfffffbff.toInt()
                }
                11 -> {
                    representative_id = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 11).inv()
                    mask0 = mask0 and 0xfffff7ff.toInt()
                }
                12 -> {
                    conversation_checksum = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 12).inv()
                    mask0 = mask0 and 0xffffefff.toInt()
                }
                13 -> {
                    mentions = nullableListOfStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 13).inv()
                    mask0 = mask0 and 0xffffdfff.toInt()
                }
                14 -> {
                    jsep = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 14).inv()
                    mask0 = mask0 and 0xffffbfff.toInt()
                }
                15 -> {
                    candidate = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 15).inv()
                    mask0 = mask0 and 0xffff7fff.toInt()
                }
                16 -> {
                    track_id = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 16).inv()
                    mask0 = mask0 and 0xfffeffff.toInt()
                }
                17 -> {
                    recipient_ids = nullableListOfStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 17).inv()
                    mask0 = mask0 and 0xfffdffff.toInt()
                }
                18 -> {
                    offset = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 18).inv()
                    mask0 = mask0 and 0xfffbffff.toInt()
                }
                19 -> {
                    silent = nullableBooleanAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 19).inv()
                    mask0 = mask0 and 0xfff7ffff.toInt()
                }
                -1 -> {
                    // Unknown name, skip it.
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()
        if (mask0 == 0xfff00000.toInt()) {
            // All parameters with defaults are set, invoke the constructor directly
            return BlazeMessageParam(
                conversation_id = conversation_id,
                recipient_id = recipient_id,
                message_id = message_id,
                category = category,
                `data` = data_,
                status = status,
                recipients = recipients,
                keys = keys,
                messages = messages,
                quote_message_id = quote_message_id,
                session_id = session_id,
                representative_id = representative_id,
                conversation_checksum = conversation_checksum,
                mentions = mentions,
                jsep = jsep,
                candidate = candidate,
                track_id = track_id,
                recipient_ids = recipient_ids,
                offset = offset,
                silent = silent
            )
        } else {
            // Reflectively invoke the synthetic defaults constructor
            @Suppress("UNCHECKED_CAST")
            val localConstructor: Constructor<BlazeMessageParam> =
                this.constructorRef ?: BlazeMessageParam::class.java.getDeclaredConstructor(
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    ArrayList::class.java,
                    SignalKeyRequest::class.java,
                    List::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    List::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    List::class.java,
                    String::class.java,
                    Boolean::class.javaObjectType,
                    Int::class.javaPrimitiveType,
                    Util.DEFAULT_CONSTRUCTOR_MARKER
                ).also {
                    this.constructorRef = it
                }
            return localConstructor.newInstance(
                conversation_id,
                recipient_id,
                message_id,
                category,
                data_,
                status,
                recipients,
                keys,
                messages,
                quote_message_id,
                session_id,
                representative_id,
                conversation_checksum,
                mentions,
                jsep,
                candidate,
                track_id,
                recipient_ids,
                offset,
                silent,
                mask0,
                /* DefaultConstructorMarker */ null
            )
        }
    }

    override fun toJson(writer: JsonWriter, value_: BlazeMessageParam?) {
        if (value_ == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        if (value_.conversation_id != null) {
            writer.name("conversation_id")
            nullableStringAdapter.toJson(writer, value_.conversation_id)
        }
        writer.name("recipient_id")
        nullableStringAdapter.toJson(writer, value_.recipient_id)
        writer.name("message_id")
        nullableStringAdapter.toJson(writer, value_.message_id)
        writer.name("category")
        nullableStringAdapter.toJson(writer, value_.category)
        writer.name("data")
        nullableStringAdapter.toJson(writer, value_.`data`)
        writer.name("status")
        nullableStringAdapter.toJson(writer, value_.status)
        writer.name("recipients")
        nullableArrayListOfBlazeMessageParamSessionAdapter.toJson(writer, value_.recipients)
        if (value_.keys != null) {
            writer.name("keys")
            nullableSignalKeyRequestAdapter.toJson(writer, value_.keys)
        }
        if (value_.messages != null) {
            writer.name("messages")
            nullableListOfBlazeSignalKeyMessageAdapter.toJson(writer, value_.messages)
        }
        writer.name("quote_message_id")
        nullableStringAdapter.toJson(writer, value_.quote_message_id)
        writer.name("session_id")
        nullableStringAdapter.toJson(writer, value_.session_id)
        writer.name("representative_id")
        nullableStringAdapter.toJson(writer, value_.representative_id)
        writer.name("conversation_checksum")
        nullableStringAdapter.toJson(writer, value_.conversation_checksum)
        if (value_.mentions != null) {
            writer.name("mentions")
            nullableListOfStringAdapter.toJson(writer, value_.mentions)
        }
        writer.name("jsep")
        nullableStringAdapter.toJson(writer, value_.jsep)
        writer.name("candidate")
        nullableStringAdapter.toJson(writer, value_.candidate)
        writer.name("track_id")
        nullableStringAdapter.toJson(writer, value_.track_id)
        if (value_.mentions != null) {
            writer.name("recipient_ids")
            nullableListOfStringAdapter.toJson(writer, value_.recipient_ids)
        }
        writer.name("offset")
        nullableStringAdapter.toJson(writer, value_.offset)
        writer.name("silent")
        nullableBooleanAdapter.toJson(writer, value_.silent)
        writer.endObject()
    }
}
