package one.mixin.android.ui.wallet

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
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import one.mixin.android.R
import one.mixin.android.mock.mockAssetWithDestination
import one.mixin.android.mock.mockAssetWithDestinationAndTag
import one.mixin.android.util.EspressoIdlingResource
import one.mixin.android.util.waitMillis
import one.mixin.android.vo.toAssetItem
import org.hamcrest.core.IsNot.not
import org.junit.After
import org.junit.Before

abstract class DepositFragmentTest {

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    protected fun showDepositTipBottom() {
        waitMillis(1000)
        onView(withId(R.id.continue_tv))
            .inRoot(isDialog())
            .check(matches(not(isEnabled())))
            .check(matches(isDisplayed()))

        waitMillis(3000)
        onView(withId(R.id.continue_tv))
            .check(matches(isEnabled()))
            .perform(click())
    }

    protected fun closeTipBottom() {
        waitMillis(3000)
        onView(withId(R.id.continue_tv))
            .check(matches(isEnabled()))
            .perform(click())
    }

    protected fun go2Deposit(isAccount: Boolean, action: (NavController?, ActivityScenario<WalletActivity>) -> Unit) {
        var navController: NavController? = null
        val activityScenario = ActivityScenario.launch(WalletActivity::class.java).onActivity {
            navController = it.navController
        }
        onView(withId(R.id.coins_rv))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))
        var fragment: TransactionsFragment? = null
        activityScenario.onActivity { activity ->
            fragment = activity.supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.first() as TransactionsFragment
        }
        fragment?.asset = (if (isAccount) mockAssetWithDestinationAndTag() else mockAssetWithDestination()).toAssetItem()
        onView(withId(R.id.receive_tv)).perform(click())

        action.invoke(navController, activityScenario)

        activityScenario.close()
    }
}
