package one.mixin.android.ui.wallet.home

import one.mixin.android.api.response.EarnAccountSummary
import one.mixin.android.api.response.EarnProduct
import one.mixin.android.vo.WithdrawalMemoPossibility
import one.mixin.android.vo.safe.TokenItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class WalletHomeEarnAccountTest {
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
    fun aggregatesEarnDetailsForTokenDetailCard() {
        val product = earnProduct(
            productionId = "production-1",
            assetId = "asset-1",
            annualRates = listOf("0.0365", "0.1095", "0.0730"),
            totalPrincipal = "100",
            totalEarnings = "20",
            yesterdayEarnings = "2",
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
                    ),
                ),
                product.copy(
                    productionId = "lower-earnings",
                    account = product.account.copy(
                        totalEarnings = "19",
                    ),
                ),
                preferredProduct,
                preferredProduct.copy(
                    account = product.account.copy(
                        totalPrincipal = "1",
                        totalEarnings = "0",
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
            ).toWalletEarnDetails(
                assetId = "asset-1",
                priceUsd = "1",
            ),
        )

        assertEquals(0, details.totalPrincipal.compareTo(BigDecimal("300")))
        assertEquals("019f21ba-95f7-7bd4-a108-3620661dd591", details.productionId)
        assertEquals(0, details.totalEarningsUsd.compareTo(BigDecimal("59")))
        assertEquals(0, details.yesterdayEarnings.compareTo(BigDecimal("8")))
        assertEquals("3.65%-20.00%", details.rewardRate)
    }

    @Test
    fun selectsHigherApyWhenProductionBalancesMatch() {
        val product = earnProduct(
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
            ).toWalletEarnDetails("asset-1", "1"),
        )

        assertEquals("production-2", details.productionId)
    }

    @Test
    fun showsTokenDetailCardWhenAllProductsHaveZeroAccounts() {
        val product = earnProduct(
            productionId = "production-1",
            assetId = "asset-1",
        )

        val details = requireNotNull(
            listOf(product).toWalletEarnDetails("asset-1", "1"),
        )

        assertEquals(0, details.totalPrincipal.compareTo(BigDecimal.ZERO))
        assertEquals(0, details.yesterdayEarnings.compareTo(BigDecimal.ZERO))
        assertEquals(0, details.totalEarningsUsd.compareTo(BigDecimal.ZERO))
    }

    @Test
    fun tokenDetailYesterdayEarningsAggregatesAcrossProducts() {
        val details = requireNotNull(
            listOf(
                earnProduct(
                    productionId = "production-1",
                    assetId = "asset-1",
                    yesterdayEarnings = "1.25",
                ),
                earnProduct(
                    productionId = "production-2",
                    assetId = "asset-1",
                    yesterdayEarnings = "0.75",
                ),
                earnProduct(
                    productionId = "production-3",
                    assetId = "asset-2",
                    yesterdayEarnings = "99",
                ),
            ).toWalletEarnDetails("asset-1", "1"),
        )

        assertEquals(0, details.yesterdayEarnings.compareTo(BigDecimal("2.00")))
    }

    @Test
    fun tokenDetailYesterdayEarningsFallsBackToZeroForMissingAndMalformedValues() {
        val base = earnProduct(
            productionId = "production-1",
            assetId = "asset-1",
            yesterdayEarnings = "bad",
        )

        val details = requireNotNull(
            listOf(
                base,
                base.copy(
                    productionId = "production-2",
                    account = base.account.copy(yesterdayEarnings = "0"),
                ),
                base.copy(
                    productionId = "production-3",
                    account = base.account.copy(yesterdayEarnings = ""),
                ),
            ).toWalletEarnDetails("asset-1", "1"),
        )

        assertEquals(0, details.yesterdayEarnings.compareTo(BigDecimal.ZERO))
    }

    @Test
    fun mapsZeroBalanceAccountToTheAccountCard() {
        val product = earnProduct(
            productionId = "production-1",
            assetId = "asset-1",
        )

        val account = listOf(product).toWalletHomeEarnAccounts(
            assetItems = mapOf("asset-1" to tokenItem("asset-1", "1")),
        ).single()

        assertEquals(0, account.balanceUsd.compareTo(BigDecimal.ZERO))
        assertEquals(0, account.earningsUsd.compareTo(BigDecimal.ZERO))
    }

    @Test
    fun mapsAccountWithoutLocalPriceAsZeroUsdLikeIos() {
        val product = earnProduct(
            productionId = "production-1",
            assetId = "asset-1",
            iconUrl = "https://example.com/usdt.png",
            totalPrincipal = "100",
        )

        val account = listOf(product).toWalletHomeEarnAccounts().single()

        assertEquals(0, account.balanceUsd.compareTo(BigDecimal.ZERO))
        assertEquals("https://example.com/usdt.png", account.iconUrl)
    }

    @Test
    fun mapsPrincipalAndEarningsToTheAccountCard() {
        val product = earnProduct(
            productionId = "production-1",
            assetId = "asset-1",
            chainId = "chain-1",
            iconUrl = "https://example.com/usdt.png",
            annualRates = listOf("0.0500"),
            totalPrincipal = "1500",
            totalEarnings = "25",
        )

        val account = listOf(product).toWalletHomeEarnAccounts(
            assetItems = mapOf("asset-1" to tokenItem("asset-1", "2", symbol = "USDT")),
        ).single()

        assertEquals(0, account.balanceUsd.compareTo(BigDecimal("3000")))
        assertEquals(0, account.earningsUsd.compareTo(BigDecimal("50")))
        assertEquals("5.00%", account.apyText)
    }

    @Test
    fun homeBalanceExcludesTotalEarnings() {
        val product = earnProduct(
            productionId = "production-1",
            assetId = "asset-1",
            totalPrincipal = "100",
            totalEarnings = "20",
        )

        val account = listOf(product).toWalletHomeEarnAccounts(
            assetItems = mapOf("asset-1" to tokenItem("asset-1", "2")),
        ).single()

        assertEquals(0, account.balanceUsd.compareTo(BigDecimal("200")))
        assertEquals(0, account.earningsUsd.compareTo(BigDecimal("40")))
    }

    @Test
    fun mergesProductsByAssetWithoutCombiningDifferentAssets() {
        val firstAssetProduct = earnProduct(
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
                account = EarnAccountSummary(
                    totalPrincipal = "50",
                    totalEarnings = "1",
                ),
            ),
            firstAssetProduct.copy(
                assetId = "asset-2",
                annualRates = listOf("0.0400"),
                account = EarnAccountSummary(
                    totalPrincipal = "25",
                    totalEarnings = "0",
                ),
            ),
        ).toWalletHomeEarnAccounts(
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

    private fun earnProduct(
        productionId: String = "production-1",
        assetId: String = "asset-1",
        chainId: String = "chain-1",
        iconUrl: String = "https://example.com/usdt.png",
        annualRates: List<String> = emptyList(),
        totalPrincipal: String = "0",
        totalEarnings: String = "0",
        yesterdayEarnings: String = "0",
    ) = EarnProduct(
        productionId = productionId,
        assetId = assetId,
        chainId = chainId,
        iconUrl = iconUrl,
        annualRates = annualRates,
        account = EarnAccountSummary(
            totalPrincipal = totalPrincipal,
            totalEarnings = totalEarnings,
            yesterdayEarnings = yesterdayEarnings,
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
