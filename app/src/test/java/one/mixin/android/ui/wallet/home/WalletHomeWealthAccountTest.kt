package one.mixin.android.ui.wallet.home

import java.math.BigDecimal
import one.mixin.android.api.response.WealthAccountSummary
import one.mixin.android.api.response.EarnProduct
import one.mixin.android.vo.WithdrawalMemoPossibility
import one.mixin.android.vo.safe.TokenItem
import org.junit.Assert.assertEquals
import org.junit.Test

class WalletHomeWealthAccountTest {
    @Test
    fun showsAnnualRateRangeRegardlessOfOrder() {
        assertEquals(
            "3.65%-10.95%",
            annualRateRange(listOf("0.0365", "0.1095", "0.0730")),
        )
    }

    @Test
    fun ignoresInvalidAnnualRates() {
        assertEquals("7.30%", annualRateRange(listOf("invalid", "0.0730", "")))
        assertEquals(null, annualRateRange(listOf("invalid", "")))
    }

    @Test
    fun keepsPercentageAnnualRatesCompatible() {
        assertEquals("3.65%-10.95%", annualRateRange(listOf("3.65%", "10.95%", "7.30%")))
    }

    @Test
    fun aggregatesWealthDetailsForTokenDetailCard() {
        val product = wealthProduct(
            productionId = "production-1",
            assetId = "asset-1",
            annualRates = listOf("0.0365", "0.1095", "0.0730"),
            totalPrincipal = "100",
            totalEarnings = "20",
            redeemableEarnings = "19.99959998",
        )

        val preferredProduct = product.copy(
            productionId = "019f21ba-95f7-7bd4-a108-3620661dd591",
            annualRates = listOf("0.1095"),
        )
        val details = requireNotNull(
            listOf(
                product.copy(
                    productionId = "lower-principal",
                    account = product.account.copy(
                        totalPrincipal = "99",
                        redeemableEarnings = "20",
                    ),
                ),
                product.copy(
                    productionId = "lower-earnings",
                    account = product.account.copy(
                        totalEarnings = "19",
                        redeemableEarnings = "19",
                    ),
                ),
                preferredProduct,
                preferredProduct.copy(
                    account = product.account.copy(
                        totalPrincipal = "1",
                        totalEarnings = "0",
                        redeemableEarnings = "0",
                    ),
                ),
                preferredProduct.copy(
                    assetId = "asset-2",
                    annualRates = listOf("0.2000"),
                    account = product.account.copy(
                        totalPrincipal = "10000",
                        totalEarnings = "1000",
                    ),
                ),
            ).toWalletWealthDetails(
                assetId = "asset-1",
                priceUsd = "1",
            ),
        )

        assertEquals(0, details.totalPrincipal.compareTo(BigDecimal("300")))
        assertEquals("019f21ba-95f7-7bd4-a108-3620661dd591", details.productionId)
        assertEquals(0, details.totalEarningsUsd.compareTo(BigDecimal("59")))
        assertEquals(0, details.pendingEarningsUsd.compareTo(BigDecimal("58.99959998")))
        assertEquals("3.65%-20.00%", details.rewardRate)
    }

    @Test
    fun selectsHigherApyWhenProductionBalancesMatch() {
        val product = wealthProduct(
            productionId = "production-1",
            assetId = "asset-1",
            annualRates = listOf("0.0300"),
            totalPrincipal = "100",
        )

        val details = requireNotNull(
            listOf(
                product,
                product.copy(
                    productionId = "production-2",
                    annualRates = listOf("0.0500"),
                ),
            ).toWalletWealthDetails("asset-1", "1"),
        )

        assertEquals("production-2", details.productionId)
    }

    @Test
    fun showsTokenDetailCardWhenAllProductsHaveZeroAccounts() {
        val product = wealthProduct(
            productionId = "production-1",
            assetId = "asset-1",
        )

        val details = requireNotNull(
            listOf(product).toWalletWealthDetails("asset-1", "1"),
        )

        assertEquals(0, details.totalPrincipal.compareTo(BigDecimal.ZERO))
        assertEquals(0, details.totalEarningsUsd.compareTo(BigDecimal.ZERO))
        assertEquals(0, details.pendingEarningsUsd.compareTo(BigDecimal.ZERO))
    }

    @Test
    fun tokenDetailPendingEarningUsesRedeemableEarnings() {
        val product = wealthProduct(
            productionId = "production-1",
            assetId = "asset-1",
            totalPrincipal = "100",
            totalEarnings = "20",
            redeemableEarnings = "5",
        )

        val details = requireNotNull(
            listOf(product).toWalletWealthDetails("asset-1", "2"),
        )

        assertEquals(0, details.totalEarningsUsd.compareTo(BigDecimal("40")))
        assertEquals(0, details.pendingEarningsUsd.compareTo(BigDecimal("10")))
    }

    @Test
    fun selectsHigherRedeemableEarningsWhenProductionBalancesMatch() {
        val product = wealthProduct(
            productionId = "production-1",
            assetId = "asset-1",
            totalPrincipal = "100",
            redeemableEarnings = "1",
        )

        val details = requireNotNull(
            listOf(
                product,
                product.copy(
                    productionId = "production-2",
                    account = product.account.copy(redeemableEarnings = "2"),
                ),
            ).toWalletWealthDetails("asset-1", "1"),
        )

        assertEquals("production-2", details.productionId)
    }

    @Test
    fun mapsZeroBalanceAccountToTheAccountCard() {
        val product = wealthProduct(
            productionId = "production-1",
            assetId = "asset-1",
        )

        val account = listOf(product).toWalletHomeWealthAccounts(
            assetItems = mapOf("asset-1" to tokenItem("asset-1", "1")),
        ).single()

        assertEquals(0, account.balanceUsd.compareTo(BigDecimal.ZERO))
        assertEquals(0, account.earningsUsd.compareTo(BigDecimal.ZERO))
    }

    @Test
    fun mapsAccountWithoutLocalPriceAsZeroUsdLikeIos() {
        val product = wealthProduct(
            productionId = "production-1",
            assetId = "asset-1",
            iconUrl = "https://example.com/usdt.png",
            totalPrincipal = "100",
            redeemableEarnings = "5",
        )

        val account = listOf(product).toWalletHomeWealthAccounts().single()

        assertEquals(0, account.balanceUsd.compareTo(BigDecimal.ZERO))
        assertEquals("https://example.com/usdt.png", account.iconUrl)
    }

    @Test
    fun mapsPrincipalAndRedeemableEarningsToTheAccountCard() {
        val product = wealthProduct(
            productionId = "production-1",
            assetId = "asset-1",
            chainId = "chain-1",
            iconUrl = "https://example.com/usdt.png",
            annualRates = listOf("0.0500"),
            totalPrincipal = "1500",
            totalEarnings = "25",
            redeemableEarnings = "18",
        )

        val account = listOf(product).toWalletHomeWealthAccounts(
            assetItems = mapOf("asset-1" to tokenItem("asset-1", "2", symbol = "USDT")),
        ).single()

        assertEquals(0, account.balanceUsd.compareTo(BigDecimal("3036")))
        assertEquals(0, account.earningsUsd.compareTo(BigDecimal("50")))
        assertEquals("5.00%", account.apyText)
    }

    @Test
    fun homeBalanceCountsRedeemableEarningsWhenPrincipalIsZero() {
        val product = wealthProduct(
            productionId = "production-1",
            assetId = "asset-1",
            totalEarnings = "10",
            redeemableEarnings = "4",
        )

        val account = listOf(product).toWalletHomeWealthAccounts(
            assetItems = mapOf("asset-1" to tokenItem("asset-1", "1")),
        ).single()

        assertEquals(0, account.balanceUsd.compareTo(BigDecimal("4")))
        assertEquals(0, account.earningsUsd.compareTo(BigDecimal("10")))
    }

    @Test
    fun homeBalanceIncludesRedeemableEarningsNotPendingTotalEarnings() {
        val product = wealthProduct(
            productionId = "production-1",
            assetId = "asset-1",
            totalPrincipal = "100",
            totalEarnings = "20",
            redeemableEarnings = "5",
        )

        val account = listOf(product).toWalletHomeWealthAccounts(
            assetItems = mapOf("asset-1" to tokenItem("asset-1", "2")),
        ).single()

        assertEquals(0, account.balanceUsd.compareTo(BigDecimal("210")))
        assertEquals(0, account.earningsUsd.compareTo(BigDecimal("40")))
    }

    @Test
    fun mergesProductsByAssetWithoutCombiningDifferentAssets() {
        val firstAssetProduct = wealthProduct(
            productionId = "production-1",
            assetId = "asset-1",
            iconUrl = "https://example.com/usdt.png",
            annualRates = listOf("0.0300"),
            totalPrincipal = "100",
            totalEarnings = "2",
        )
        val accounts = listOf(
            firstAssetProduct,
            firstAssetProduct.copy(
                productionId = "production-2",
                annualRates = listOf("0.0500"),
                account = WealthAccountSummary(
                    totalPrincipal = "50",
                    totalEarnings = "1",
                    redeemableEarnings = "0",
                ),
            ),
            firstAssetProduct.copy(
                assetId = "asset-2",
                annualRates = listOf("0.0400"),
                account = WealthAccountSummary(
                    totalPrincipal = "25",
                    totalEarnings = "0",
                    redeemableEarnings = "0",
                ),
            ),
        ).toWalletHomeWealthAccounts(
            assetItems = mapOf(
                "asset-1" to tokenItem("asset-1", "2", symbol = "USDT"),
                "asset-2" to tokenItem("asset-2", "2", symbol = "USDC"),
            ),
        )

        assertEquals(2, accounts.size)
        val firstAccount = accounts.first { it.assetId == "asset-1" }
        assertEquals(0, firstAccount.balanceUsd.compareTo(BigDecimal("300")))
        assertEquals(0, firstAccount.earningsUsd.compareTo(BigDecimal("6")))
        assertEquals("3.00%-5.00%", firstAccount.apyText)
    }

    private fun wealthProduct(
        productionId: String = "production-1",
        assetId: String = "asset-1",
        chainId: String = "chain-1",
        iconUrl: String = "https://example.com/usdt.png",
        annualRates: List<String> = emptyList(),
        totalPrincipal: String = "0",
        totalEarnings: String = "0",
        redeemableEarnings: String = "0",
    ) = EarnProduct(
        productionId = productionId,
        assetId = assetId,
        chainId = chainId,
        iconUrl = iconUrl,
        annualRates = annualRates,
        account = WealthAccountSummary(
            totalPrincipal = totalPrincipal,
            totalEarnings = totalEarnings,
            redeemableEarnings = redeemableEarnings,
        ),
    )

    private fun tokenItem(
        assetId: String,
        priceUsd: String,
        symbol: String = "USDT",
    ) = TokenItem(
        assetId = assetId,
        symbol = symbol,
        name = symbol,
        iconUrl = "",
        balance = "0",
        priceBtc = "0",
        priceUsd = priceUsd,
        chainId = "chain-1",
        changeUsd = "0",
        changeBtc = "0",
        hidden = false,
        confirmations = 0,
        chainIconUrl = "",
        chainSymbol = symbol,
        chainName = symbol,
        assetKey = assetId,
        dust = null,
        withdrawalMemoPossibility = WithdrawalMemoPossibility.POSSIBLE,
        collectionHash = null,
        level = 0,
        precision = 8,
    )
}
