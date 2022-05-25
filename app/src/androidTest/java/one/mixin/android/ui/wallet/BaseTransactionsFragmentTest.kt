package one.mixin.android.ui.wallet

import android.content.Context
import android.widget.TextView
import androidx.navigation.NavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import one.mixin.android.R
import one.mixin.android.util.EspressoIdlingResource
import one.mixin.android.util.waitMillis
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.absoluteValue

abstract class BaseTransactionsFragmentTest {

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @Test
    fun testFilter() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val filterId = if (isAllTransactions()) R.id.right_animator else R.id.group_info_member_title_sort
        val currentId = if (isAllTransactions()) R.id.all_transactions_fragment else R.id.transactions_fragment
        go2Transactions { navController, activityScenario ->
            // sort by amount
            onView(withId(filterId)).perform(click())
            onView(withId(R.id.apply_tv))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
            onView(withId(R.id.sort_amount)).perform(click())
            onView(withId(R.id.apply_tv)).perform(click())
            assertTrue(navController?.currentDestination?.id == currentId)

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

            // TODO test sort by time

            // filter by transfer
            showFilterBottom(navController, R.id.filters_radio_transfer)
            checkTransitionName(activityScenario, ctx.getString(R.string.action_transfer))

            // filter by deposit
            showFilterBottom(navController, R.id.filters_radio_deposit)
            checkTransitionName(activityScenario, ctx.getString(R.string.filters_deposit))

            // filter by withdrawal
            showFilterBottom(navController, R.id.filters_radio_withdrawal)
            checkTransitionName(activityScenario, ctx.getString(R.string.withdrawal))

            // filter by fee
            showFilterBottom(navController, R.id.filters_radio_fee)
            checkTransitionName(activityScenario, ctx.getString(R.string.fee))

            // filter by rebate
            showFilterBottom(navController, R.id.filters_radio_rebate)
            checkTransitionName(activityScenario, ctx.getString(R.string.action_rebate))

            // filter by raw
            showFilterBottom(navController, R.id.filters_radio_raw)
            checkTransitionName(activityScenario, ctx.getString(R.string.filters_raw))
        }
    }

    private fun showFilterBottom(navController: NavController?, filterId: Int) {
        val filterIconId = if (isAllTransactions()) R.id.right_animator else R.id.group_info_member_title_sort
        val currentId = if (isAllTransactions()) R.id.all_transactions_fragment else R.id.transactions_fragment
        onView(withId(filterIconId)).perform(click())
        onView(withId(R.id.apply_tv))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withId(filterId)).perform(click())
        onView(withId(R.id.apply_tv)).perform(click())
        assertTrue(navController?.currentDestination?.id == currentId)

        // for paging reload
        if (isAllTransactions()) {
            waitMillis(1000)
        }
    }

    private fun checkTransitionName(activityScenario: ActivityScenario<WalletActivity>, expect: String) {
        activityScenario.onActivity {
            val rv = it.findViewById<RecyclerView>(R.id.transactions_rv)
            val lm = rv.layoutManager as LinearLayoutManager
            val first = lm.findFirstVisibleItemPosition() + if (isAllTransactions()) 0 else 1 // ignore header
            val last = lm.findLastVisibleItemPosition()
            for (i in first..last) {
                val textView = rv.findViewHolderForAdapterPosition(i)?.itemView?.findViewById<TextView>(R.id.name)
                    ?: continue
                val targetName = textView.text?.toString()
                val confirmationString = it.resources.getQuantityString(R.plurals.pending_confirmations, 0, 0, 10).split(' ')[1]
                if (targetName != null && targetName.endsWith(confirmationString)) { // ignore pending
                    continue
                }
                assertTrue(expect == targetName)
            }
        }
    }

    abstract fun go2Transactions(action: (NavController?, ActivityScenario<WalletActivity>) -> Unit)
    abstract fun isAllTransactions(): Boolean
}
