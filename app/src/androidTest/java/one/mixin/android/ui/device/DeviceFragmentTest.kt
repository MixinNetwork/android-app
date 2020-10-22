package one.mixin.android.ui.device

import android.content.Intent
import one.mixin.android.R
import one.mixin.android.launchFragmentInHiltContainer
import one.mixin.android.ui.TestRegistry
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_FOR_SCAN_RESULT
import org.junit.Test

class DeviceFragmentTest {

    @Test
    fun testGetScanResult() {
        val expectedResult = Intent().apply {
            putExtra(ARGS_FOR_SCAN_RESULT, "abc")
        }
        val testRegistry = TestRegistry(expectedResult)
        launchFragmentInHiltContainer(DeviceFragment(testRegistry), R.style.AppTheme_NoActionBar) {
            this.getScanResult.launch(Pair(ARGS_FOR_SCAN_RESULT, true))

            assert(this.scanResult == expectedResult.getStringExtra(ARGS_FOR_SCAN_RESULT))
        }
    }
}
