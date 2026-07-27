package one.mixin.android.ui.landing

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginPinGateCoordinatorTest {
    @Test
    fun verifiedResultContinuesInProvidedOwnerScope() = runBlocking {
        val routedPin = CompletableDeferred<String?>()
        val finished = CompletableDeferred<Unit>()
        val callback = loginPinGateDismissCallback(
            ownerScope = this,
            openNext = { pin ->
                routedPin.complete(pin)
                true
            },
            finish = { finished.complete(Unit) },
        )

        callback(true, "123456")

        assertEquals("123456", routedPin.await())
        finished.await()
    }

    @Test
    fun failedResultDoesNotContinue() = runBlocking {
        var routed = false
        var finished = false
        val callback = loginPinGateDismissCallback(
            ownerScope = this,
            openNext = {
                routed = true
                true
            },
            finish = { finished = true },
        )

        callback(false, null)
        yield()

        assertFalse(routed)
        assertFalse(finished)
    }

    @Test
    fun blockedNextStepDoesNotFinishOwner() = runBlocking {
        var finished = false
        val callback = loginPinGateDismissCallback(
            ownerScope = this,
            openNext = { false },
            finish = { finished = true },
        )

        callback(true, "123456")
        yield()

        assertFalse(finished)
    }

    @Test
    fun restoredDialogIsReboundWithoutCreatingAnotherDialog() {
        val existing = Any()
        var bound: Any? = null
        var created = false
        var shown = false

        val result = reuseOrCreateLoginPinGate(
            existing = existing,
            create = {
                created = true
                Any()
            },
            bind = { bound = it },
            show = { shown = true },
        )

        assertSame(existing, result)
        assertSame(existing, bound)
        assertFalse(created)
        assertFalse(shown)
    }

    @Test
    fun missingDialogIsCreatedBoundAndShownOnce() {
        val createdDialog = Any()
        var bound: Any? = null
        var shown: Any? = null

        val result = reuseOrCreateLoginPinGate(
            existing = null,
            create = { createdDialog },
            bind = { bound = it },
            show = { shown = it },
        )

        assertSame(createdDialog, result)
        assertSame(createdDialog, bound)
        assertSame(createdDialog, shown)
        assertTrue(result === createdDialog)
    }
}
