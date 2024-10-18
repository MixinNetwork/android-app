package one.mixin.android.vo

data class RecentSearch(
    val type: RecentSearchType,
    val iconUrl:String?= null,
    val title: String?= null,
    val subTitle: String?= null,
    val primaryKey:String? = null,
)

enum class RecentSearchType(val value: String) {
    DAPP("dapp"),
    MARKET("market"),
    BOT("bot"),
    LINK("link")
}