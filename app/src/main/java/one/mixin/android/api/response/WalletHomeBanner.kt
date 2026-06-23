package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class WalletHomeBanner(
    @SerializedName(value = "banner_id", alternate = ["id"])
    val bannerId: String? = null,
    @SerializedName("placement")
    val placement: String? = null,
    @SerializedName("lang")
    val lang: String? = null,
    @SerializedName("icon_url")
    val iconUrl: String? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("actions")
    val actions: List<WalletHomeBannerAction>? = emptyList(),
    @SerializedName("action_url")
    val actionUrl: String? = null,
    @SerializedName("tracking_key")
    val trackingKey: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("start_at")
    val startAt: String? = null,
    @SerializedName("end_at")
    val endAt: String? = null,
    @SerializedName("chains")
    val chains: List<String>? = emptyList(),
    @SerializedName("priority")
    val priority: Int = 0,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
) {
    val key: String
        get() = bannerId.takeUnless(String?::isNullOrBlank)
            ?: actionUrl.takeUnless(String?::isNullOrBlank)
            ?: title.takeUnless(String?::isNullOrBlank)
            ?: iconUrl.orEmpty()

    val hasVisualContent: Boolean
        get() = !title.isNullOrBlank() ||
            !description.isNullOrBlank() ||
            visibleActions.isNotEmpty() ||
            !iconUrl.isNullOrBlank()

    val visibleActions: List<WalletHomeBannerAction>
        get() = actions.orEmpty()
            .firstOrNull { !it.label.isNullOrBlank() && !it.action.isNullOrBlank() }
            ?.let(::listOf)
            .orEmpty()

    val hasButtonStyle: Boolean
        get() = visibleActions.isNotEmpty()

    val isActive: Boolean
        get() = status.isNullOrBlank() || status.equals(BANNER_STATUS_ACTIVE, ignoreCase = true)

    companion object {
        const val BANNER_STATUS_ACTIVE = "active"
        const val BANNER_STATUS_INACTIVE = "inactive"
    }
}

data class WalletHomeBannerAction(
    @SerializedName("label")
    val label: String? = null,
    @SerializedName("action")
    val action: String? = null,
)

fun Set<String>.syncedWalletHomeClosedBannerIds(remoteBanners: List<WalletHomeBanner>): Set<String> {
    val remoteKeys = remoteBanners.mapNotNull { it.key.takeIf(String::isNotBlank) }.toSet()
    return filter { remoteKeys.contains(it) }.toSet()
}

fun List<WalletHomeBanner>.visibleWalletHomeBanners(closedBannerIds: Set<String>): List<WalletHomeBanner> =
    filter { banner ->
        banner.key.isNotBlank() &&
            banner.isActive &&
            banner.hasVisualContent &&
            (!banner.actionUrl.isNullOrBlank() || banner.visibleActions.isNotEmpty()) &&
            !closedBannerIds.contains(banner.key)
    }.sortedByDescending { it.priority }
