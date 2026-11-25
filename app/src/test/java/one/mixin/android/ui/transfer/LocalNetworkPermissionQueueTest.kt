package one.mixin.android.ui.transfer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalNetworkPermissionQueueTest {
    @Test
    fun cancelRemovesPendingDesktopCommandBeforePermissionGrant() {
        val queue = LocalNetworkPermissionQueue()
        var ran = false

        assertTrue(queue.enqueue(cancellableByDesktop = true) { ran = true })
        assertTrue(queue.cancelPendingDesktopCommands())
        assertFalse(queue.cancelPendingDesktopCommands())

        queue.onPermissionResult(granted = true)
        assertFalse(ran)
    }

    @Test
    fun cancelPreservesNonDesktopPendingActionAndNewDesktopCommandAfterCancelRuns() {
        val queue = LocalNetworkPermissionQueue()
        val executed = mutableListOf<String>()

        assertTrue(queue.enqueue(cancellableByDesktop = false) { executed.add("manual") })
        assertFalse(queue.enqueue(cancellableByDesktop = true) { executed.add("old-desktop") })
        assertTrue(queue.cancelPendingDesktopCommands())
        assertFalse(queue.enqueue(cancellableByDesktop = true) { executed.add("new-desktop") })

        queue.onPermissionResult(granted = true)
        assertEquals(listOf("manual", "new-desktop"), executed)
    }

    @Test
    fun deniedPermissionClearsPendingActionsAndAllowsRetry() {
        val queue = LocalNetworkPermissionQueue()
        var firstRan = false
        var retryRan = false

        assertTrue(queue.enqueue(cancellableByDesktop = true) { firstRan = true })
        queue.onPermissionResult(granted = false)
        assertFalse(firstRan)

        assertTrue(queue.enqueue(cancellableByDesktop = true) { retryRan = true })
        queue.onPermissionResult(granted = true)
        assertTrue(retryRan)
    }
}
