package one.mixin.android.moshi.adaptrer

import android.graphics.Bitmap
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.internal.Util
import one.mixin.android.ui.web.WebClip
import one.mixin.android.vo.App
import kotlin.Boolean
import kotlin.Int
import kotlin.String

class WebClipJsonAdapter(
    moshi: Moshi
) : JsonAdapter<WebClip>() {
    private val options: JsonReader.Options = JsonReader.Options.of(
        "url", "app", "titleColor",
        "name", "thumb", "icon", "conversationId", "shareable"
    )

    private val stringAdapter: JsonAdapter<String> = moshi.adapter(
        String::class.java, emptySet(),
        "url"
    )

    private val nullableAppAdapter: JsonAdapter<App?> = moshi.adapter(
        App::class.java, emptySet(),
        "app"
    )

    private val intAdapter: JsonAdapter<Int> = moshi.adapter(
        Int::class.java, emptySet(),
        "titleColor"
    )

    private val nullableStringAdapter: JsonAdapter<String?> = moshi.adapter(
        String::class.java,
        emptySet(), "name"
    )

    private val nullableBitmapAdapter: JsonAdapter<Bitmap?> = moshi.adapter(
        Bitmap::class.java,
        emptySet(), "thumb"
    )

    private val nullableBooleanAdapter: JsonAdapter<Boolean?> =
        moshi.adapter(Boolean::class.javaObjectType, emptySet(), "shareable")

    override fun toString(): String = buildString(29) {
        append("GeneratedJsonAdapter(").append("WebClip").append(')')
    }

    override fun fromJson(reader: JsonReader): WebClip {
        var url: String? = null
        var app: App? = null
        var titleColor: Int? = null
        var name: String? = null
        var thumb: Bitmap? = null
        var icon: Bitmap? = null
        var conversationId: String? = null
        var shareable: Boolean? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.selectName(options)) {
                0 -> url = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "url",
                    "url",
                    reader
                )
                1 -> app = nullableAppAdapter.fromJson(reader)
                2 -> titleColor = intAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "titleColor",
                    "titleColor", reader
                )
                3 -> name = nullableStringAdapter.fromJson(reader)
                4 -> thumb = nullableBitmapAdapter.fromJson(reader)
                5 -> icon = nullableBitmapAdapter.fromJson(reader)
                6 -> conversationId = nullableStringAdapter.fromJson(reader)
                7 -> shareable = nullableBooleanAdapter.fromJson(reader)
                -1 -> {
                    // Unknown name, skip it.
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()
        return WebClip(
            url = url ?: throw Util.missingProperty("url", "url", reader),
            app = app,
            titleColor = titleColor ?: throw Util.missingProperty(
                "titleColor",
                "titleColor",
                reader
            ),
            name = name,
            thumb = thumb,
            icon = icon,
            conversationId = conversationId,
            shareable = shareable,
            webView = null,
            isFinished = false
        )
    }

    override fun toJson(writer: JsonWriter, value_: WebClip?) {
        if (value_ == null) {
            throw NullPointerException(
                "value_ was null! Wrap in .nullSafe() to write nullable " +
                    "values."
            )
        }
        writer.beginObject()
        writer.name("url")
        stringAdapter.toJson(writer, value_.url)
        writer.name("app")
        nullableAppAdapter.toJson(writer, value_.app)
        writer.name("titleColor")
        intAdapter.toJson(writer, value_.titleColor)
        writer.name("name")
        nullableStringAdapter.toJson(writer, value_.name)
        writer.name("thumb")
        nullableBitmapAdapter.toJson(writer, value_.thumb)
        writer.name("icon")
        nullableBitmapAdapter.toJson(writer, value_.icon)
        writer.name("conversationId")
        nullableStringAdapter.toJson(writer, value_.conversationId)
        writer.name("shareable")
        nullableBooleanAdapter.toJson(writer, value_.shareable)
        writer.endObject()
    }
}
