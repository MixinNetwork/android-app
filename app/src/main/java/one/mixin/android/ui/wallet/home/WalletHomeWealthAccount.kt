package one.mixin.android.ui.wallet.home

import one.mixin.android.api.response.WealthAccountSummary
import one.mixin.android.api.response.WealthProduct
import one.mixin.android.extension.numberFormat2
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal

data class WalletHomeWealthAccount(
    val assetId: String,
    val assetSymbol: String,
    val iconUrl: String,
    val balanceUsd: BigDecimal,
    val earningsUsd: BigDecimal,
    val apyText: String?,
) {
    val balanceAmountText: String
        get() = usdBalanceAmountText(balanceUsd)

    val earningsAmountText: String
        get() {
            val prefix = when {
                earningsUsd > BigDecimal.ZERO -> "+"
                earningsUsd < BigDecimal.ZERO -> "-"
                else -> ""
            }
            return "${prefix}\$${earningsUsd.abs().numberFormat2()}"
        }
}

data class WalletWealthDetails(
    val productionId: String,
    val totalPrincipal: BigDecimal,
    val totalEarningsUsd: BigDecimal,
    val pendingEarningsUsd: BigDecimal,
    val rewardRate: String?,
)

internal fun List<WealthProduct>.toWalletWealthDetails(
    assetId: String,
    priceUsd: String,
): WalletWealthDetails? {
    val products = filter { it.assetId == assetId }
    if (products.isEmpty()) return null
    val productionIds = products.map { it.productionId }.toSet()
    val selectedProductionId = products
        .groupBy { it.productionId }
        .maxWithOrNull(
            compareBy<Map.Entry<String, List<WealthProduct>>> { (_, productionProducts) ->
                productionProducts.fold(BigDecimal.ZERO) { total, product ->
                    total + decimal(product.account.totalPrincipal)
                }
            }.thenBy { (_, productionProducts) ->
                productionProducts.fold(BigDecimal.ZERO) { total, product ->
                    total + decimal(product.account.redeemableEarnings)
                }
            }.thenBy { (_, productionProducts) ->
                maxAnnualRateValue(productionProducts.flatMap { it.annualRates })
            },
        )
        ?.key
        ?: return null

    val totalPrincipal = products.fold(BigDecimal.ZERO) { total, product ->
        total + decimal(product.account.totalPrincipal)
    }
    val totalEarnings = products.fold(BigDecimal.ZERO) { total, product ->
        total + decimal(product.account.totalEarnings)
    }
    val redeemableEarnings = products.fold(BigDecimal.ZERO) { total, product ->
        total + decimal(product.account.redeemableEarnings)
    }
    val assetPriceUsd = priceUsd.toBigDecimalOrNull()
        ?.takeIf { it > BigDecimal.ZERO }
        ?: BigDecimal.ZERO
    return WalletWealthDetails(
        productionId = selectedProductionId,
        totalPrincipal = totalPrincipal,
        totalEarningsUsd = totalEarnings.multiply(assetPriceUsd),
        pendingEarningsUsd = redeemableEarnings.multiply(assetPriceUsd),
        rewardRate = annualRateRange(
            filter { it.productionId in productionIds }
                .flatMap { it.annualRates },
        ),
    )
}

internal fun List<WealthProduct>.toWalletHomeWealthAccounts(
    assetItems: Map<String, TokenItem> = emptyMap(),
): List<WalletHomeWealthAccount> =
    groupBy { it.assetId }.map { (assetId, products) ->
        val asset = assetItems[assetId]
        val accountValues = products.map { product ->
            val priceUsd = asset?.priceUsd
                ?.toBigDecimalOrNull()
                ?.takeIf { it > BigDecimal.ZERO }
                ?: BigDecimal.ZERO
            product.account to priceUsd
        }
        WalletHomeWealthAccount(
            assetId = assetId,
            assetSymbol = asset?.symbol.orEmpty(),
            iconUrl = products.firstNotNullOfOrNull { it.iconUrl.takeIf(String::isNotBlank) }
                ?: asset?.iconUrl.orEmpty(),
            balanceUsd = accountValues.fold(BigDecimal.ZERO) { total, (account, priceUsd) ->
                total + wealthAccountUsdBalance(account, priceUsd)
            },
            earningsUsd = accountValues.fold(BigDecimal.ZERO) { total, (account, priceUsd) ->
                total + decimal(account.totalEarnings).multiply(priceUsd)
            },
            apyText = annualRateRange(products.flatMap { it.annualRates }),
        )
    }

// Matches iOS EarnAccount: (totalPrincipal + redeemableEarnings) * priceUsd.
internal fun wealthAccountUsdBalance(
    account: WealthAccountSummary,
    priceUsd: BigDecimal,
): BigDecimal = (decimal(account.totalPrincipal) + decimal(account.redeemableEarnings)).multiply(priceUsd)

internal fun annualRateRange(annualRates: List<String>?): String? {
    val rates = annualRates.orEmpty().mapNotNull(::annualRateValue)
    val minRate = rates.minOrNull() ?: return null
    val maxRate = rates.maxOrNull() ?: return null
    val minText = "${minRate.toPlainString()}%"
    return if (minRate.compareTo(maxRate) == 0) minText else "$minText-${maxRate.toPlainString()}%"
}

private fun maxAnnualRateValue(annualRates: List<String>?): BigDecimal =
    annualRates
        .orEmpty()
        .mapNotNull(::annualRateValue)
        .maxOrNull()
        ?: BigDecimal.ZERO

private fun annualRateValue(rate: String): BigDecimal? {
    val normalizedRate = rate.trim()
    val value = normalizedRate
        .removeSuffix("%")
        .trim()
        .toBigDecimalOrNull()
        ?: return null
    return if (normalizedRate.endsWith('%')) value else value.movePointRight(2)
}

internal fun WalletHomeState.withWealthAccounts(
    accounts: List<WalletHomeWealthAccount>,
): WalletHomeState {
    val cardsWithoutAccountCards = cards.filterNot {
        it == WalletHomeCardType.CASH || it == WalletHomeCardType.ACCOUNTS
    }
    val hasWealthAccount = accounts.isNotEmpty()
    val accountCard = when {
        hasWealthAccount -> WalletHomeCardType.ACCOUNTS
        cashAccount != null -> WalletHomeCardType.CASH
        else -> null
    }
    val cards = accountCard?.let {
        cardsWithoutAccountCards.withCardAfterBanner(it)
    } ?: cardsWithoutAccountCards
    return copy(
        cards = cards,
        wealthAccounts = accounts,
    )
}

private fun decimal(value: String?): BigDecimal = value?.toBigDecimalOrNull() ?: BigDecimal.ZERO

private fun List<WalletHomeCardType>.withCardAfterBanner(
    card: WalletHomeCardType,
): List<WalletHomeCardType> {
    val bannerIndex = indexOf(WalletHomeCardType.BANNER)
    if (bannerIndex != -1) return take(bannerIndex + 1) + card + drop(bannerIndex + 1)
    val balanceIndex = indexOf(WalletHomeCardType.BALANCE)
    if (balanceIndex == -1) return listOf(card) + this
    return take(balanceIndex + 1) + card + drop(balanceIndex + 1)
}
