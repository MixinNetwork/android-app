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
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import one.mixin.android.R
import one.mixin.android.extension.getRVCount
import one.mixin.android.util.EspressoIdlingResource
import one.mixin.android.util.swipeRight
import one.mixin.android.util.waitMillis
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class HiddenAssetsFragmentTest {

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
    fun testClickItem() {
        go2Hidden { navController, activityScenario ->
            if (activityScenario.getRVCount(R.id.assets_rv) > 0) {
                onView(withId(R.id.assets_rv))
                    .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
                assertTrue(navController?.currentDestination?.id == R.id.transactions_fragment)
            }
        }
    }

    @Test
    fun testSwipeItem() {
        go2Hidden { _, _ ->
            val ctx: Context = ApplicationProvider.getApplicationContext()
            onView(withId(R.id.assets_rv))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, swipeRight()))
            onView(withId(com.google.android.material.R.id.snackbar_action))
                .check(matches(withText(ctx.getString(R.string.undo_capital))))

            waitMillis(1000)
            onView(withId(com.google.android.material.R.id.snackbar_action))
                .perform(click())
        }
    }

    private fun go2Hidden(action: (NavController?, ActivityScenario<WalletActivity>) -> Unit) {
        var navController: NavController? = null
        val activityScenario = ActivityScenario.launch(WalletActivity::class.java).onActivity {
            navController = it.navController
        }

        onView(withId(R.id.right_animator)).perform(click())
        onView(withId(R.id.hide)).perform(click())
        assertTrue(navController?.currentDestination?.id == R.id.hidden_assets_fragment)

        action.invoke(navController, activityScenario)

        activityScenario.close()
    }
}
