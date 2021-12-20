package one.mixin.android.moshi.adaptrer

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.internal.Util
import one.mixin.android.vo.Message
import java.lang.reflect.Constructor
import kotlin.Int
import kotlin.Long
import kotlin.String

class MessageJsonAdapter(
    moshi: Moshi
) : JsonAdapter<Message>() {
    private val options: JsonReader.Options = JsonReader.Options.of(
        "id",
        "conversation_id",
        "user_id",
        "category",
        "content",
        "media_url",
        "media_mime_type",
        "media_size",
        "media_duration",
        "media_width",
        "media_height",
        "media_hash",
        "thumb_image",
        "thumb_url",
        "mediaKey",
        "mediaDigest",
        "mediaStatus",
        "status",
        "created_at",
        "action",
        "participant_id",
        "snapshot_id",
        "hyperlink",
        "name",
        "album_id",
        "sticker_id",
        "shared_user_id",
        "mediaWaveform",
        "media_mine_type",
        "quote_message_id",
        "quote_content",
        "caption"
    )

    private val stringAdapter: JsonAdapter<String> = moshi.adapter(
        String::class.java, emptySet(),
        "id"
    )

    private val nullableStringAdapter: JsonAdapter<String?> = moshi.adapter(
        String::class.java,
        emptySet(), "content"
    )

    private val nullableLongAdapter: JsonAdapter<Long?> = moshi.adapter(
        Long::class.javaObjectType,
        emptySet(), "mediaSize"
    )

    private val nullableIntAdapter: JsonAdapter<Int?> = moshi.adapter(
        Int::class.javaObjectType,
        emptySet(), "mediaWidth"
    )

    private val nullableByteArrayAdapter: JsonAdapter<ByteArray?> =
        moshi.adapter(ByteArray::class.java, emptySet(), "mediaKey")

    @Volatile
    private var constructorRef: Constructor<Message>? = null

    override fun toString(): String = buildString(29) {
        append("GeneratedJsonAdapter(").append("Message").append(')')
    }

    override fun fromJson(reader: JsonReader): Message {
        var id: String? = null
        var conversationId: String? = null
        var userId: String? = null
        var category: String? = null
        var content: String? = null
        var mediaUrl: String? = null
        var mediaMimeType: String? = null
        var mediaSize: Long? = null
        var mediaDuration: String? = null
        var mediaWidth: Int? = null
        var mediaHeight: Int? = null
        var mediaHash: String? = null
        var thumbImage: String? = null
        var thumbUrl: String? = null
        var mediaKey: ByteArray? = null
        var mediaDigest: ByteArray? = null
        var mediaStatus: String? = null
        var status: String? = null
        var createdAt: String? = null
        var action: String? = null
        var participantId: String? = null
        var snapshotId: String? = null
        var hyperlink: String? = null
        var name: String? = null
        var albumId: String? = null
        var stickerId: String? = null
        var sharedUserId: String? = null
        var mediaWaveform: ByteArray? = null
        var mediaMineType: String? = null
        var quoteMessageId: String? = null
        var quoteContent: String? = null
        var caption: String? = null
        var mask0 = -1
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.selectName(options)) {
                0 ->
                    id =
                        stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull("id", "id", reader)
                1 -> conversationId = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "conversationId",
                    "conversation_id",
                    reader
                )
                2 -> userId = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "userId",
                    "user_id", reader
                )
                3 -> category = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "category",
                    "category", reader
                )
                4 -> content = nullableStringAdapter.fromJson(reader)
                5 -> mediaUrl = nullableStringAdapter.fromJson(reader)
                6 -> mediaMimeType = nullableStringAdapter.fromJson(reader)
                7 -> mediaSize = nullableLongAdapter.fromJson(reader)
                8 -> mediaDuration = nullableStringAdapter.fromJson(reader)
                9 -> mediaWidth = nullableIntAdapter.fromJson(reader)
                10 -> mediaHeight = nullableIntAdapter.fromJson(reader)
                11 -> mediaHash = nullableStringAdapter.fromJson(reader)
                12 -> thumbImage = nullableStringAdapter.fromJson(reader)
                13 -> thumbUrl = nullableStringAdapter.fromJson(reader)
                14 -> {
                    mediaKey = nullableByteArrayAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 14).inv()
                    mask0 = mask0 and 0xffffbfff.toInt()
                }
                15 -> {
                    mediaDigest = nullableByteArrayAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 15).inv()
                    mask0 = mask0 and 0xffff7fff.toInt()
                }
                16 -> {
                    mediaStatus = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 16).inv()
                    mask0 = mask0 and 0xfffeffff.toInt()
                }
                17 -> status = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "status",
                    "status", reader
                )
                18 -> createdAt = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "createdAt",
                    "created_at", reader
                )
                19 -> {
                    action = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 19).inv()
                    mask0 = mask0 and 0xfff7ffff.toInt()
                }
                20 -> {
                    participantId = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 20).inv()
                    mask0 = mask0 and 0xffefffff.toInt()
                }
                21 -> {
                    snapshotId = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 21).inv()
                    mask0 = mask0 and 0xffdfffff.toInt()
                }
                22 -> {
                    hyperlink = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 22).inv()
                    mask0 = mask0 and 0xffbfffff.toInt()
                }
                23 -> {
                    name = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 23).inv()
                    mask0 = mask0 and 0xff7fffff.toInt()
                }
                24 -> {
                    albumId = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 24).inv()
                    mask0 = mask0 and 0xfeffffff.toInt()
                }
                25 -> {
                    stickerId = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 25).inv()
                    mask0 = mask0 and 0xfdffffff.toInt()
                }
                26 -> {
                    sharedUserId = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 26).inv()
                    mask0 = mask0 and 0xfbffffff.toInt()
                }
                27 -> {
                    mediaWaveform = nullableByteArrayAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 27).inv()
                    mask0 = mask0 and 0xf7ffffff.toInt()
                }
                28 -> {
                    nullableStringAdapter.fromJson(reader)?.let {
                        mediaMimeType = it
                    }
                    // $mask = $mask and (1 shl 28).inv()
                    mask0 = mask0 and 0xefffffff.toInt()
                }
                29 -> {
                    quoteMessageId = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 29).inv()
                    mask0 = mask0 and 0xdfffffff.toInt()
                }
                30 -> {
                    quoteContent = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 30).inv()
                    mask0 = mask0 and 0xbfffffff.toInt()
                }
                31 -> {
                    caption = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 31).inv()
                    mask0 = mask0 and 0x7fffffff.toInt()
                }
                -1 -> {
                    // Unknown name, skip it.
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()
        if (mask0 == 0x63fff.toInt()) {
            // All parameters with defaults are set, invoke the constructor directly
            return Message(
                id = id ?: throw Util.missingProperty("id", "id", reader),
                conversationId = conversationId ?: throw Util.missingProperty(
                    "conversationId",
                    "conversation_id", reader
                ),
                userId = userId ?: throw Util.missingProperty("userId", "user_id", reader),
                category = category ?: throw Util.missingProperty("category", "category", reader),
                content = content,
                mediaUrl = mediaUrl,
                mediaMimeType = mediaMimeType,
                mediaSize = mediaSize,
                mediaDuration = mediaDuration,
                mediaWidth = mediaWidth,
                mediaHeight = mediaHeight,
                mediaHash = mediaHash,
                thumbImage = thumbImage,
                thumbUrl = thumbUrl,
                mediaKey = mediaKey,
                mediaDigest = mediaDigest,
                mediaStatus = mediaStatus,
                status = status ?: throw Util.missingProperty("status", "status", reader),
                createdAt = createdAt ?: throw Util.missingProperty(
                    "createdAt",
                    "created_at",
                    reader
                ),
                action = action,
                participantId = participantId,
                snapshotId = snapshotId,
                hyperlink = hyperlink,
                name = name,
                albumId = albumId,
                stickerId = stickerId,
                sharedUserId = sharedUserId,
                mediaWaveform = mediaWaveform,
                mediaMineType = mediaMineType,
                quoteMessageId = quoteMessageId,
                quoteContent = quoteContent,
                caption = caption
            )
        } else {
            // Reflectively invoke the synthetic defaults constructor
            @Suppress("UNCHECKED_CAST")
            val localConstructor: Constructor<Message> =
                this.constructorRef ?: Message::class.java.getDeclaredConstructor(
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    Long::class.javaObjectType,
                    String::class.java,
                    Int::class.javaObjectType,
                    Int::class.javaObjectType,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    ByteArray::class.java,
                    ByteArray::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    ByteArray::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    Int::class.javaPrimitiveType,
                    Util.DEFAULT_CONSTRUCTOR_MARKER
                ).also {
                    this.constructorRef = it
                }
            return localConstructor.newInstance(
                id ?: throw Util.missingProperty("id", "id", reader),
                conversationId ?: throw Util.missingProperty(
                    "conversationId",
                    "conversation_id",
                    reader
                ),
                userId ?: throw Util.missingProperty("userId", "user_id", reader),
                category ?: throw Util.missingProperty("category", "category", reader),
                content,
                mediaUrl,
                mediaMimeType,
                mediaSize,
                mediaDuration,
                mediaWidth,
                mediaHeight,
                mediaHash,
                thumbImage,
                thumbUrl,
                mediaKey,
                mediaDigest,
                mediaStatus,
                status ?: throw Util.missingProperty("status", "status", reader),
                createdAt ?: throw Util.missingProperty("createdAt", "created_at", reader),
                action,
                participantId,
                snapshotId,
                hyperlink,
                name,
                albumId,
                stickerId,
                sharedUserId,
                mediaWaveform,
                mediaMineType,
                quoteMessageId,
                quoteContent,
                caption,
                mask0,
                /* DefaultConstructorMarker */ null
            )
        }
    }

    override fun toJson(writer: JsonWriter, value_: Message?) {
        if (value_ == null) {
            throw NullPointerException(
                "value_ was null! Wrap in .nullSafe() to write nullable " +
                    "values."
            )
        }
        writer.beginObject()
        writer.name("id")
        stringAdapter.toJson(writer, value_.id)
        writer.name("conversation_id")
        stringAdapter.toJson(writer, value_.conversationId)
        writer.name("user_id")
        stringAdapter.toJson(writer, value_.userId)
        writer.name("category")
        stringAdapter.toJson(writer, value_.category)
        writer.name("content")
        nullableStringAdapter.toJson(writer, value_.content)
        writer.name("media_url")
        nullableStringAdapter.toJson(writer, value_.mediaUrl)
        writer.name("media_mime_type")
        nullableStringAdapter.toJson(writer, value_.mediaMimeType)
        writer.name("media_size")
        nullableLongAdapter.toJson(writer, value_.mediaSize)
        writer.name("media_duration")
        nullableStringAdapter.toJson(writer, value_.mediaDuration)
        writer.name("media_width")
        nullableIntAdapter.toJson(writer, value_.mediaWidth)
        writer.name("media_height")
        nullableIntAdapter.toJson(writer, value_.mediaHeight)
        writer.name("media_hash")
        nullableStringAdapter.toJson(writer, value_.mediaHash)
        writer.name("thumb_image")
        nullableStringAdapter.toJson(writer, value_.thumbImage)
        writer.name("thumb_url")
        nullableStringAdapter.toJson(writer, value_.thumbUrl)
        writer.name("mediaKey")
        nullableByteArrayAdapter.toJson(writer, value_.mediaKey)
        writer.name("mediaDigest")
        nullableByteArrayAdapter.toJson(writer, value_.mediaDigest)
        writer.name("mediaStatus")
        nullableStringAdapter.toJson(writer, value_.mediaStatus)
        writer.name("status")
        stringAdapter.toJson(writer, value_.status)
        writer.name("created_at")
        stringAdapter.toJson(writer, value_.createdAt)
        writer.name("action")
        nullableStringAdapter.toJson(writer, value_.action)
        writer.name("participant_id")
        nullableStringAdapter.toJson(writer, value_.participantId)
        writer.name("snapshot_id")
        nullableStringAdapter.toJson(writer, value_.snapshotId)
        writer.name("hyperlink")
        nullableStringAdapter.toJson(writer, value_.hyperlink)
        writer.name("name")
        nullableStringAdapter.toJson(writer, value_.name)
        writer.name("album_id")
        nullableStringAdapter.toJson(writer, value_.stickerId)
        writer.name("sticker_id")
        nullableStringAdapter.toJson(writer, value_.stickerId)
        writer.name("shared_user_id")
        nullableStringAdapter.toJson(writer, value_.sharedUserId)
        writer.name("mediaWaveform")
        nullableByteArrayAdapter.toJson(writer, value_.mediaWaveform)
        writer.name("quote_message_id")
        nullableStringAdapter.toJson(writer, value_.quoteMessageId)
        writer.name("quote_content")
        nullableStringAdapter.toJson(writer, value_.quoteContent)
        writer.name("caption")
        nullableStringAdapter.toJson(writer, value_.caption)
        writer.endObject()
    }
}
