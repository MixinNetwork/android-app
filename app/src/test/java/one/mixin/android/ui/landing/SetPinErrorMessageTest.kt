package one.mixin.android.ui.landing

import kotlin.test.Test
import kotlin.test.assertEquals

class SetPinErrorMessageTest {
    @Test
    fun technicalErrorKeepsSummaryAndDropsStackTrace() {
        val message =
            """
            Set or update PIN failed
            java.lang.IllegalArgumentException: required oldPin can not be null
                at one.mixin.android.ui.tip.TipFlowInteractor.process(r8-map-id-c64e16044f75583ed76647c657465991d5ea4242af7c39ecafdd2fe8481b2cde:24)
                at one.mixin.android.ui.landing.SetupPinViewModel${'$'}executeCreatePin${'$'}1.invokeSuspend(r8-map-id-c64e16044f75583ed76647c657465991d5ea4242af7c39ecafdd2fe8481b2cde:108)
            """.trimIndent()

        assertEquals(
            "Set or update PIN failed\njava.lang.IllegalArgumentException: required oldPin can not be null",
            message.toSetPinErrorMessage(
                fallbackMessage = "Failed to set up PIN. Please check your network connection and try again.",
            ),
        )
    }

    @Test
    fun readableErrorIsKept() {
        assertEquals(
            "PIN incorrect",
            "PIN incorrect".toSetPinErrorMessage(
                fallbackMessage = "Failed to set up PIN. Please check your network connection and try again.",
            ),
        )
    }

    @Test
    fun coroutineExceptionSummaryIsKept() {
        assertEquals(
            "Set or update PIN failed kotlinx.coroutines.JobCancellationException",
            "Set or update PIN failed kotlinx.coroutines.JobCancellationException".toSetPinErrorMessage(
                fallbackMessage = "Failed to set up PIN. Please check your network connection and try again.",
            ),
        )
    }

    @Test
    fun inlineStackTraceIsDropped() {
        val message =
            "Set or update PIN failed java.io.IOException: net::ERR_NAME_NOT_RESOLVED " +
                "at org.bitcoinj.script.ScriptBuilder.getResponse(r8-map-id-c64e16044f75583ed76647c657465991d5ea4242af7c39ecafdd2fe8481b2cde:164)"

        assertEquals(
            "Set or update PIN failed java.io.IOException: net::ERR_NAME_NOT_RESOLVED",
            message.toSetPinErrorMessage(
                fallbackMessage = "Failed to set up PIN. Please check your network connection and try again.",
            ),
        )
    }
}
