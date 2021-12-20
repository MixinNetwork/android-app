package one.mixin.android.vo

import androidx.recyclerview.widget.DiffUtil
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HyperlinkItem(
    @Json(name = "hyperlink")
    val hyperlink: String,
    @Json(name = "site_name")
    val siteName: String,
    @Json(name = "site_title")
    val siteTitle: String,
    @Json(name = "site_description")
    val siteDescription: String?,
    @Json(name = "site_image")
    val siteImage: String?,
    @Json(name = "created_at")
    val createdAt: String
) {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<HyperlinkItem>() {
            override fun areItemsTheSame(oldItem: HyperlinkItem, newItem: HyperlinkItem) =
                oldItem.hyperlink == newItem.hyperlink

            override fun areContentsTheSame(oldItem: HyperlinkItem, newItem: HyperlinkItem) =
                oldItem == newItem
        }
    }
}
