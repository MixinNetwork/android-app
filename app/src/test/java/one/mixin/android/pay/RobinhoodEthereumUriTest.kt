package one.mixin.android.pay

import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.api.response.AddressResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RobinhoodEthereumUriTest {
    @Test
    fun erc681ChainIdResolvesToRobinhood() = runBlocking {
        val destination = "0x1111111111111111111111111111111111111111"
        var validatedChainId: String? = null

        val transfer = parseEthereum(
            url = "ethereum:$destination@4663",
            validateAddress = { assetId, chainId, address ->
                validatedChainId = chainId
                AddressResponse(address, assetId = assetId)
            },
            getFee = { _, _ -> emptyList() },
            findAssetIdByAssetKey = { null },
            getAssetPrecisionById = { null },
            balanceCheck = { _, _, _, _ -> },
        )

        assertNotNull(transfer)
        assertEquals(Constants.ChainId.Robinhood, validatedChainId)
        assertEquals(Constants.ChainId.Robinhood, transfer?.assetId)
    }
}
