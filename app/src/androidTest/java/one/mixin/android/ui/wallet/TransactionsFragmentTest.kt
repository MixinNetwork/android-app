package one.mixin.android.ui.wallet

import android.content.Context
import android.widget.TextView
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.android.synthetic.main.item_wallet_transactions.view.*
import one.mixin.android.R
import one.mixin.android.util.EspressoIdlingResource
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.absoluteValue

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class TransactionsFragmentTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

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
            onView(withId(R.id.transactions_rv))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))
            assertTrue(navController?.currentDestination?.id == R.id.transaction_fragment)
            activityScenario.onActivity {
                navController?.navigateUp()
            }
        }
    }

    @Test
    fun testFilter() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        go2Transactions { navController, activityScenario ->
            // sort by amount
            onView(withId(R.id.group_info_member_title_sort)).perform(click())
            onView(withId(R.id.apply_tv))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
            onView(withId(R.id.sort_amount)).perform(click())
            onView(withId(R.id.apply_tv)).perform(click())
            assertTrue(navController?.currentDestination?.id == R.id.transactions_fragment)

            activityScenario.onActivity {
                val rv = it.findViewById<RecyclerView>(R.id.transactions_rv)
                val itemCount = rv.adapter?.itemCount ?: 1
                if (itemCount > 2) {
                    // ignore head
                    val first = rv.findViewHolderForAdapterPosition(1)
                    val second = rv.findViewHolderForAdapterPosition(2)
                    val firstAmount = first?.itemView?.findViewById<TextView>(R.id.value)?.text?.toString()?.toDoubleOrNull()?.absoluteValue
                    val secondAmount = second?.itemView?.findViewById<TextView>(R.id.value)?.text?.toString()?.toDoubleOrNull()?.absoluteValue
                    if (firstAmount != null && secondAmount != null) {
                        assertTrue(firstAmount >= secondAmount)
                    }
                }
            }

            // filter by transfer
            onView(withId(R.id.group_info_member_title_sort)).perform(click())
            onView(withId(R.id.apply_tv))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
            onView(withId(R.id.filters_radio_transfer)).perform(click())
            onView(withId(R.id.apply_tv)).perform(click())
            assertTrue(navController?.currentDestination?.id == R.id.transactions_fragment)
            // val name = ctx.getString(R.string.filters_transfer)
            // onView(allOf(withId(R.id.name)))
            //     .check(matches(withText(name)))
        }
    }

    private fun go2Transactions(action: (NavController?, ActivityScenario<WalletActivity>) -> Unit) {
        var navController: NavController? = null
        val activityScenario = ActivityScenario.launch(WalletActivity::class.java).onActivity {
            navController = it.navController
        }
        onView(withId(R.id.coins_rv))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        action.invoke(navController, activityScenario)

        activityScenario.close()
    }
}
