package one.mixin.android.vo

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class TranscriptData(
    val chatUri: String,
    @Json(name ="document_uris")
    val documentUris: List<String>
) : Parcelable
