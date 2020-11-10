package one.mixin.android.ui.wallet

import android.content.Context
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.NoMatchingViewException
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
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putLong
import one.mixin.android.util.EspressoIdlingResource
import one.mixin.android.util.swipeRight
import one.mixin.android.util.waitMillis
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class WalletFragmentTest {

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
    fun testShowCheckPin() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        ctx.defaultSharedPreferences.putLong(Constants.Account.PREF_PIN_CHECK, 0)

        val activityScenario = ActivityScenario.launch(WalletActivity::class.java)

        onView(withId(R.id.top_ll))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))

        activityScenario.close()
    }

    @Test
    fun testNotShowCheckPin() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        ctx.defaultSharedPreferences.putLong(Constants.Account.PREF_PIN_CHECK, System.currentTimeMillis())

        val activityScenario = ActivityScenario.launch(WalletActivity::class.java)

        try {
            onView(withId(R.id.top_ll))
                .check(matches(isDisplayed()))
            fail("Should not show PinCheckDialogFragment")
        } catch (e: NoMatchingViewException) {
            // true
        }

        activityScenario.close()
    }

    @Test
    fun testOpenWalletSearch() {
        var navController: NavController? = null
        val activityScenario = ActivityScenario.launch(WalletActivity::class.java).onActivity {
            navController = it.navController
        }

        onView(withId(R.id.search_ib)).perform(click())
        assertTrue(navController?.currentDestination?.id == R.id.wallet_search_fragment)

        activityScenario.close()
    }

    @Test
    fun testBottomMenuNavigate() {
        var navController: NavController? = null
        val activityScenario = ActivityScenario.launch(WalletActivity::class.java).onActivity {
            navController = it.navController
        }

        // open HiddenAssetFragment
        onView(withId(R.id.right_animator)).perform(click())
        onView(withId(R.id.hide))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withId(R.id.hide)).perform(click())
        assertTrue(navController?.currentDestination?.id == R.id.hidden_assets_fragment)
        activityScenario.onActivity {
            navController?.navigateUp()
        }

        // open AllTransactionsFragment
        onView(withId(R.id.right_animator)).perform(click())
        onView(withId(R.id.transactions_tv))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withId(R.id.transactions_tv)).perform(click())
        assertTrue(navController?.currentDestination?.id == R.id.all_transactions_fragment)

        activityScenario.close()
    }

    @Test
    fun testAssetRv() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        var navController: NavController? = null
        val activityScenario = ActivityScenario.launch(WalletActivity::class.java).onActivity { activity ->
            navController = activity.navController
        }

        // open first asset item
        onView(
            withId(R.id.coins_rv)
        )
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))
        assertTrue(navController?.currentDestination?.id == R.id.transactions_fragment)
        activityScenario.onActivity {
            navController?.navigateUp()
        }

        // swipe asset item
        onView(withId(R.id.coins_rv))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(1, swipeRight()))
        onView(withId(com.google.android.material.R.id.snackbar_action))
            .check(matches(withText(ctx.getString(R.string.undo_capital))))

        waitMillis(1000)
        onView(withId(com.google.android.material.R.id.snackbar_action))
            .perform(click())

        activityScenario.close()
    }
}
