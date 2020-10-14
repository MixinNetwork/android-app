package one.mixin.android.ui.address

import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
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
import org.junit.Test

class AddressAddFragmentTest {

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
