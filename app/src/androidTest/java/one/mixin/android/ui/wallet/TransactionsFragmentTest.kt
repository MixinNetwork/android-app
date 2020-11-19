package one.mixin.android.ui.wallet

import android.content.Context
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import one.mixin.android.R
import one.mixin.android.extension.getRVCount
import one.mixin.android.mock.mockAsset
import one.mixin.android.mock.mockAssetWithDestination
import one.mixin.android.mock.mockAssetWithDestinationAndTag
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.toAssetItem
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsNot.not
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class TransactionsFragmentTest : BaseTransactionsFragmentTest() {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testShowAssetKeyBottom() {
        go2Transactions { _, _ ->
            onView(withId(R.id.top_rl))
                .perform(click())

            onView(withId(R.id.asset_key_tv))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testMore() {
        go2Transactions { navController, _ ->
            // cancel
            onView(withId(R.id.right_animator)).perform(click())
            onView(withId(R.id.cancel))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
            onView(withId(R.id.cancel)).perform(click())
            assertTrue(navController?.currentDestination?.id == R.id.transactions_fragment)

            // hide
            onView(withId(R.id.right_animator))
                .perform(click())

            onView(withId(R.id.hide))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))

            onView(withId(R.id.hide)).perform(click())
            assertTrue(navController?.currentDestination?.id == R.id.wallet_fragment)
        }
    }

    @Test
    fun testTransactionsRv() {
        go2Transactions { navController, activityScenario ->
            // open first transaction item
            val itemCount = activityScenario.getRVCount(R.id.transactions_rv)
            if (itemCount < 1) return@go2Transactions

            onView(withId(R.id.transactions_rv))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))
            assertTrue(navController?.currentDestination?.id == R.id.transaction_fragment)
            activityScenario.onActivity {
                navController?.navigateUp()
            }
        }
    }

    @Test
    fun testOpenTransferOut() {
        go2Transactions { navController, activityScenario ->
            // open send contact
            openSendBottom(navController, R.id.contact, R.id.single_friend_select_fragment)
            activityScenario.onActivity {
                navController?.navigateUp()
            }

            // open send contact
            openSendBottom(navController, R.id.address, R.id.address_management_fragment)
            activityScenario.onActivity {
                navController?.navigateUp()
            }

            // cancel send bottom
            openSendBottom(navController, R.id.send_cancel, R.id.transactions_fragment)
        }
    }

    @Test
    fun testOpenDepositAccount() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        go2Transactions { navController, activityScenario ->
            var fragment: TransactionsFragment? = null
            activityScenario.onActivity { activity ->
                fragment = activity.supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.first() as TransactionsFragment
            }

            fragment?.asset = mockAsset().toAssetItem()
            onView(withId(R.id.receive_tv)).perform(click())
            onView(withText(ctx.getString(R.string.error_bad_data, ErrorHandler.BAD_DATA)))
                .inRoot(withDecorView(not(`is`(fragment?.activity?.window?.decorView))))
                .check(matches(isDisplayed()))

            fragment?.asset = mockAssetWithDestinationAndTag().toAssetItem()
            onView(withId(R.id.receive_tv)).perform(click())
            assertTrue(navController?.currentDestination?.id == R.id.deposit_account_fragment)
        }
    }

    @Test
    fun testOpenDepositPublicKey() {
        go2Transactions { navController, activityScenario ->
            var fragment: TransactionsFragment? = null
            activityScenario.onActivity { activity ->
                fragment = activity.supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.first() as TransactionsFragment
            }

            fragment?.asset = mockAssetWithDestination().toAssetItem()
            onView(withId(R.id.receive_tv)).perform(click())
            assertTrue(navController?.currentDestination?.id == R.id.deposit_public_key_fragment)
        }
    }

    private fun openSendBottom(
        navController: NavController?,
        clickId: Int,
        expectFragmentId: Int,
    ) {
        onView(withId(R.id.send_tv)).perform(click())
        onView(withId(R.id.send_cancel))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withId(clickId)).perform(click())
        assertTrue(navController?.currentDestination?.id == expectFragmentId)
    }

    override fun go2Transactions(action: (NavController?, ActivityScenario<WalletActivity>) -> Unit) {
        var navController: NavController? = null
        val activityScenario = ActivityScenario.launch(WalletActivity::class.java).onActivity {
            navController = it.navController
        }
        onView(withId(R.id.coins_rv))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        action.invoke(navController, activityScenario)

        activityScenario.close()
    }

    override fun isAllTransactions() = false
}
