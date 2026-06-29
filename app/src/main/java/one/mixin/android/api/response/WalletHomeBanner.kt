package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class WalletHomeBanner(
    @SerializedName(value = "banner_id", alternate = ["id"])
    val bannerId: String = "",
    @SerializedName("placement")
    val placement: String = "",
    @SerializedName("lang")
    val lang: String = "",
    @SerializedName("icon_url")
    val iconUrl: String = "",
    @SerializedName("title")
    val title: String = "",
    @SerializedName("description")
    val description: String = "",
    @SerializedName("actions")
    val actions: List<WalletHomeBannerAction> = emptyList(),
    @SerializedName("action_url")
    val actionUrl: String? = null,
    @SerializedName("tracking_key")
    val trackingKey: String = "",
    @SerializedName("status")
    val status: String = BANNER_STATUS_ACTIVE,
    @SerializedName("start_at")
    val startAt: String = "",
    @SerializedName("end_at")
    val endAt: String = "",
    @SerializedName("chains")
    val chains: List<String> = emptyList(),
    @SerializedName("priority")
    val priority: Int = 0,
    @SerializedName("created_at")
    val createdAt: String = "",
    @SerializedName("updated_at")
    val updatedAt: String = "",
) {
    val key: String
        get() = bannerId.takeUnless(String::isBlank)
            ?: actionUrl.takeUnless(String?::isNullOrBlank)
            ?: title.takeUnless(String::isBlank)
            ?: iconUrl

    val hasVisualContent: Boolean
        get() = title.isNotBlank() ||
            description.isNotBlank() ||
            visibleActions.isNotEmpty() ||
            iconUrl.isNotBlank()

    val visibleActions: List<WalletHomeBannerAction>
        get() = actions
            .firstOrNull { it.label.isNotBlank() && it.action.isNotBlank() }
            ?.let(::listOf)
            .orEmpty()

    val hasButtonStyle: Boolean
        get() = visibleActions.isNotEmpty()

    val isActive: Boolean
        get() = status.isBlank() || status.equals(BANNER_STATUS_ACTIVE, ignoreCase = true)

    companion object {
        const val BANNER_STATUS_ACTIVE = "active"
        const val BANNER_STATUS_INACTIVE = "inactive"
    }
}

data class WalletHomeBannerAction(
    @SerializedName("label")
    val label: String = "",
    @SerializedName("action")
    val action: String = "",
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
