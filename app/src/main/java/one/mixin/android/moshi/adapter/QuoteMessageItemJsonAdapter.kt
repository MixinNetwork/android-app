package one.mixin.android.moshi.adapter

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.internal.Util
import one.mixin.android.vo.QuoteMessageItem
import java.lang.reflect.Constructor
import kotlin.Int
import kotlin.Long
import kotlin.String

class QuoteMessageItemJsonAdapter(
    moshi: Moshi
) : JsonAdapter<QuoteMessageItem>() {
    private val options: JsonReader.Options = JsonReader.Options.of(
        "message_id",
        "messageId",
        "conversation_id",
        "conversationId",
        "user_id",
        "userId",
        "user_full_name",
        "userFullName",
        "user_identity_number",
        "userIdentityNumber",
        "type",
        "content",
        "created_at",
        "createdAt",
        "status",
        "media_status",
        "mediaStatus",
        "user_avatar_url",
        "userAvatarUrl",
        "media_name",
        "mediaName",
        "media_mime_type",
        "mediaMimeType",
        "media_size",
        "mediaSize",
        "media_width",
        "mediaWidth",
        "media_height",
        "mediaHeight",
        "thumb_image",
        "thumbImage",
        "thumb_url",
        "thumbUrl",
        "media_url",
        "mediaUrl",
        "media_duration",
        "mediaDuration",
        "asset_url",
        "assetUrl",
        "asset_height",
        "assetHeight",
        "asset_width",
        "assetWidth",
        "sticker_id",
        "stickerId",
        "app_id",
        "appId",
        "shared_user_id",
        "sharedUserId",
        "shared_user_full_name",
        "sharedUserFullName",
        "shared_user_identity_number",
        "sharedUserIdentityNumber",
        "shared_user_avatar_url",
        "sharedUserAvatarUrl",
        "mentions"
    )

    private val stringAdapter: JsonAdapter<String> = moshi.adapter(
        String::class.java, emptySet(),
        "messageId"
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

    @Volatile
    private var constructorRef: Constructor<QuoteMessageItem>? = null

    override fun toString(): String = buildString(43) {
        append("GeneratedJsonAdapter(").append("SnakeQuoteMessageItem").append(')')
    }

    override fun fromJson(reader: JsonReader): QuoteMessageItem {
        var messageId: String? = null
        var conversationId: String? = null
        var userId: String? = null
        var userFullName: String? = null
        var userIdentityNumber: String? = null
        var type: String? = null
        var content: String? = null
        var createdAt: String? = null
        var status: String? = null
        var mediaStatus: String? = null
        var userAvatarUrl: String? = null
        var mediaName: String? = null
        var mediaMimeType: String? = null
        var mediaSize: Long? = null
        var mediaWidth: Int? = null
        var mediaHeight: Int? = null
        var thumbImage: String? = null
        var thumbUrl: String? = null
        var mediaUrl: String? = null
        var mediaDuration: String? = null
        var assetUrl: String? = null
        var assetHeight: Int? = null
        var assetWidth: Int? = null
        var stickerId: String? = null
        var appId: String? = null
        var sharedUserId: String? = null
        var sharedUserFullName: String? = null
        var sharedUserIdentityNumber: String? = null
        var sharedUserAvatarUrl: String? = null
        var mentions: String? = null
        var mask0 = -1
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.selectName(options)) {
                0, 1 -> nullableStringAdapter.fromJson(reader)?.let {
                    messageId = it
                }
                2, 3 -> nullableStringAdapter.fromJson(reader)?.let { conversationId = it }
                4, 5 -> nullableStringAdapter.fromJson(reader)?.let { userId = it }
                6, 7 -> nullableStringAdapter.fromJson(reader)?.let { userFullName = it }
                8, 9 -> nullableStringAdapter.fromJson(reader)?.let { userIdentityNumber = it }
                10 -> type = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "type", "type",
                    reader
                )
                11 -> content = nullableStringAdapter.fromJson(reader)
                12, 13 -> nullableStringAdapter.fromJson(reader)?.let { createdAt = it }
                14 -> status = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "status",
                    "status", reader
                )
                15, 16 -> nullableStringAdapter.fromJson(reader)?.let { mediaStatus = it }
                17, 18 -> nullableStringAdapter.fromJson(reader)?.let { userAvatarUrl = it }
                19, 20 -> nullableStringAdapter.fromJson(reader)?.let { mediaName = it }
                21, 22 -> nullableStringAdapter.fromJson(reader)?.let { mediaMimeType = it }
                23, 24 -> nullableLongAdapter.fromJson(reader)?.let { mediaSize = it }
                25, 26 -> nullableIntAdapter.fromJson(reader)?.let { mediaWidth = it }
                27, 28 -> nullableIntAdapter.fromJson(reader)?.let { mediaHeight = it }
                29, 30 -> nullableStringAdapter.fromJson(reader)?.let { thumbImage = it }
                31, 32 -> nullableStringAdapter.fromJson(reader)?.let { thumbUrl = it }
                33, 34 -> nullableStringAdapter.fromJson(reader)?.let { mediaUrl = it }
                35, 36 -> nullableStringAdapter.fromJson(reader)?.let { mediaDuration = it }
                37, 38 -> nullableStringAdapter.fromJson(reader)?.let { assetUrl = it }
                39, 40 -> nullableIntAdapter.fromJson(reader)?.let { assetHeight = it }
                41, 42 -> nullableIntAdapter.fromJson(reader)?.let { assetWidth = it }
                43, 44 -> nullableStringAdapter.fromJson(reader)?.let { stickerId = it }
                45, 46 -> nullableStringAdapter.fromJson(reader)?.let { appId = it }
                47, 48 -> {
                    nullableStringAdapter.fromJson(reader)?.let { sharedUserId = it }
                    // $mask = $mask and (1 shl 25).inv()
                    mask0 = mask0 and 0xfdffffff.toInt()
                }
                49, 50 -> {
                    nullableStringAdapter.fromJson(reader)?.let { sharedUserFullName = it }
                    // $mask = $mask and (1 shl 26).inv()
                    mask0 = mask0 and 0xfbffffff.toInt()
                }
                51, 52 -> {
                    nullableStringAdapter.fromJson(reader)?.let { sharedUserIdentityNumber = it }
                    // $mask = $mask and (1 shl 27).inv()
                    mask0 = mask0 and 0xf7ffffff.toInt()
                }
                53, 54 -> {
                    nullableStringAdapter.fromJson(reader)?.let { sharedUserAvatarUrl = it }
                    // $mask = $mask and (1 shl 28).inv()
                    mask0 = mask0 and 0xefffffff.toInt()
                }
                55 -> {
                    mentions = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 29).inv()
                    mask0 = mask0 and 0xdfffffff.toInt()
                }
                -1 -> {
                    // Unknown name, skip it.
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()
        if (mask0 == 0xc1ffffff.toInt()) {
            // All parameters with defaults are set, invoke the constructor directly
            return QuoteMessageItem(
                messageId = messageId ?: throw Util.missingProperty(
                    "messageId",
                    "message_id",
                    reader
                ),
                conversationId = conversationId ?: throw Util.missingProperty(
                    "conversationId",
                    "conversation_id", reader
                ),
                userId = userId ?: throw Util.missingProperty("userId", "user_id", reader),
                userFullName = userFullName ?: throw Util.missingProperty(
                    "userFullName",
                    "user_full_name", reader
                ),
                userIdentityNumber = userIdentityNumber
                    ?: throw Util.missingProperty(
                        "userIdentityNumber",
                        "user_identity_number",
                        reader
                    ),
                type = type ?: throw Util.missingProperty("type", "type", reader),
                content = content,
                createdAt = createdAt ?: throw Util.missingProperty(
                    "createdAt",
                    "created_at",
                    reader
                ),
                status = status ?: throw Util.missingProperty("status", "status", reader),
                mediaStatus = mediaStatus,
                userAvatarUrl = userAvatarUrl,
                mediaName = mediaName,
                mediaMimeType = mediaMimeType,
                mediaSize = mediaSize,
                mediaWidth = mediaWidth,
                mediaHeight = mediaHeight,
                thumbImage = thumbImage,
                thumbUrl = thumbUrl,
                mediaUrl = mediaUrl,
                mediaDuration = mediaDuration,
                assetUrl = assetUrl,
                assetHeight = assetHeight,
                assetWidth = assetWidth,
                stickerId = stickerId,
                appId = appId,
                sharedUserId = sharedUserId,
                sharedUserFullName = sharedUserFullName,
                sharedUserIdentityNumber = sharedUserIdentityNumber,
                sharedUserAvatarUrl = sharedUserAvatarUrl,
                mentions = mentions
            )
        } else {
            // Reflectively invoke the synthetic defaults constructor
            @Suppress("UNCHECKED_CAST")
            val localConstructor: Constructor<QuoteMessageItem> = this.constructorRef
                ?: QuoteMessageItem::class.java.getDeclaredConstructor(
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
                    String::class.java,
                    String::class.java,
                    Long::class.javaObjectType,
                    Int::class.javaObjectType,
                    Int::class.javaObjectType,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    Int::class.javaObjectType,
                    Int::class.javaObjectType,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    Int::class.javaPrimitiveType,
                    Util.DEFAULT_CONSTRUCTOR_MARKER
                ).also { this.constructorRef = it }
            return localConstructor.newInstance(
                messageId ?: throw Util.missingProperty("messageId", "message_id", reader),
                conversationId ?: throw Util.missingProperty(
                    "conversationId",
                    "conversation_id",
                    reader
                ),
                userId ?: throw Util.missingProperty("userId", "user_id", reader),
                userFullName ?: throw Util.missingProperty(
                    "userFullName",
                    "user_full_name",
                    reader
                ),
                userIdentityNumber ?: throw Util.missingProperty(
                    "userIdentityNumber",
                    "user_identity_number", reader
                ),
                type ?: throw Util.missingProperty("type", "type", reader),
                content,
                createdAt ?: throw Util.missingProperty("createdAt", "created_at", reader),
                status ?: throw Util.missingProperty("status", "status", reader),
                mediaStatus,
                userAvatarUrl,
                mediaName,
                mediaMimeType,
                mediaSize,
                mediaWidth,
                mediaHeight,
                thumbImage,
                thumbUrl,
                mediaUrl,
                mediaDuration,
                assetUrl,
                assetHeight,
                assetWidth,
                stickerId,
                appId,
                sharedUserId,
                sharedUserFullName,
                sharedUserIdentityNumber,
                sharedUserAvatarUrl,
                mentions,
                mask0,
                /* DefaultConstructorMarker */ null
            )
        }
    }

    override fun toJson(writer: JsonWriter, value_: QuoteMessageItem?) {
        if (value_ == null) {
            throw NullPointerException(
                "value_ was null! Wrap in .nullSafe() to write nullable " +
                    "values."
            )
        }
        writer.beginObject()
        writer.name("message_id")
        stringAdapter.toJson(writer, value_.messageId)
        writer.name("conversation_id")
        stringAdapter.toJson(writer, value_.conversationId)
        writer.name("user_id")
        stringAdapter.toJson(writer, value_.userId)
        writer.name("user_full_name")
        stringAdapter.toJson(writer, value_.userFullName)
        writer.name("user_identity_number")
        stringAdapter.toJson(writer, value_.userIdentityNumber)
        writer.name("type")
        stringAdapter.toJson(writer, value_.type)
        writer.name("content")
        nullableStringAdapter.toJson(writer, value_.content)
        writer.name("created_at")
        stringAdapter.toJson(writer, value_.createdAt)
        writer.name("status")
        stringAdapter.toJson(writer, value_.status)
        writer.name("media_status")
        nullableStringAdapter.toJson(writer, value_.mediaStatus)
        writer.name("user_avatar_url")
        nullableStringAdapter.toJson(writer, value_.userAvatarUrl)
        writer.name("media_name")
        nullableStringAdapter.toJson(writer, value_.mediaName)
        writer.name("media_mime_type")
        nullableStringAdapter.toJson(writer, value_.mediaMimeType)
        writer.name("media_size")
        nullableLongAdapter.toJson(writer, value_.mediaSize)
        writer.name("media_width")
        nullableIntAdapter.toJson(writer, value_.mediaWidth)
        writer.name("media_height")
        nullableIntAdapter.toJson(writer, value_.mediaHeight)
        writer.name("thumb_image")
        nullableStringAdapter.toJson(writer, value_.thumbImage)
        writer.name("thumb_url")
        nullableStringAdapter.toJson(writer, value_.thumbUrl)
        writer.name("media_url")
        nullableStringAdapter.toJson(writer, value_.mediaUrl)
        writer.name("media_duration")
        nullableStringAdapter.toJson(writer, value_.mediaDuration)
        writer.name("asset_url")
        nullableStringAdapter.toJson(writer, value_.assetUrl)
        writer.name("asset_height")
        nullableIntAdapter.toJson(writer, value_.assetHeight)
        writer.name("asset_width")
        nullableIntAdapter.toJson(writer, value_.assetWidth)
        writer.name("sticker_id")
        nullableStringAdapter.toJson(writer, value_.stickerId)
        writer.name("app_id")
        nullableStringAdapter.toJson(writer, value_.appId)
        writer.name("shared_user_id")
        nullableStringAdapter.toJson(writer, value_.sharedUserId)
        writer.name("shared_user_full_name")
        nullableStringAdapter.toJson(writer, value_.sharedUserFullName)
        writer.name("shared_user_identity_number")
        nullableStringAdapter.toJson(writer, value_.sharedUserIdentityNumber)
        writer.name("shared_user_avatar_url")
        nullableStringAdapter.toJson(writer, value_.sharedUserAvatarUrl)
        writer.name("mentions")
        nullableStringAdapter.toJson(writer, value_.mentions)
        writer.endObject()
    }
}
