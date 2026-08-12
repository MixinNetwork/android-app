package one.mixin.android.vo

import com.google.gson.annotations.SerializedName

data class RecentSearch(
    @SerializedName("type")
    val type: RecentSearchType,
    @SerializedName("iconUrl")
    val iconUrl: String? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("subTitle")
    val subTitle: String? = null,
    @SerializedName("primaryKey")
    val primaryKey: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (type == RecentSearchType.LINK && other is RecentSearch && other.type == RecentSearchType.LINK) {
            return subTitle == other.subTitle
        }
        return super.equals(other)
    }
}

enum class RecentSearchType(val value: String) {
    DAPP("dapp"),
    ASSET("asset"),
    MARKET("market"),
    BOT("bot"),
    LINK("link")
}