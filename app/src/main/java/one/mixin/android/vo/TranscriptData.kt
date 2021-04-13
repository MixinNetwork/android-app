package one.mixin.android.vo

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class TranscriptData(
    val chatUri: String,
    @SerializedName("document_uris")
    val documentUris: List<String>
) : Parcelable
