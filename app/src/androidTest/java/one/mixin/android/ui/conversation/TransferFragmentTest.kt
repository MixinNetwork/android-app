package one.mixin.android.ui.conversation

import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import one.mixin.android.R
import one.mixin.android.extension.launchFragmentInHiltContainer
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.TestRegistry
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_FOR_SCAN_RESULT
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class TransferFragmentTest {

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
        launchFragmentInHiltContainer(TransferFragment.newInstance(testRegistry).withArgs { }, R.style.AppTheme_NoActionBar) {
            this.getScanResult.launch(Pair(ARGS_FOR_SCAN_RESULT, true))
        }
        onView(withId(R.id.transfer_memo)).check(matches(withText("abc")))
    }
}
