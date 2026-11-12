package one.mixin.android.extension

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.AudioDeviceInfoBuilder
import org.robolectric.shadows.ShadowAudioManager
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 35], shadows = [AudioSwitchDeviceQueryTest.CountingAudioManager::class])
class AudioSwitchDeviceQueryTest {
    @Test
    fun refreshQueriesEachDeviceListOnceAndKeepsBluetoothPriority() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(AudioManager::class.java)
        val audio = Shadow.extract<CountingAudioManager>(manager)
        val speaker = AudioDeviceInfoBuilder.newBuilder().setType(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER).build()
        val bluetooth = AudioDeviceInfoBuilder.newBuilder().setType(AudioDeviceInfo.TYPE_BLUETOOTH_SCO).build()
        ReflectionHelpers.setField(ReflectionHelpers.getField<Any>(bluetooth, "mPort"), "mName", "Test headset")
        audio.setOutputDevices(listOf(speaker))
        audio.setAvailableCommunicationDevices(listOf(speaker, bluetooth))
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_TELEPHONY, true)
        val audioSwitch = AudioSwitch(context)

        try {
            audioSwitch.start()
            audio.outputQueries = 0
            audio.communicationQueries = 0
            ReflectionHelpers.callInstanceMethod<Void>(audioSwitch, "refreshDevices")

            assertEquals(1, audio.outputQueries)
            assertEquals(1, audio.communicationQueries)
            assertTrue(audioSwitch.selectedAudioDevice is AudioDevice.BluetoothHeadset)
            assertTrue(audioSwitch.availableAudioDevices.any { it is AudioDevice.Speakerphone })
            assertTrue(audioSwitch.availableAudioDevices.any { it is AudioDevice.Earpiece })

            audioSwitch.start()
            assertEquals(1, audio.outputQueries)
            assertEquals(1, audio.communicationQueries)
        } finally {
            audioSwitch.stop()
        }
    }

    @Test
    fun restartUsesCurrentDevicesAndWiredHeadsetHidesEarpiece() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val audio = Shadow.extract<CountingAudioManager>(context.getSystemService(AudioManager::class.java))
        val speaker = AudioDeviceInfoBuilder.newBuilder().setType(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER).build()
        val wired = AudioDeviceInfoBuilder.newBuilder().setType(AudioDeviceInfo.TYPE_WIRED_HEADSET).build()
        audio.setOutputDevices(listOf(speaker, wired))
        audio.setAvailableCommunicationDevices(emptyList())
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_TELEPHONY, true)
        val audioSwitch = AudioSwitch(context)

        try {
            audioSwitch.start()
            assertTrue(audioSwitch.selectedAudioDevice is AudioDevice.WiredHeadset)
            assertFalse(audioSwitch.availableAudioDevices.any { it is AudioDevice.Earpiece })
            audioSwitch.stop()

            audio.setOutputDevices(listOf(speaker))
            audioSwitch.start()
            assertTrue(audioSwitch.selectedAudioDevice is AudioDevice.Earpiece)
            assertFalse(audioSwitch.availableAudioDevices.any { it is AudioDevice.WiredHeadset })
        } finally {
            audioSwitch.stop()
        }
    }

    @Implements(AudioManager::class)
    class CountingAudioManager : ShadowAudioManager() {
        var outputQueries = 0
        var communicationQueries = 0

        @Implementation
        override fun getDevices(flags: Int): Array<AudioDeviceInfo> {
            outputQueries++
            return super.getDevices(flags)
        }

        @Implementation(minSdk = 31)
        public override fun getAvailableCommunicationDevices(): List<AudioDeviceInfo> {
            communicationQueries++
            return super.getAvailableCommunicationDevices()
        }
    }
}
