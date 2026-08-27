package one.mixin.android.ui.wallet.home

import java.math.BigDecimal
import one.mixin.android.api.response.WealthAccountSummary
import one.mixin.android.api.response.WealthProduct
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
        val product = WealthProduct(
            productionId = "production-1",
            assetId = "asset-1",
            priceUsd = "1",
            annualRates = listOf("0.0365", "0.1095", "0.0730"),
            account = WealthAccountSummary(
                totalPrincipal = "100",
                totalEarnings = "20",
                redeemableEarnings = "19.99959998",
            ),
        )

        val preferredProduct = product.copy(
            productionId = "019f21ba-95f7-7bd4-a108-3620661dd591",
            annualRates = listOf("0.1095"),
        )
        val details = requireNotNull(
            listOf(
                product.copy(
                    productionId = "lower-principal",
                    account = product.account?.copy(
                        totalPrincipal = "99",
                        redeemableEarnings = "20",
                    ),
                ),
                product.copy(
                    productionId = "lower-earnings",
                    account = product.account?.copy(
                        totalEarnings = "19",
                        redeemableEarnings = "19",
                    ),
                ),
                preferredProduct,
                preferredProduct.copy(
                    account = product.account?.copy(
                        totalPrincipal = "1",
                        totalEarnings = "0",
                        redeemableEarnings = "0",
                    ),
                ),
                preferredProduct.copy(
                    assetId = "asset-2",
                    annualRates = listOf("0.2000"),
                    account = product.account?.copy(
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
        assertEquals(0, details.pendingEarningsUsd.compareTo(BigDecimal("0.00040002")))
        assertEquals("3.65%-20.00%", details.rewardRate)
    }

    @Test
    fun selectsHigherApyWhenProductionBalancesMatch() {
        val product = WealthProduct(
            productionId = "production-1",
            assetId = "asset-1",
            annualRates = listOf("0.0300"),
            account = WealthAccountSummary(totalPrincipal = "100"),
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
        val product = WealthProduct(
            productionId = "production-1",
            assetId = "asset-1",
            priceUsd = "1",
            account = WealthAccountSummary(
                totalPrincipal = "0",
                totalEarnings = "0",
                redeemableEarnings = "0",
            ),
        )

        val details = requireNotNull(
            listOf(product).toWalletWealthDetails("asset-1", "1"),
        )

        assertEquals(0, details.totalPrincipal.compareTo(BigDecimal.ZERO))
        assertEquals(0, details.totalEarningsUsd.compareTo(BigDecimal.ZERO))
        assertEquals(0, details.pendingEarningsUsd.compareTo(BigDecimal.ZERO))
    }

    @Test
    fun showsTokenDetailCardForSupportedTokenWithoutAccount() {
        val product = WealthProduct(
            productionId = "production-1",
            assetId = "asset-1",
            priceUsd = "1",
        )

        val details = requireNotNull(
            listOf(product).toWalletWealthDetails("asset-1", "1"),
        )

        assertEquals("production-1", details.productionId)
        assertEquals(0, details.totalEarningsUsd.compareTo(BigDecimal.ZERO))
    }

    @Test
    fun mapsZeroBalanceAccountToTheAccountCard() {
        val product = WealthProduct(
            productionId = "production-1",
            assetId = "asset-1",
            priceUsd = "1",
            account = WealthAccountSummary(
                totalPrincipal = "0",
                totalEarnings = "0",
                redeemableEarnings = "0",
            ),
        )

        val account = listOf(product).toWalletHomeWealthAccounts().single()

        assertEquals(0, account.balanceUsd.compareTo(BigDecimal.ZERO))
        assertEquals(0, account.earningsUsd.compareTo(BigDecimal.ZERO))
    }

    @Test
    fun mapsPrincipalAndRedeemableEarningsToTheAccountCard() {
        val product = WealthProduct(
            productionId = "production-1",
            assetId = "asset-1",
            chainId = "chain-1",
            assetName = "Tether USD",
            assetSymbol = "USDT",
            precision = 8,
            iconUrl = "https://example.com/usdt.png",
            priceUsd = "2",
            kind = "flexible",
            name = "USDT Flexible",
            description = "Daily USDT yield",
            status = "active",
            startAt = "2026-08-01T00:00:00Z",
            createdAt = "2026-07-20T08:00:00Z",
            updatedAt = "2026-08-17T00:00:00Z",
            annualRates = listOf("0.0500"),
            annualRateTiers = emptyList(),
            maxPerUser = "10000",
            sharePrices = emptyMap(),
            account = WealthAccountSummary(
                totalPrincipal = "1500",
                totalEarnings = "25",
                redeemableEarnings = "18",
            ),
        )

        val account = listOf(product).toWalletHomeWealthAccounts().single()

        assertEquals(0, account.balanceUsd.compareTo(BigDecimal("3036")))
        assertEquals(0, account.earningsUsd.compareTo(BigDecimal("50")))
        assertEquals("5.00%", account.apyText)
    }

    @Test
    fun homeBalanceCountsRedeemableEarningsWhenPrincipalIsZero() {
        val product = WealthProduct(
            productionId = "production-1",
            assetId = "asset-1",
            priceUsd = "1",
            account = WealthAccountSummary(
                totalPrincipal = "0",
                totalEarnings = "10",
                redeemableEarnings = "4",
            ),
        )

        val account = listOf(product).toWalletHomeWealthAccounts().single()

        assertEquals(0, account.balanceUsd.compareTo(BigDecimal("4")))
        assertEquals(0, account.earningsUsd.compareTo(BigDecimal("10")))
    }

    @Test
    fun homeBalanceIncludesRedeemableEarningsNotPendingTotalEarnings() {
        val product = WealthProduct(
            productionId = "production-1",
            assetId = "asset-1",
            priceUsd = "2",
            account = WealthAccountSummary(
                totalPrincipal = "100",
                totalEarnings = "20",
                redeemableEarnings = "5",
            ),
        )

        val account = listOf(product).toWalletHomeWealthAccounts().single()

        assertEquals(0, account.balanceUsd.compareTo(BigDecimal("210")))
        assertEquals(0, account.earningsUsd.compareTo(BigDecimal("40")))
    }

    @Test
    fun mergesProductsByAssetWithoutCombiningDifferentAssets() {
        val firstAssetProduct = WealthProduct(
            productionId = "production-1",
            assetId = "asset-1",
            assetSymbol = "USDT",
            iconUrl = "https://example.com/usdt.png",
            priceUsd = "2",
            annualRates = listOf("0.0300"),
            account = WealthAccountSummary(
                totalPrincipal = "100",
                totalEarnings = "2",
            ),
        )
        val accounts = listOf(
            firstAssetProduct,
            firstAssetProduct.copy(
                productionId = "production-2",
                annualRates = listOf("0.0500"),
                account = WealthAccountSummary(
                    totalPrincipal = "50",
                    totalEarnings = "1",
                ),
            ),
            firstAssetProduct.copy(
                assetId = "asset-2",
                assetSymbol = "USDC",
                annualRates = listOf("0.0400"),
                account = WealthAccountSummary(totalPrincipal = "25"),
            ),
        ).toWalletHomeWealthAccounts()

        assertEquals(2, accounts.size)
        val firstAccount = accounts.first { it.assetId == "asset-1" }
        assertEquals(0, firstAccount.balanceUsd.compareTo(BigDecimal("300")))
        assertEquals(0, firstAccount.earningsUsd.compareTo(BigDecimal("6")))
        assertEquals("3.00%-5.00%", firstAccount.apyText)
    }
}
