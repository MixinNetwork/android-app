package one.mixin.android.ui.conversation

import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import one.mixin.android.R
import one.mixin.android.launchFragmentInHiltContainer
import one.mixin.android.ui.TestRegistry
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_FOR_SCAN_RESULT
import org.junit.Test

class TransferFragmentTest {

    @Test
    fun testGetScanResult() {
        val expectedResult = Intent().apply {
            putExtra(ARGS_FOR_SCAN_RESULT, "abc")
        }
        val testRegistry = TestRegistry(expectedResult)
        launchFragmentInHiltContainer(TransferFragment.newInstance(testRegistry), R.style.AppTheme_NoActionBar) {
            this.getScanResult.launch(Pair(ARGS_FOR_SCAN_RESULT, true))
        }
        onView(withId(R.id.transfer_memo)).check(matches(withText("abc")))
    }
}
