package one.mixin.android.extension

import android.app.Application
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [33])
class LifecycleExtensionTest {
    @Test
    fun stoppingCancelsOnceWithoutRestartingTheRequest() {
        val owner = TestOwner()
        owner.lifecycleScope
        idle()
        val observers = owner.lifecycle.observerCount
        val response = CompletableDeferred<Unit>()
        var requests = 0
        var completed = false
        var cleanedUp = false
        val job = owner.launchUntilStop {
            requests++
            try {
                response.await()
                completed = true
            } finally {
                cleanedUp = true
            }
        }
        idle()
        assertEquals(1, requests)
        owner.lifecycle.currentState = Lifecycle.State.CREATED
        idle()
        assertTrue(job.isCancelled)
        assertTrue(cleanedUp)
        assertEquals(observers, owner.lifecycle.observerCount)
        owner.lifecycle.currentState = Lifecycle.State.STARTED
        response.complete(Unit)
        idle()
        assertEquals(1, requests)
        assertFalse(completed)
        owner.lifecycle.currentState = Lifecycle.State.DESTROYED
    }

    @Test
    fun completedRequestsRemoveTheirStopObservers() {
        val owner = TestOwner()
        owner.lifecycleScope
        idle()
        val baseline = owner.lifecycle.observerCount
        val response = CompletableDeferred<Unit>()
        val job = owner.launchUntilStop { response.await() }
        idle()
        assertEquals(baseline + 1, owner.lifecycle.observerCount)
        response.complete(Unit)
        idle()
        assertTrue(job.isCompleted)
        assertEquals(baseline, owner.lifecycle.observerCount)
        owner.lifecycle.currentState = Lifecycle.State.DESTROYED
    }

    @Test
    fun destroyingViewOwnerCancelsPendingRequests() {
        val owner = TestOwner()
        val response = CompletableDeferred<Unit>()
        var completed = false
        val job = owner.launchUntilStop {
            response.await()
            completed = true
        }
        idle()
        owner.lifecycle.currentState = Lifecycle.State.DESTROYED
        idle()
        response.complete(Unit)
        idle()
        assertTrue(job.isCancelled)
        assertFalse(completed)
        assertEquals(0, owner.lifecycle.observerCount)
    }

    @Test
    fun destroyedOwnerDoesNotStartRequests() {
        val owner = TestOwner()
        owner.lifecycle.currentState = Lifecycle.State.DESTROYED
        var requested = false
        val job = owner.launchUntilStop { requested = true }
        idle()
        assertTrue(job.isCancelled)
        assertFalse(requested)
        assertEquals(0, owner.lifecycle.observerCount)
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private class TestOwner : LifecycleOwner {
        override val lifecycle = LifecycleRegistry(this).apply {
            currentState = Lifecycle.State.STARTED
        }
    }
}
