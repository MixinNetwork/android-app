package one.mixin.android.api.response

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import one.mixin.android.api.MixinResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class EarnAccountTest {
    @Test
    fun parsesActualEarnProductsResponse() {
        val json =
            """
            {
              "data": [
                {
                  "production_id": "production-1",
                  "asset_id": "asset-1",
                  "chain_id": "chain-1",
                  "icon_url": "https://example.com/usdt.png",
                  "annual_rates": ["0.1095", "0.0730", "0.0365"],
                  "account": {
                    "total_principal": "0",
                    "total_earnings": "0",
                    "yesterday_earnings": "1.5"
                  }
                },
                {
                  "production_id": "production-1",
                  "asset_id": "asset-2",
                  "chain_id": "chain-1",
                  "icon_url": "https://example.com/usdc.png",
                  "annual_rates": ["0.0365"],
                  "account": {
                    "total_principal": "0",
                    "total_earnings": "0"
                  }
                },
                {
                  "production_id": "production-2",
                  "asset_id": "asset-3",
                  "chain_id": "chain-1",
                  "icon_url": "https://example.com/usdc.png",
                  "annual_rates": ["0.0200"],
                  "account": {
                    "total_principal": "0",
                    "total_earnings": "0"
                  }
                }
              ]
            }
            """.trimIndent()
        val type = object : TypeToken<MixinResponse<List<EarnProduct>>>() {}.type

        val response = Gson().fromJson<MixinResponse<List<EarnProduct>>>(json, type)
        val products = response.data!!
        val product = products.first()
        val account = product.account

        assertEquals(3, products.size)
        assertEquals("production-1", product.productionId)
        assertEquals("production-1", products[1].productionId)
        assertEquals("asset-2", products[1].assetId)
        assertEquals(listOf("0.1095", "0.0730", "0.0365"), product.annualRates)
        assertEquals("chain-1", product.chainId)
        assertEquals("https://example.com/usdt.png", product.iconUrl)
        assertEquals("0", account.totalPrincipal)
        assertEquals("0", account.totalEarnings)
        assertEquals("1.5", account.yesterdayEarnings)
    }

}
