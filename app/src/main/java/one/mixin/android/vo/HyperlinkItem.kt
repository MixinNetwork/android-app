package one.mixin.android.vo

import androidx.recyclerview.widget.DiffUtil
import com.google.gson.annotations.SerializedName

data class HyperlinkItem(
    @SerializedName("message_id")
    val messageId: String,
    @SerializedName("hyperlink")
    val hyperlink: String,
    @SerializedName("site_name")
    val siteName: String,
    @SerializedName("site_title")
    val siteTitle: String,
    @SerializedName("site_description")
    val siteDescription: String?,
    @SerializedName("site_image")
    val siteImage: String?,
    @SerializedName("created_at")
    val createdAt: String,
) {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<HyperlinkItem>() {
            override fun areItemsTheSame(oldItem: HyperlinkItem, newItem: HyperlinkItem) =
                oldItem.messageId == newItem.messageId

            override fun areContentsTheSame(oldItem: HyperlinkItem, newItem: HyperlinkItem) =
                oldItem == newItem
        }
    }
}
