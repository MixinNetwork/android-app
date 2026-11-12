package one.mixin.android.webrtc

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowActivityManager

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [CallServiceQueryTest.CountingActivityManager::class])
class CallServiceQueryTest {
    @Test
    fun noCallChecksRunningServicesOnlyOnce() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = Shadow.extract<CountingActivityManager>(context.getSystemService(ActivityManager::class.java))
        manager.setServices(emptyList())

        assertFalse(anyCallServiceRunning(context))
        assertEquals(1, manager.serviceQueries)
    }

    @Test
    fun detectsEitherCallServiceWithOneQuery() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = Shadow.extract<CountingActivityManager>(context.getSystemService(ActivityManager::class.java))
        for (type in listOf(VoiceCallService::class.java, GroupCallService::class.java)) {
            manager.setServices(listOf(ActivityManager.RunningServiceInfo().apply {
                service = ComponentName(context, type)
            }))
            manager.serviceQueries = 0

            assertTrue(anyCallServiceRunning(context))
            assertEquals(1, manager.serviceQueries)
        }
    }

    @Implements(ActivityManager::class)
    class CountingActivityManager : ShadowActivityManager() {
        var serviceQueries = 0

        @Implementation
        public override fun getRunningServices(maxNum: Int): List<ActivityManager.RunningServiceInfo> {
            serviceQueries++
            return super.getRunningServices(maxNum)
        }
    }
}
