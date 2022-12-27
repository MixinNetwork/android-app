package one.mixin.android.pay

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import one.mixin.android.api.response.AddressFeeResponse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class ExternalTransferUriParserTest {
    @Test
    fun testParseBitcoin() = runBlocking {
        val url1 = "bitcoin:BC1QA7A84SQ2NNKPXUA5DLY6FG553D5V06NSL608SS?amount=0.00186487"
        val url2 = "bitcoin:35pkcZ531UWYwVWRGeMG6eXkWbPptFg6AG?amount=0.00173492&fee=5&rbf=false&lightning=LNBC1734920N1P3EC8DGPP5NTUUNWS3GF9XUE4EZ2NCPEJCZHAJRVALFW8ALWFPN29LEE76NV5SDZ2GF5HGUN9VE5KCMPQV9SNYCMZVE3RWTF3XVMK2TF5XGMRJTFCXSCNSTF4VCCXYERYXQ6N2VRPVVCQZX7XQRP9SSP5Q4JSN54FHFQ8TRGHQGDQW2PUV790PXNSFVZG20CW322K0E6L7M8Q9QYYSSQA42ZJEMX44Y6PEW3YHWHXV9JUXTM96DMHKEPMD3LXUQTPH0HGSKX9TVZD2XVG7DETCVN450JXN25FM8G80GRYGU9ZHXC3XURSJ4Z20GPF8SQT7"
        val url3 = "LIGHTNING:LNBC1197710N1P36QPY7PP5NZT3GTZMZP00E8NAR0C40DQVS5JT7PWCF7Z4MLXKH6F988QT08MSDZ2GF5HGUN9VE5KCMPQXGENSVFKXPNRXTTRV43NWTF5V4SKVTFEVCUXYTTXXAJNZVM9X4JRGETY8YCQZX7XQRP9SSP5EU7UUK9E5VKGQ2KYTW68D2JHTK7ALWSTFKYFMMSL2FGT22ZLMW9Q9QYYSSQAWC3VFFRPEGE79NLXKRMPVVR8Q9NVUMD4LFF3U2QRJ23A0RUUTGKJ7UCQQTE3RV93JYH20GJFPQHGLL7K2RPJMNYFXAP9NXPC4XQ80GPFE00SQ"

        val result1 = parseExternalTransferUri(url1) { assetId, destination -> mockGetAddressFeeResponse(assetId, destination) }
        val result2 = parseExternalTransferUri(url2) { assetId, destination -> mockGetAddressFeeResponse(assetId, destination) }
        val result3 = parseExternalTransferUri(url3) { assetId, destination -> mockGetAddressFeeResponse(assetId, destination) }

        assert(result1 != null)
        assert(result2 != null)
        assert(result3 == null)
    }

    private val mockGetAddressFeeResponse: suspend (String, String) -> AddressFeeResponse? = { assetId, destination ->
        AddressFeeResponse(destination, null, assetId, "0")
    }
}