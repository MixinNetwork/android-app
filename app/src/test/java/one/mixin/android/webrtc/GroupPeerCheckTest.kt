package one.mixin.android.webrtc

import android.app.BackgroundServiceStartNotAllowedException
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class GroupPeerCheckTest {
    @Test
    @Config(sdk = [28])
    fun skipsLegacyBackgroundStartDenial() {
        val error = IllegalStateException("Not allowed to start service Intent { act=check_peer }: app is in background uid UidRecord")
        checkPeers(rejectingContext(error), "group")
    }

    @Test
    @Config(sdk = [31])
    fun skipsBackgroundStartDenial() {
        checkPeers(rejectingContext(BackgroundServiceStartNotAllowedException("denied")), "group")
    }

    @Test
    @Config(sdk = [28, 31])
    fun propagatesUnrelatedStartFailure() {
        val error = IllegalStateException("unrelated service error")
        assertSame(error, assertThrows(IllegalStateException::class.java) { checkPeers(rejectingContext(error), "group") })
    }

    private fun rejectingContext(error: IllegalStateException) =
        object : ContextWrapper(ApplicationProvider.getApplicationContext<Context>()) {
            override fun startService(service: Intent): ComponentName? = throw error
        }
}
