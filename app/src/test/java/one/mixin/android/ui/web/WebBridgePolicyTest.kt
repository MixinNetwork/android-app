package one.mixin.android.ui.web

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebBridgePolicyTest {
    @Test
    fun genericWebContentDoesNotReceiveNativeBridge() {
        val policy = webBridgePolicy(trustedAppUrl = false, dappBrowser = false)
        assertFalse(policy.mixinContext)
        assertFalse(policy.wallet)
    }

    @Test
    fun trustedAppReceivesBothBridges() {
        val policy = webBridgePolicy(trustedAppUrl = true, dappBrowser = false)
        assertTrue(policy.mixinContext)
        assertTrue(policy.wallet)
    }

    @Test
    fun dappBrowserReceivesOnlyWalletBridge() {
        val policy = webBridgePolicy(trustedAppUrl = false, dappBrowser = true)
        assertFalse(policy.mixinContext)
        assertTrue(policy.wallet)
    }
}
