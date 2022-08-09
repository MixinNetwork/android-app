package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class AlbumUploadRequest(
    @SerializedName("data_zip_base64")
    val dataZipBase64: String
)
