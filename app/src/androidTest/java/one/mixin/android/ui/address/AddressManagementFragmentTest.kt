package one.mixin.android.ui.address

import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import one.mixin.android.R
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.util.EspressoIdlingResource
import one.mixin.android.util.swipeRight
import org.hamcrest.core.IsNot.not
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class AddressManagementFragmentTest {

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
    fun testDisplay() {
        go2AddressManagement { navController, activityScenario ->
            onView(withId(R.id.left_ib)).perform(click())
            assertTrue(navController?.currentDestination?.id == R.id.transactions_fragment)
            onView(withId(R.id.send_tv)).perform(click())
            onView(withId(R.id.address)).perform(click())

            onView(withId(R.id.right_animator)).perform(click())
            assertTrue(navController?.currentDestination?.id == R.id.address_add_fragment)
            activityScenario.onActivity {
                navController?.navigateUp()
            }
        }
    }

    @Test
    fun testAddressRV() {
        go2AddressManagement { navController, activityScenario ->
            var itemCount = 0
            activityScenario.onActivity { activity ->
                val rv = activity.findViewById<RecyclerView>(R.id.addr_rv)
                itemCount = rv.adapter?.itemCount ?: 1
            }
            if (itemCount > 0) {
                onView(withId(R.id.search_et)).check(matches(isDisplayed()))
                onView(withId(R.id.empty_tv)).check(matches(not(isDisplayed())))
                onView(withId(R.id.addr_rv))
                    .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
                onView(withId(R.id.asset_rl))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withId(R.id.left_ib)).perform(click())
                assertTrue(navController?.currentDestination?.id == R.id.address_management_fragment)

                onView(withId(R.id.addr_rv))
                    .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, swipeRight()))
                onView(withId(R.id.asset_address))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withId(R.id.right_iv)).perform(click())
                assertTrue(navController?.currentDestination?.id == R.id.address_management_fragment)
            } else {
                onView(withId(R.id.search_et)).check(matches(not(isDisplayed())))
                onView(withId(R.id.empty_tv))
                    .check(matches(isDisplayed()))
                    .check(matches(withText(R.string.withdrawal_addr_add)))
                    .perform(click())
                assertTrue(navController?.currentDestination?.id == R.id.address_add_fragment)
                activityScenario.onActivity {
                    navController?.navigateUp()
                }
            }
        }
    }

    private fun go2AddressManagement(action: (NavController?, ActivityScenario<WalletActivity>) -> Unit) {
        var navController: NavController? = null
        val activityScenario = ActivityScenario.launch(WalletActivity::class.java).onActivity {
            navController = it.navController
        }
        onView(withId(R.id.coins_rv))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))
        onView(withId(R.id.send_tv)).perform(click())
        onView(withId(R.id.send_cancel))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withId(R.id.address)).perform(click())

        action.invoke(navController, activityScenario)

        activityScenario.close()
    }
}
