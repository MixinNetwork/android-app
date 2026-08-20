package one.mixin.android.web3.js

import org.junit.Assert.assertEquals
import org.junit.Test

class Web3SignerNetworkTest {
    @Test
    fun pearlUsesStableNetworkName() {
        assertEquals("pearl", Web3Signer.JsSignerNetwork.Pearl.name)
    }
}
