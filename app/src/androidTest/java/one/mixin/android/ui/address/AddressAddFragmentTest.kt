package one.mixin.android.ui.address

import android.content.Context
import android.content.Intent
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isFocused
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.launchFragmentInHiltContainer
import one.mixin.android.extension.withArgs
import one.mixin.android.extension.withTextColor
import one.mixin.android.mock.mockAsset
import one.mixin.android.mock.mockRippleAsset
import one.mixin.android.ui.TestRegistry
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.wallet.WalletRule
import one.mixin.android.util.EspressoIdlingResource
import one.mixin.android.util.decodeICAP
import one.mixin.android.util.isIcapAddress
import one.mixin.android.util.isKeyboardShown
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.toAssetItem
import org.hamcrest.core.IsNot.not
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class AddressAddFragmentTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val walletRule = WalletRule()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @Test
    fun testRippleDisplay() {
        testDisplayInternal(mockRippleAsset().toAssetItem())
    }

    @Test
    fun testDisplay() {
        testDisplayInternal(mockAsset().toAssetItem())
    }

    @Test
    fun testSave() {
        go2AddressAdd { navController, _ ->
            onView(withId(R.id.save_tv)).check(matches(not(isEnabled())))

            onView(withId(R.id.label_et)).perform(typeText("abc"))
            onView(withId(R.id.save_tv))
                .check(matches(not(isEnabled())))
                .check(matches(withTextColor(R.color.wallet_text_gray)))

            onView(withId(R.id.addr_et)).perform(typeText("abc"))
            onView(withId(R.id.save_tv))
                .check(matches(not(isEnabled())))
                .check(matches(withTextColor(R.color.wallet_text_gray)))

            onView(withId(R.id.info)).perform(scrollTo(), click())
            onView(withId(R.id.save_tv))
                .check(matches(isEnabled()))
                .check(matches(withTextColor(R.color.white)))

            onView(withId(R.id.info)).perform(scrollTo(), click())
            onView(withId(R.id.save_tv))
                .check(matches(not(isEnabled())))
                .check(matches(withTextColor(R.color.wallet_text_gray)))

            onView(withId(R.id.tag_et)).perform(typeText("abc"))
            onView(withId(R.id.save_tv))
                .check(matches(isEnabled()))
                .check(matches(withTextColor(R.color.white)))

            onView(withId(R.id.save_tv)).perform(scrollTo(), click())
            onView(withId(R.id.asset_address))
                .inRoot(RootMatchers.isDialog())
                .check(matches(ViewMatchers.isDisplayed()))
            onView(withId(R.id.right_iv)).perform(click())
            assertTrue(navController?.currentDestination?.id == R.id.address_add_fragment)
        }
    }

    @Test
    fun testGetScanResult() {
        val expectedResult = Intent().apply {
            putExtra(CaptureActivity.ARGS_FOR_SCAN_RESULT, "abc")
        }
        val testRegistry = TestRegistry(expectedResult)
        var isAddr = false
        launchFragmentInHiltContainer(
            R.style.AppTheme_NoActionBar,
            {
                AddressAddFragment(testRegistry).withArgs {
                    putParcelable(ARGS_ASSET, mockAsset().toAssetItem())
                }
            },
            {
                this.getScanResult.launch(Pair(CaptureActivity.ARGS_FOR_SCAN_RESULT, true))

                isAddr = this.isAddr
            }
        )

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

    private fun go2AddressAdd(action: (NavController?, ActivityScenario<WalletActivity>) -> Unit) {
        var navController: NavController? = null
        walletRule.activityScenario = ActivityScenario.launch(WalletActivity::class.java).onActivity {
            navController = it.navController
        }
        onView(withId(R.id.coins_rv))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))
        onView(withId(R.id.send_tv)).perform(click())
        onView(withId(R.id.send_cancel))
            .inRoot(RootMatchers.isDialog())
            .check(matches(ViewMatchers.isDisplayed()))
        onView(withId(R.id.address)).perform(click())
        onView(withId(R.id.right_animator)).perform(click())

        action.invoke(navController, walletRule.activityScenario)
    }

    private fun testDisplayInternal(asset: AssetItem) {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        go2AddressAdd { _, activityScenario ->
            activityScenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.first() as AddressAddFragment
                fragment.asset = asset
            }
            onView(withId(R.id.label_et))
                .check(matches(isFocused()))
                .check(matches(withHint(ctx.getString(R.string.withdrawal_label))))
                .check(matches(withText("")))
            onView(withId(R.id.tag_et))
                .check(
                    matches(
                        withHint(
                            ctx.getString(
                                if (asset.chainId == Constants.ChainId.RIPPLE_CHAIN_ID) {
                                    R.string.wallet_transfer_tag
                                } else {
                                    R.string.withdrawal_addr_memo_hint
                                }
                            )
                        )
                    )
                )
            assertTrue(isKeyboardShown())
        }
    }
}
