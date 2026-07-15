package one.mixin.android.ui.home.web3.trade.perps

import android.os.Bundle
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import one.mixin.android.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PerpsRouteNavigatorTest {
    @Test
    fun routeTransactionAddsBackStackEntry() {
        val activity = Robolectric.buildActivity(TestActivity::class.java).setup().get()
        val fragmentManager = activity.supportFragmentManager

        val method = PerpsRouteNavigator::class.java.getDeclaredMethod(
            "addRoute",
            FragmentManager::class.java,
            Fragment::class.java,
            String::class.java,
        )
        method.isAccessible = true
        method.invoke(PerpsRouteNavigator, fragmentManager, Fragment(), "test_route")
        fragmentManager.executePendingTransactions()

        assertEquals(1, fragmentManager.backStackEntryCount)
    }

    class TestActivity : FragmentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(
                FrameLayout(this).apply {
                    id = R.id.container
                }
            )
        }
    }
}
