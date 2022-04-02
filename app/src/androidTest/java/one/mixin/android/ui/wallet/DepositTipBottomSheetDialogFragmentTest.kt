package one.mixin.android.ui.wallet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.getTipsByAsset
import one.mixin.android.extension.launchFragmentInHiltContainer
import one.mixin.android.mock.mockAssetWithDestinationAndTag
import one.mixin.android.util.waitMillis
import one.mixin.android.vo.needShowReserve
import one.mixin.android.vo.toAssetItem
import org.hamcrest.core.IsNot.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class DepositTipBottomSheetDialogFragmentTest : DepositFragmentTest() {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testDisplay() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val asset = mockAssetWithDestinationAndTag().toAssetItem()
        val fragment = DepositTipBottomSheetDialogFragment.newInstance(asset)
        var tips: String? = null
        launchFragmentInHiltContainer(fragment, R.style.AppTheme_NoActionBar) {
            tips = getTipsByAsset(asset) + " " + requireContext().resources.getQuantityString(R.plurals.deposit_confirmation, asset.confirmations, asset.confirmations)
        }

        onView(withId(R.id.continue_tv))
            .check(matches(not(isEnabled())))

        val titleText = ctx.getString(R.string.bottom_deposit_title, asset.symbol)
        onView(withId(R.id.title_tv))
            .check(matches(withText(titleText)))

        onView(withId(R.id.tips_tv))
            .check(matches(withText(tips)))

        val reserveTip = if (asset.needShowReserve()) {
            ctx.getString(R.string.deposit_reserve, "${asset.reserve} ${asset.symbol}")
        } else ""
        val warningText = when (asset.chainId) {
            Constants.ChainId.EOS_CHAIN_ID -> {
                "${ctx.getString(R.string.deposit_account_attention, asset.symbol)} $reserveTip"
            }
            else -> {
                "${ctx.getString(R.string.deposit_attention)} $reserveTip"
            }
        }
        onView(withId(R.id.warning_tv))
            .check(matches(withText(warningText)))

        // TODO simulate click outside

        waitMillis(3000)
        onView(withId(R.id.continue_tv))
            .check(matches(isEnabled()))
    }
}
