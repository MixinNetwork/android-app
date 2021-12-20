package one.mixin.android.vo.github

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.BuildConfig

@JsonClass(generateAdapter = true)
data class Latest(
    @Json(name = "tag_name")
    val tagName: String?
) {
    private fun versionCode(): Int? {
        return tagName?.run {
            this.substring(1).replace(".", "").toInt() * 100
        }
    }

    fun hasNewVersion(): Boolean {
        return versionCode()?.run { this > BuildConfig.VERSION_CODE }
            ?: false
    }
}
