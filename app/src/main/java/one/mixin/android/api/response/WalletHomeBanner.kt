package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName
import org.threeten.bp.Instant

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

    fun isExpired(now: Instant = Instant.now()): Boolean {
        if (endAt.isBlank()) return false
        return runCatching {
            !now.isBefore(Instant.parse(endAt))
        }.getOrDefault(false)
    }

    companion object {
        const val BANNER_STATUS_ACTIVE = "active"
        const val BANNER_STATUS_INACTIVE = "inactive"
        const val BANNER_PLACEMENT_WALLET = "wallet_banner"
    }
}

data class WalletHomeBannerAction(
    @SerializedName("label")
    val label: String = "",
    @SerializedName("action")
    val action: String = "",
)

fun List<WalletHomeBanner>.filterWalletHomeBannersByChains(chains: Collection<String>): List<WalletHomeBanner> {
    val walletChains = chains.filterTo(mutableSetOf(), String::isNotBlank)
    if (walletChains.isEmpty()) return this

    return filter { banner ->
        banner.chains.none(String::isNotBlank) || banner.chains.any(walletChains::contains)
    }
}

fun Set<String>.syncedWalletHomeClosedBannerIds(remoteBanners: List<WalletHomeBanner>): Set<String> {
    return this
}

fun List<WalletHomeBanner>.visibleWalletHomeBanners(
    closedBannerIds: Set<String>,
    now: Instant = Instant.now(),
): List<WalletHomeBanner> =
    filter { banner ->
        banner.key.isNotBlank() &&
            (banner.placement.isBlank() || banner.placement == WalletHomeBanner.BANNER_PLACEMENT_WALLET) &&
            banner.isActive &&
            !banner.isExpired(now) &&
            banner.hasVisualContent &&
            (!banner.actionUrl.isNullOrBlank() || banner.visibleActions.isNotEmpty()) &&
            !closedBannerIds.contains(banner.key)
    }.sortedByDescending { it.priority }
