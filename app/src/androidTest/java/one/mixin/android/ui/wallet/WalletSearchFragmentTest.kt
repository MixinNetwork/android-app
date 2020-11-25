package one.mixin.android.ui.wallet

import android.content.Context
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isFocused
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import one.mixin.android.Constants.Account.PREF_RECENT_SEARCH_ASSETS
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getRVCount
import one.mixin.android.extension.putString
import one.mixin.android.util.ClickDrawableAction
import one.mixin.android.util.EspressoIdlingResource
import one.mixin.android.util.isKeyboardShown
import one.mixin.android.util.waitMillis
import org.hamcrest.core.IsNot.not
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class WalletSearchFragmentTest {

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
    fun testDisplay() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        go2Search { navController, _ ->
            onView(withId(R.id.search_et))
                .check(matches(isFocused()))
                .check(matches(withHint(ctx.getString(R.string.wallet_search_hint))))
                .check(matches(withText("")))
            assertTrue(isKeyboardShown())

            onView(withId(R.id.default_rv)).check(matches(isDisplayed()))
            onView(withId(R.id.search_rv)).check(matches(not(isDisplayed())))

            onView(withId(R.id.back_ib)).perform(click())
            assertTrue(navController?.currentDestination?.id == R.id.wallet_fragment)
        }
    }

    @Test
    fun testSearchET() {
        go2Search { _, _ ->
            onView(withId(R.id.search_et)).perform(typeText("btc"))

            onView(withId(R.id.default_rv)).check(matches(isDisplayed()))
            onView(withId(R.id.search_rv)).check(matches(not(isDisplayed())))
            onView(withId(R.id.pb)).check(matches(not(isDisplayed())))
            onView(withId(R.id.empty_ll)).check(matches(not(isDisplayed())))

            waitMillis(200)

            onView(withId(R.id.default_rv)).check(matches(isDisplayed()))
            onView(withId(R.id.search_rv)).check(matches(not(isDisplayed())))
            onView(withId(R.id.pb)).check(matches(not(isDisplayed())))
            onView(withId(R.id.empty_ll)).check(matches(not(isDisplayed())))

            waitMillis(500)

            onView(withId(R.id.default_rv)).check(matches(not(isDisplayed())))
            onView(withId(R.id.search_rv)).check(matches(isDisplayed()))
            onView(withId(R.id.pb)).check(matches(isDisplayed()))
            onView(withId(R.id.empty_ll)).check(matches(not(isDisplayed())))

            onView(withId(R.id.search_et)).perform(ClickDrawableAction(ClickDrawableAction.Right))

            onView(withId(R.id.default_rv)).check(matches(not(isDisplayed())))
            onView(withId(R.id.search_rv)).check(matches(isDisplayed()))
            onView(withId(R.id.pb)).check(matches(isDisplayed()))
            onView(withId(R.id.empty_ll)).check(matches(not(isDisplayed())))

            waitMillis(500)

            onView(withId(R.id.default_rv)).check(matches(isDisplayed()))
            onView(withId(R.id.search_rv)).check(matches(not(isDisplayed())))
            onView(withId(R.id.pb)).check(matches(not(isDisplayed())))
            onView(withId(R.id.empty_ll)).check(matches(not(isDisplayed())))

            onView(withId(R.id.search_et)).perform(typeText("Can not find"))

            // waitMillis(2000) // need a better way
            //
            // onView(withId(R.id.default_rv)).check(matches(not(isDisplayed())))
            // onView(withId(R.id.search_rv)).check(matches(not(isDisplayed())))
            // onView(withId(R.id.pb)).check(matches(not(isDisplayed())))
            // onView(withId(R.id.empty_ll)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun testDefaultRv() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        go2Search { navController, activityScenario ->
            // TODO check recent search and top assets
            // var isRecentDisplayed = false
            // if (onView(withText(ctx.getString(R.string.wallet_recent_search))).isDisplayed()) {
            //     isRecentDisplayed = true
            //     onView(withId(R.id.default_rv))
            //         .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
            //     assertTrue(navController?.currentDestination?.id == R.id.transactions_fragment)
            //
            //     activityScenario.onActivity {
            //         navController?.navigateUp()
            //     }
            // }

            val count = activityScenario.getRVCount(R.id.default_rv)
            if (count > 2) {
                onView(withId(R.id.default_rv))
                    .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(2, click()))

                // onView(withId(R.id.progress))
                //     .inRoot(isDialog())
                //     .check(matches(isDisplayed()))
                // assertTrue(navController?.currentDestination?.id == R.id.transactions_fragment)
            }
        }
    }

    @Test
    fun testSearchRv() {
        go2Search { navController, activityScenario ->
            val count = activityScenario.getRVCount(R.id.search_rv)
            if (count > 0) {
                onView(withId(R.id.search_rv))
                    .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
                assertTrue(navController?.currentDestination?.id == R.id.transactions_fragment)
            }
        }
    }

    private fun go2Search(action: (NavController?, ActivityScenario<WalletActivity>) -> Unit) {
        var navController: NavController? = null
        walletRule.activityScenario = ActivityScenario.launch(WalletActivity::class.java).onActivity {
            navController = it.navController
        }

        onView(withId(R.id.search_ib)).perform(click())
        assertTrue(navController?.currentDestination?.id == R.id.wallet_search_fragment)

        action.invoke(navController, walletRule.activityScenario)
    }

    private fun mockRecentSearch() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val mockIds = arrayOf(
            "05891083-63d2-4f3d-bfbe-d14d7fb9b25a",
            "05c5ac01-31f9-4a69-aa8a-ab796de1d041",
            "2204c1ee-0ea2-4add-bb9a-b3719cfff93a",
            "23dfb5a5-5d7b-48b6-905f-3970e3176e27",
            "43d61dcd-e413-450d-80b8-101d5e903357",
            "fd11b6e3-0b87-41f1-a41f-f0e9b49e5bf0"
        )
        val recentString = mockIds.joinToString(separator = "=")
        ctx.defaultSharedPreferences.putString(PREF_RECENT_SEARCH_ASSETS, recentString)
    }
}
