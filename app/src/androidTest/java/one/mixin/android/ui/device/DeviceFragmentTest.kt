package one.mixin.android.ui.device

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import one.mixin.android.R
import one.mixin.android.launchFragmentInHiltContainer
import one.mixin.android.ui.TestRegistry
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_FOR_SCAN_RESULT
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class DeviceFragmentTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testGetScanResult() {
        val expectedResult = Intent().apply {
            putExtra(ARGS_FOR_SCAN_RESULT, "abc")
        }
        val testRegistry = TestRegistry(expectedResult)
        launchFragmentInHiltContainer(DeviceFragment(testRegistry), R.style.AppTheme_NoActionBar) {
            this.getScanResult.launch(Pair(ARGS_FOR_SCAN_RESULT, true))

            assertTrue(this.scanResult == expectedResult.getStringExtra(ARGS_FOR_SCAN_RESULT))
        }
    }
}
