package one.mixin.android.ui.address

import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import one.mixin.android.R
import one.mixin.android.extension.withArgs
import one.mixin.android.launchFragmentInHiltContainer
import one.mixin.android.mock.mockAsset
import one.mixin.android.ui.TestRegistry
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.decodeICAP
import one.mixin.android.util.isIcapAddress
import one.mixin.android.vo.toAssetItem
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class AddressAddFragmentTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testGetScanResult() {
        val expectedResult = Intent().apply {
            putExtra(CaptureActivity.ARGS_FOR_SCAN_RESULT, "abc")
        }
        val testRegistry = TestRegistry(expectedResult)
        var isAddr = false
        val fragment = AddressAddFragment(testRegistry).withArgs {
            putParcelable(ARGS_ASSET, mockAsset().toAssetItem())
        }
        launchFragmentInHiltContainer(fragment, R.style.AppTheme_NoActionBar) {
            this.getScanResult.launch(Pair(CaptureActivity.ARGS_FOR_SCAN_RESULT, true))

            isAddr = this.isAddr
        }

        if (isAddr) {
            val expect = if (isIcapAddress("abc")) {
                decodeICAP("abc")
            } else {
                "abc"
            }
            onView(withId(R.id.addr_et)).check(matches(withText(expect)))
        } else {
            onView(withId(R.id.tag_et)).check(matches(withText("abc")))
        }
    }
}
