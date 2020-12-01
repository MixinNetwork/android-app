package one.mixin.android.ui.wallet

import android.content.Context
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import one.mixin.android.R
import one.mixin.android.util.EspressoIdlingResource
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class SingleFriendSelectFragmentTest {
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
        go2SingleFriendSelect { _, _ ->
            onView(withId(R.id.title_tv))
                .check(matches(withText(ctx.getString(R.string.transfer_to))))

            onView(withId(R.id.search_et))
                .check(matches(ViewMatchers.withHint(ctx.getString(R.string.contact_search_hint))))
                .check(matches(withText("")))
        }
    }

    @Test
    fun testSearchET() {
    }

    @Test
    fun testRv() {
        go2SingleFriendSelect { navController, _ ->
            onView(withId(R.id.friends_rv))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
            onView(withId(R.id.asset_rl))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))

            onView(withId(R.id.left_ib)).perform(click())

            assertTrue(navController?.currentDestination?.id == R.id.transactions_fragment)
        }
    }

    private fun go2SingleFriendSelect(action: (NavController?, ActivityScenario<WalletActivity>) -> Unit) {
        var navController: NavController? = null
        walletRule.activityScenario = ActivityScenario.launch(WalletActivity::class.java).onActivity {
            navController = it.navController
        }
        onView(withId(R.id.coins_rv))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))
        onView(withId(R.id.send_tv)).perform(click())
        onView(withId(R.id.send_cancel))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withId(R.id.contact)).perform(click())

        action.invoke(navController, walletRule.activityScenario)
    }
}
