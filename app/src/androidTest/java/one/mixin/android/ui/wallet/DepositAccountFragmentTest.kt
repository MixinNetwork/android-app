package one.mixin.android.ui.wallet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.android.synthetic.main.view_round_title.view.*
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.getTipsByAsset
import one.mixin.android.vo.needShowReserve
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class DepositAccountFragmentTest : DepositFragmentTest() {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testShowDepositBottom() {
        go2Deposit(true) { _, _ ->
            showDepositTipBottom()
        }
    }

    @Test
    fun testDisplay() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        go2Deposit(true) { _, activityScenario ->
            closeTipBottom()

            var fragment: DepositPublicKeyFragment? = null
            activityScenario.onActivity { activity ->
                fragment = activity.supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.first() as DepositPublicKeyFragment
            }
            val asset = fragment!!.asset

            onView(withId(R.id.title_tv))
                .check(matches(withText(ctx.getString(R.string.filters_deposit))))
            onView(withId(R.id.sub_title_tv))
                .check(matches(withText(asset.symbol)))

            val tips: String = fragment!!.getTipsByAsset(asset) + " " + ctx.getString(R.string.deposit_confirmation, asset.confirmations)
            onView(withId(R.id.confirm_tv))
                .check(matches(withText(tips)))

            val reserveTip = if (asset.needShowReserve()) {
                ctx.getString(R.string.deposit_reserve, asset.reserve, asset.symbol)
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
        }
    }

    @Test
    fun testClickImage() {
        go2Deposit(true) { _, _ ->
            closeTipBottom()

            onView(withId(R.id.account_name_qr)).perform(click())
            onView(withId(R.id.content_ll))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))

            onView(withId(R.id.right_iv)).perform(click())

            onView(withId(R.id.account_memo_qr)).perform(click())
            onView(withId(R.id.content_ll))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
        }
    }
}
