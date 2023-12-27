package one.mixin.android.pay

import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.api.response.AddressResponse
import one.mixin.android.api.response.WithdrawalResponse
import one.mixin.android.vo.AssetPrecision
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExternalTransferUriParserTest {
    @Test
    fun testParseBitcoin() =
        runBlocking {
            val url1 = "bitcoin:BC1QA7A84SQ2NNKPXUA5DLY6FG553D5V06NSL608SS?amount=0.00186487"
            val url2 = "bitcoin:35pkcZ531UWYwVWRGeMG6eXkWbPptFg6AG?amount=0.00173492&fee=5&rbf=false&lightning=LNBC1734920N1P3EC8DGPP5NTUUNWS3GF9XUE4EZ2NCPEJCZHAJRVALFW8ALWFPN29LEE76NV5SDZ2GF5HGUN9VE5KCMPQV9SNYCMZVE3RWTF3XVMK2TF5XGMRJTFCXSCNSTF4VCCXYERYXQ6N2VRPVVCQZX7XQRP9SSP5Q4JSN54FHFQ8TRGHQGDQW2PUV790PXNSFVZG20CW322K0E6L7M8Q9QYYSSQA42ZJEMX44Y6PEW3YHWHXV9JUXTM96DMHKEPMD3LXUQTPH0HGSKX9TVZD2XVG7DETCVN450JXN25FM8G80GRYGU9ZHXC3XURSJ4Z20GPF8SQT7"
            val url3 = "LIGHTNING:LNBC1197710N1P36QPY7PP5NZT3GTZMZP00E8NAR0C40DQVS5JT7PWCF7Z4MLXKH6F988QT08MSDZ2GF5HGUN9VE5KCMPQXGENSVFKXPNRXTTRV43NWTF5V4SKVTFEVCUXYTTXXAJNZVM9X4JRGETY8YCQZX7XQRP9SSP5EU7UUK9E5VKGQ2KYTW68D2JHTK7ALWSTFKYFMMSL2FGT22ZLMW9Q9QYYSSQAWC3VFFRPEGE79NLXKRMPVVR8Q9NVUMD4LFF3U2QRJ23A0RUUTGKJ7UCQQTE3RV93JYH20GJFPQHGLL7K2RPJMNYFXAP9NXPC4XQ80GPFE00SQ"
            val url4 = "bitcoin:BC1QA7A84SQ2NNKPXUA5DLY6FG553D5V06NSL608SS?amount=0.12e3"

            val result1 = parse(url1)
            val result2 = parse(url2)
            val result3 = parse(url3)
            val result4 = parse(url4)

            assertTrue(result1 != null)
            if (result1 != null) {
                checkResult(result1, Constants.ChainId.BITCOIN_CHAIN_ID, "BC1QA7A84SQ2NNKPXUA5DLY6FG553D5V06NSL608SS", "0.00186487")
            }
            assertTrue(result2 != null)
            if (result2 != null) {
                checkResult(result2, Constants.ChainId.BITCOIN_CHAIN_ID, "35pkcZ531UWYwVWRGeMG6eXkWbPptFg6AG", "0.00173492")
            }
            assertTrue(result3 == null)
            assertTrue(result4 == null)
        }

    @Test
    fun testParseEthereum() =
        runBlocking {
            val url1 = "ethereum:0xfb6916095ca1df60bb79Ce92ce3ea74c37c5d359?value=2.014e18"
            val url2 = "ethereum:pay-0xdAC17F958D2ee523a2206206994597C13D831ec7@1/transfer?address=0x00d02d4A148bCcc66C6de20C4EB1CbAB4298cDcc&uint256=2e7&gasPrice=14"
            val url3 = "ethereum:0xD994790d2905b073c438457c9b8933C0148862db@1?value=1.697e16&gasPrice=14&label=Bitrefill%2008cba4ee-b6cd-47c8-9768-c82959c0edce"
            val url4 = "ethereum:0xA974c709cFb4566686553a20790685A47acEAA33@1/transfer?address=0xB38F2E40e82F0AE5613D55203d84953aE4d5181B&amount=1.24&uint256=1.24e18"
            val url5 = "ethereum:pay-0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48@1/transfer?address=0x50bF16E33E892F1c9Aa7C7FfBaF710E971b86Dd1&gasPrice=14"
            val url6 = "ethereum:0xA974c709cFb4566686553a20790685A47acEAA33@1/transfer?a=b&c=d&uint256=1.24e18&e=f&amount=1.24&g=h&address=0xB38F2E40e82F0AE5613D55203d84953aE4d5181B&i=j&k=m&n=o&p=q"
            val url7 = "ethereum:0xA974c709cFb4566686553a20790685A47acEAA33@1/transfer?address=0xB38F2E40e82F0AE5613D55203d84953aE4d5181B&amount=1e7&uint256=1.24e18"
            val url8 = "ethereum:0x20269e75b1637632e87f65A0A053d6720A781f39?amount=0.00016882"
            val url9 = "ethereum:0xfb6916095ca1df60bb79Ce92ce3ea74c37c5d359?value=2.014e18&amount=2.014"
            val url10 = "ethereum:0xfb6916095ca1df60bb79Ce92ce3ea74c37c5d359?value=2.014e18&amount=2.013"
            val url11 = "ethereum:0xA974c709cFb4566686553a20790685A47acEAA33@1/transfer?address=0xB38F2E40e82F0AE5613D55203d84953aE4d5181B&amount=1&uint256=1.24e18"

            // polygon usdc
            val url12 = "ethereum:0x2791Bca1f2de4661ED88A30C99A7a9449Aa84174@137/transfer?address=0x1DB766A18aB5b70A38e2A8a8819Ba6472029E9Ac&uint256=3.27e6&gas=250000"

            val url13 = "ethereum:0x752420f80e0A6158f06c00A864Ff220503EB502a?amount=68.255292&label=R2PY8P&uuid=UHTR42MTTBSORZEB&req-asset=0xdAC17F958D2ee523a2206206994597C13D831ec7"

            val result1 = parse(url1)
            val result2 = parse(url2)
            val result3 = parse(url3)
            val result4 = parse(url4)
            val result5 = parse(url5)
            val result6 = parse(url6)
            val result7 = parse(url7)
            val result8 = parse(url8)
            val result9 = parse(url9)
            val result10 = parse(url10)
            val result11 = parse(url11)
            val result12 = parse(url12)
            val result13 = parse(url13)

            assertTrue(result1 != null)
            if (result1 != null) {
                checkResult(result1, Constants.ChainId.ETHEREUM_CHAIN_ID, "0xfb6916095ca1df60bb79Ce92ce3ea74c37c5d359", "2.014")
            }
            assertTrue(result2 != null)
            if (result2 != null) {
                checkResult(result2, "4d8c508b-91c5-375b-92b0-ee702ed2dac5", "0x00d02d4A148bCcc66C6de20C4EB1CbAB4298cDcc", "20")
            }
            assertTrue(result3 != null)
            if (result3 != null) {
                checkResult(result3, Constants.ChainId.ETHEREUM_CHAIN_ID, "0xD994790d2905b073c438457c9b8933C0148862db", "0.01697")
            }
            assertTrue(result4 != null)
            if (result4 != null) {
                checkResult(result4, "c94ac88f-4671-3976-b60a-09064f1811e8", "0xB38F2E40e82F0AE5613D55203d84953aE4d5181B", "1.24")
            }
            assertTrue(result5 == null)
            assertTrue(result6 != null)
            if (result6 != null) {
                checkResult(result6, "c94ac88f-4671-3976-b60a-09064f1811e8", "0xB38F2E40e82F0AE5613D55203d84953aE4d5181B", "1.24")
            }
            assertTrue(result7 == null)
            assertTrue(result8 != null)
            if (result8 != null) {
                checkResult(result8, Constants.ChainId.ETHEREUM_CHAIN_ID, "0x20269e75b1637632e87f65A0A053d6720A781f39", "0.00016882")
            }
            assertTrue(result9 != null)
            if (result9 != null) {
                checkResult(result9, Constants.ChainId.ETHEREUM_CHAIN_ID, "0xfb6916095ca1df60bb79Ce92ce3ea74c37c5d359", "2.014")
            }
            assertTrue(result10 == null)
            assertTrue(result11 == null)

            assertTrue(result12 != null)
            if (result12 != null) {
                checkResult(result12, "80b65786-7c75-3523-bc03-fb25378eae41", "0x1DB766A18aB5b70A38e2A8a8819Ba6472029E9Ac", "3.27")
            }

            assertTrue(result13 != null)
            if (result13 != null) {
                checkResult(result13, "4d8c508b-91c5-375b-92b0-ee702ed2dac5", "0x752420f80e0A6158f06c00A864Ff220503EB502a", "68.255292")
            }
        }

    // @Test
    // fun testParseTron() = runBlocking {
    //     val url1 = "tron:TLVMBZbtv97N2R4zsKZjjcpG6ucxNUKs3p?amount=20.00"
    //
    //     val result1 = parse(url1)
    //
    //     assertTrue(result1 != null)
    // }

    @Test
    fun testParseLitecoin() =
        runBlocking {
            val url1 = "litecoin:MAA5rAYDJcfpGShL2fHHyqdH5Sum4hC9My?amount=0.31837321"
            val url2 = "litecoin:MAA5rAYDJcfpGShL2fHHyqdH5Sum4hC9My?amount=0.31e5"

            val result1 = parse(url1)
            val result2 = parse(url2)

            assertTrue(result1 != null)
            if (result1 != null) {
                checkResult(result1, Constants.ChainId.Litecoin, "MAA5rAYDJcfpGShL2fHHyqdH5Sum4hC9My", "0.31837321")
            }
            assertTrue(result2 == null)
        }

    @Test
    fun testParseDogecoin() =
        runBlocking {
            val url1 = "dogecoin:DQDHx7KcDjq1uDR5MC8tHQPiUp1C3eQHcd?amount=258.69"
            val url2 = "dogecoin:DQDHx7KcDjq1uDR5MC8tHQPiUp1C3eQHcd?amount=258.6e5"

            val result1 = parse(url1)
            val result2 = parse(url2)

            assertTrue(result1 != null)
            if (result1 != null) {
                checkResult(result1, Constants.ChainId.Dogecoin, "DQDHx7KcDjq1uDR5MC8tHQPiUp1C3eQHcd", "258.69")
            }
            assertTrue(result2 == null)
        }

    @Test
    fun testParseDash() =
        runBlocking {
            val url1 = "dash:XimNHukVq5PFRkadrwybyuppbree51mByS?amount=0.47098703&IS=1"
            val url2 = "dash:XimNHukVq5PFRkadrwybyuppbree51mByS?amount=0.47e5&IS=1"

            val result1 = parse(url1)
            val result2 = parse(url2)

            assertTrue(result1 != null)
            if (result1 != null) {
                checkResult(result1, Constants.ChainId.Dash, "XimNHukVq5PFRkadrwybyuppbree51mByS", "0.47098703")
            }
            assertTrue(result2 == null)
        }

    @Test
    fun testParseMonero() =
        runBlocking {
            val url1 = "monero:83sfoqWFNrsGTAyuC3PxHeS9stn8TQiTkiBcizHwjyHN57NczsRJE8UfrnhTUxT5PLBWLnq5yXrtPKeAjWeoDTkCPHGVe1Y?tx_amount=1.61861962"
            val url2 = "monero:83sfoqWFNrsGTAyuC3PxHeS9stn8TQiTkiBcizHwjyHN57NczsRJE8UfrnhTUxT5PLBWLnq5yXrtPKeAjWeoDTkCPHGVe1Y?tx_amount=1.61e6"

            val result1 = parse(url1)
            val result2 = parse(url2)

            assertTrue(result1 != null)
            if (result1 != null) {
                checkResult(result1, Constants.ChainId.Monero, "83sfoqWFNrsGTAyuC3PxHeS9stn8TQiTkiBcizHwjyHN57NczsRJE8UfrnhTUxT5PLBWLnq5yXrtPKeAjWeoDTkCPHGVe1Y", "1.61861962")
            }
            assertTrue(result2 == null)
        }

    @Test
    fun testParseSolana() =
        runBlocking {
            val url1 = "solana:mvines9iiHiQTysrwkJjGf2gb9Ex9jXJX8ns3qwf2kN?amount=1&label=Michael&message=Thanks%20for%20all%20the%20fish&memo=OrderId12345"
            val url2 = "solana:mvines9iiHiQTysrwkJjGf2gb9Ex9jXJX8ns3qwf2kN?amount=0.01&spl-token=EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"
            val url3 = "solana:mvines9iiHiQTysrwkJjGf2gb9Ex9jXJX8ns3qwf2kN?amount=1e7&label=Michael&message=Thanks%20for%20all%20the%20fish&memo=OrderId12345"

            val result1 = parse(url1)
            val result2 = parse(url2)
            val result3 = parse(url3)

            assertTrue(result1 != null)
            if (result1 != null) {
                checkResult(result1, Constants.ChainId.Solana, "mvines9iiHiQTysrwkJjGf2gb9Ex9jXJX8ns3qwf2kN", "1")
            }
            assertTrue(result2 == null)
            assertTrue(result3 == null)
        }

    private fun checkResult(
        result: ExternalTransfer,
        targetAssetId: String,
        targetDestination: String,
        targetAmount: String,
    ) {
        assertTrue(result.assetId == targetAssetId)
        assertTrue(result.destination == targetDestination)
        assertTrue(result.amount == targetAmount)
    }

    private suspend fun parse(url: String) =
        parseExternalTransferUri(
            url,
            { assetId, destination ->
                mockGetAddressResponse(assetId, destination)
            },
            { assetId, destination ->
                mockGetFeeResponse(assetId, destination)
            },
            { assetKey ->
                return@parseExternalTransferUri mockAssetKeyAssetId[assetKey]
            },
            { assetId ->
                return@parseExternalTransferUri AssetPrecision(assetId, Constants.ChainId.ETHEREUM_CHAIN_ID, mockAssetPrecision[assetId] ?: 0)
            },
        )

    private val mockGetAddressResponse: suspend (String, String) -> AddressResponse? = { assetId, destination ->
        AddressResponse(destination, null, assetId)
    }

    private val mockGetFeeResponse: suspend (String, String) -> List<WithdrawalResponse>? = { assetId, destination ->
        listOf(WithdrawalResponse(assetId, "0"))
    }

    private val mockAssetKeyAssetId =
        mapOf(
            "0xdac17f958d2ee523a2206206994597c13d831ec7" to "4d8c508b-91c5-375b-92b0-ee702ed2dac5", // ERC20 USDT
            "0xa974c709cfb4566686553a20790685a47aceaa33" to "c94ac88f-4671-3976-b60a-09064f1811e8", // XIN
            "0x2791bca1f2de4661ed88a30c99a7a9449aa84174" to "80b65786-7c75-3523-bc03-fb25378eae41", // Polygon USDC
        )

    private val mockAssetPrecision =
        mapOf(
            "4d8c508b-91c5-375b-92b0-ee702ed2dac5" to 6,
            "c94ac88f-4671-3976-b60a-09064f1811e8" to 18,
            "80b65786-7c75-3523-bc03-fb25378eae41" to 6,
        )
}
