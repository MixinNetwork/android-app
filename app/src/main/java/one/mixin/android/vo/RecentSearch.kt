package one.mixin.android.vo

data class RecentSearch(
    val type: RecentSearchType,
    val iconUrl: String? = null,
    val title: String? = null,
    val subTitle: String? = null,
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