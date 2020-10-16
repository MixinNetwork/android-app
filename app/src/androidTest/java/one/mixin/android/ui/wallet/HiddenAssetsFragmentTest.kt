package one.mixin.android.ui.wallet

import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import one.mixin.android.util.EspressoIdlingResource
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class HiddenAssetsFragmentTest {

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
    fun testClickItem() {
        // launchFragmentInHiltContainer(HiddenAssetsFragment(), R.style.AppTheme_NoActionBar)
        //
        // onView(withId(R.id.assets_rv))
        //     .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
        // assertTrue(navController?.currentDestination?.id == R.id.transactions_fragment)
    }

    @Test
    fun testSwipeItem() {
        // val ctx: Context = ApplicationProvider.getApplicationContext()
        // val activityScenario = ActivityScenario.launch(WalletActivity::class.java)
        // launchFragmentInHiltContainer(HiddenAssetsFragment())
        //
        // // swipe asset item
        // onView(withId(R.id.assets_rv))
        //     .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, swipeRight()))
        // onView(withId(com.google.android.material.R.id.snackbar_action))
        //     .check(matches(withText(ctx.getString(R.string.undo_capital))))
        //
        // EspressoIdlingResource.increment()
        // val job = GlobalScope.launch {
        //     delay(1000)
        // }
        // job.invokeOnCompletion {
        //     EspressoIdlingResource.decrement()
        // }
        // onView(withId(com.google.android.material.R.id.snackbar_action))
        //     .perform(click())
        //
        // activityScenario.close()
    }
}
