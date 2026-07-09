package one.mixin.android.pay

import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.api.response.AddressResponse
import one.mixin.android.api.response.WithdrawalResponse
import one.mixin.android.extension.isExternalTransferUrl
import one.mixin.android.vo.AssetPrecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExternalTransferUriParserTonTest {
    @Test
    fun tonTransferUriIsExternalTransferUrl() {
        assertTrue("ton://transfer/UQAG6wmpZWRwXQFP7HBZVEkHgwsH1wC2zHTV1VHKBDYgoMii".isExternalTransferUrl())
    }

    @Test
    fun parseTonJettonTransferUri() =
        runBlocking {
            val result = parse(
                "ton://transfer/UQAG6wmpZWRwXQFP7HBZVEkHgwsH1wC2zHTV1VHKBDYgoMii" +
                    "?jetton=EQCxE6mUtQJKFnGfaROTKOt1lZbDiiX1kCixRv7Nw2Id_sDs" +
                    "&amount=40000000&text=TON+Console%3A+Refill"
            )

            assertNotNull(result)
            assertEquals(Constants.AssetId.USDT_ASSET_TON_ID, result!!.assetId)
            assertEquals("UQAG6wmpZWRwXQFP7HBZVEkHgwsH1wC2zHTV1VHKBDYgoMii", result.destination)
            assertEquals("40", result.amount)
            assertEquals("TON Console: Refill", result.memo)
        }

    @Test
    fun parseNativeTonTransferUri() =
        runBlocking {
            val result = parse("ton://transfer/UQAG6wmpZWRwXQFP7HBZVEkHgwsH1wC2zHTV1VHKBDYgoMii?amount=40000000")

            assertNotNull(result)
            assertEquals(Constants.ChainId.TON_CHAIN_ID, result!!.assetId)
            assertEquals("UQAG6wmpZWRwXQFP7HBZVEkHgwsH1wC2zHTV1VHKBDYgoMii", result.destination)
            assertEquals("0.04", result.amount)
        }

    private suspend fun parse(url: String) =
        parseExternalTransferUri(
            url,
            { assetId, _, destination ->
                AddressResponse(destination, null, assetId)
            },
            { assetId, _ ->
                listOf(WithdrawalResponse(assetId, "0"))
            },
            { assetKey ->
                mockAssetKeyAssetId[assetKey]
            },
            { assetId ->
                AssetPrecision(assetId, Constants.ChainId.TON_CHAIN_ID, mockAssetPrecision[assetId] ?: 9)
            },
            { _, _, _, _ -> },
            parseLighting = { null }
        )

    private val mockAssetKeyAssetId =
        mapOf(
            "EQCxE6mUtQJKFnGfaROTKOt1lZbDiiX1kCixRv7Nw2Id_sDs" to Constants.AssetId.USDT_ASSET_TON_ID,
        )

    private val mockAssetPrecision =
        mapOf(
            Constants.AssetId.USDT_ASSET_TON_ID to 6,
            Constants.ChainId.TON_CHAIN_ID to 9,
        )
}
