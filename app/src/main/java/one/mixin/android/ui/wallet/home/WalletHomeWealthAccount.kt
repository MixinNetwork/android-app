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
        get() = balanceUsd.numberFormat2()

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
    if (products.none { it.account?.hasCurrentBalance() == true }) return null
    val selectedProduct = products
        .sortedWith(
            compareByDescending<WealthProduct> { decimal(it.account?.totalPrincipal) }
                .thenByDescending { decimal(it.account?.totalEarnings) }
                .thenByDescending { maxAnnualRateValue(it.annualRates) },
        )
        .first()

    val totalPrincipal = products.fold(BigDecimal.ZERO) { total, product ->
        total + decimal(product.account?.totalPrincipal)
    }
    val totalEarnings = products.fold(BigDecimal.ZERO) { total, product ->
        total + decimal(product.account?.totalEarnings)
    }
    val redeemableEarnings = products.fold(BigDecimal.ZERO) { total, product ->
        total + decimal(product.account?.redeemableEarnings)
    }
    val assetPriceUsd = priceUsd.toBigDecimalOrNull()
        ?.takeIf { it > BigDecimal.ZERO }
        ?: products.firstNotNullOfOrNull { it.priceUsd?.toBigDecimalOrNull() }
        ?: BigDecimal.ZERO
    return WalletWealthDetails(
        productionId = selectedProduct.productionId,
        totalPrincipal = totalPrincipal,
        totalEarningsUsd = totalEarnings.multiply(assetPriceUsd),
        pendingEarningsUsd = (totalEarnings - redeemableEarnings)
            .coerceAtLeast(BigDecimal.ZERO)
            .multiply(assetPriceUsd),
        rewardRate = maxAnnualRate(products.flatMap { it.annualRates.orEmpty() }),
    )
}

internal fun List<WealthProduct>.toWalletHomeWealthAccounts(
    assetItems: Map<String, TokenItem> = emptyMap(),
): List<WalletHomeWealthAccount> =
    mapNotNull { product ->
        val account = product.account ?: return@mapNotNull null

        val asset = assetItems[product.assetId]
        val assetPriceUsd = (product.priceUsd ?: asset?.priceUsd)
            ?.toBigDecimalOrNull()
            ?.takeIf { it > BigDecimal.ZERO }
            ?: return@mapNotNull null
        WalletHomeWealthAccount(
            assetId = product.assetId,
            assetSymbol = product.assetSymbol ?: asset?.symbol.orEmpty(),
            iconUrl = product.iconUrl ?: asset?.iconUrl.orEmpty(),
            balanceUsd = decimal(account.totalPrincipal).multiply(assetPriceUsd),
            earningsUsd = decimal(account.totalEarnings).multiply(assetPriceUsd),
            apyText = maxAnnualRate(product.annualRates),
        )
    }

internal fun maxAnnualRate(annualRates: List<String>?): String? =
    annualRates
        .orEmpty()
        .mapNotNull { rate ->
            val normalizedRate = rate.trim()
            annualRateValue(normalizedRate)
                ?.let { normalizedRate to it }
        }
        .maxByOrNull { (_, value) -> value }
        ?.first

private fun maxAnnualRateValue(annualRates: List<String>?): BigDecimal =
    annualRates
        .orEmpty()
        .mapNotNull(::annualRateValue)
        .maxOrNull()
        ?: BigDecimal.ZERO

private fun annualRateValue(rate: String): BigDecimal? =
    rate
        .removeSuffix("%")
        .trim()
        .toBigDecimalOrNull()

internal fun WalletHomeState.withWealthAccounts(
    accounts: List<WalletHomeWealthAccount>,
): WalletHomeState {
    val cardsWithoutAccountCards = cards.filterNot {
        it == WalletHomeCardType.CASH || it == WalletHomeCardType.ACCOUNTS
    }
    val hasWealthAccount = accounts.isNotEmpty()
    val hasWealthBalance = accounts.any { it.balanceUsd.signum() != 0 }
    val accountCard = when {
        hasWealthAccount && (cashAccount == null || hasWealthBalance) -> WalletHomeCardType.ACCOUNTS
        cashAccount != null -> WalletHomeCardType.CASH
        else -> null
    }
    val cards = accountCard?.let {
        cardsWithoutAccountCards.withCardAfterBalance(it)
    } ?: cardsWithoutAccountCards
    return copy(
        cards = cards,
        wealthAccounts = accounts,
    )
}

private fun WealthAccountSummary.hasCurrentBalance(): Boolean =
    listOf(totalPrincipal, totalEarnings, redeemableEarnings).any {
        it?.toBigDecimalOrNull()?.let { amount -> amount > BigDecimal.ZERO } == true
    }

private fun decimal(value: String?): BigDecimal = value?.toBigDecimalOrNull() ?: BigDecimal.ZERO

private fun List<WalletHomeCardType>.withCardAfterBalance(
    card: WalletHomeCardType,
): List<WalletHomeCardType> {
    val balanceIndex = indexOf(WalletHomeCardType.BALANCE)
    if (balanceIndex == -1) return listOf(card) + this
    return take(balanceIndex + 1) + card + drop(balanceIndex + 1)
}
