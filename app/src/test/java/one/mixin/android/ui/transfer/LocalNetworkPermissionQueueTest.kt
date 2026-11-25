package one.mixin.android.ui.transfer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalNetworkPermissionQueueTest {
    @Test
    fun cancelRemovesPendingDesktopCommandBeforePermissionGrant() {
        val queue = LocalNetworkPermissionQueue()
        for (granted in listOf(true, false)) {
            var denied = false
            var executed = false
            assertTrue(queue.enqueue(cancellableByDesktop = true, onDenied = { denied = true }) { executed = true })
            assertTrue(queue.cancelPendingDesktopCommands())
            assertFalse(queue.cancelPendingDesktopCommands())
            assertFalse(queue.onPermissionResult(granted))
            assertFalse(executed)
            assertFalse(denied)
        }
    }

    @Test
    fun cancelPreservesNonDesktopPendingActionAndNewDesktopCommandAfterCancelRuns() {
        val queue = LocalNetworkPermissionQueue()
        val executed = mutableListOf<String>()
        assertTrue(queue.enqueue { executed.add("manual") })
        assertFalse(queue.enqueue(cancellableByDesktop = true) { executed.add("old-desktop") })
        assertTrue(queue.cancelPendingDesktopCommands())
        assertFalse(queue.enqueue(cancellableByDesktop = true) { executed.add("new-desktop") })
        assertTrue(queue.onPermissionResult(granted = true))
        assertEquals(listOf("manual", "new-desktop"), executed)
    }

    @Test
    fun deniedPermissionInvokesOnDeniedClearsQueueAndAllowsRetry() {
        val queue = LocalNetworkPermissionQueue()
        var denied = false
        var executed = false
        assertTrue(queue.enqueue(onDenied = { denied = true }) { executed = true })
        assertTrue(queue.onPermissionResult(granted = false))
        assertTrue(denied)
        assertFalse(executed)

        var retried = false
        assertTrue(queue.enqueue { retried = true })
        assertTrue(queue.onPermissionResult(granted = true))
        assertTrue(retried)
        assertFalse(executed)
    }

    @Test
    fun permissionErrorInvokesOnDeniedAndAllowsRetry() {
        val queue = LocalNetworkPermissionQueue()
        var denied = false
        var executed = false
        assertTrue(queue.enqueue(onDenied = { denied = true }) { executed = true })
        queue.onPermissionError()
        assertTrue(denied)
        assertFalse(executed)

        var retried = false
        assertTrue(queue.enqueue { retried = true })
        assertTrue(queue.onPermissionResult(granted = true))
        assertTrue(retried)
        assertFalse(executed)
    }

    @Test
    fun clearDropsPendingActionsWithoutInvokingCallbacks() {
        val queue = LocalNetworkPermissionQueue()
        var denied = false
        var executed = false
        assertTrue(queue.enqueue(onDenied = { denied = true }) { executed = true })
        queue.clear()
        assertFalse(queue.onPermissionResult(granted = true))
        assertFalse(executed)
        assertFalse(denied)
        assertTrue(queue.enqueue { })
    }

    @Test
    fun desktopCancelDropsCancellableStartActionBeforePermissionGrant() {
        val queue = LocalNetworkPermissionQueue()
        var started = false
        assertTrue(queue.enqueue(cancellableByDesktop = true) { started = true })
        assertTrue(queue.cancelPendingDesktopCommands())
        assertFalse(queue.onPermissionResult(granted = true))
        assertFalse(started)
    }
}
