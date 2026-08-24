package one.mixin.android.ui.wallet.home

import java.math.BigDecimal
import one.mixin.android.api.response.WealthAccountSummary
import one.mixin.android.api.response.WealthProduct
import org.junit.Assert.assertEquals
import org.junit.Test

class WalletHomeWealthAccountTest {
    @Test
    fun selectsMaximumAnnualRateRegardlessOfOrder() {
        assertEquals(
            "10.95%",
            maxAnnualRate(listOf("3.65%", "10.95%", "7.30%")),
        )
    }

    @Test
    fun ignoresInvalidAnnualRates() {
        assertEquals("7.30%", maxAnnualRate(listOf("invalid", "7.30%", "")))
        assertEquals(null, maxAnnualRate(listOf("invalid", "")))
    }

    @Test
    fun aggregatesWealthDetailsForTokenDetailCard() {
        val product = WealthProduct(
            productionId = "production-1",
            assetId = "asset-1",
            priceUsd = "1",
            annualRates = listOf("3.65%", "10.95%", "7.30%"),
            account = WealthAccountSummary(
                totalPrincipal = "100",
                totalEarnings = "20",
                redeemableEarnings = "19.99959998",
            ),
        )

        val preferredProduct = product.copy(
            productionId = "019f21ba-95f7-7bd4-a108-3620661dd591",
            annualRates = listOf("10.95%"),
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
            ).toWalletWealthDetails(
                assetId = "asset-1",
                priceUsd = "1",
            ),
        )

        assertEquals(0, details.totalPrincipal.compareTo(BigDecimal("299")))
        assertEquals("019f21ba-95f7-7bd4-a108-3620661dd591", details.productionId)
        assertEquals(0, details.totalEarningsUsd.compareTo(BigDecimal("59")))
        assertEquals(0, details.pendingEarningsUsd.compareTo(BigDecimal("0.00040002")))
        assertEquals("10.95%", details.rewardRate)
    }

    @Test
    fun hidesTokenDetailCardWhenAllProductsHaveZeroAccounts() {
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

        assertEquals(
            null,
            listOf(product).toWalletWealthDetails("asset-1", "1"),
        )
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
    fun mapsPrincipalAndTotalEarningsToTheAccountCard() {
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
            annualRates = listOf("5.00%"),
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

        assertEquals(0, account.balanceUsd.compareTo(BigDecimal("3000")))
        assertEquals(0, account.earningsUsd.compareTo(BigDecimal("50")))
    }
}
