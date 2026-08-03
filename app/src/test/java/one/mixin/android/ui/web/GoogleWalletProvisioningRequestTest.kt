package one.mixin.android.ui.web

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GoogleWalletProvisioningRequestTest {
    @Test
    fun `accepts the bounded opaque payload passed from a trusted web app`() {
        assertTrue(
            GoogleWalletProvisioningRequest(
                requestId = "request-1",
                opaquePaymentCard = "cGF5bG9hZA==",
                displayName = "Mixin Card",
                lastDigits = "1234",
            ).isValid(),
        )
    }

    @Test
    fun `rejects payloads that cannot be safely passed to the issuer sdk`() {
        assertFalse(
            GoogleWalletProvisioningRequest(
                requestId = "request-1",
                opaquePaymentCard = "payload",
                displayName = "Mixin Card",
                lastDigits = "12ab",
            ).isValid(),
        )
    }
}
