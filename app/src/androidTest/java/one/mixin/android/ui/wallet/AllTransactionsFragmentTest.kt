package one.mixin.android.ui.wallet

import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import one.mixin.android.R
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class AllTransactionsFragmentTest : BaseTransactionsFragmentTest() {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val walletRule = WalletRule()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testClickItem() {
        go2Transactions { navController, _ ->
            onView(withId(R.id.transactions_rv))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
            assertTrue(navController?.currentDestination?.id == R.id.transaction_fragment)
        }
    }

    override fun go2Transactions(action: (NavController?, ActivityScenario<WalletActivity>) -> Unit) {
        var navController: NavController? = null
        walletRule.activityScenario = ActivityScenario.launch(WalletActivity::class.java).onActivity {
            navController = it.navController
        }

        onView(withId(R.id.right_animator)).perform(click())
        onView(withId(R.id.transactions_tv)).perform(click())
        assertTrue(navController?.currentDestination?.id == R.id.all_transactions_fragment)

        action.invoke(navController, walletRule.activityScenario)
    }

    override fun isAllTransactions() = true
}
