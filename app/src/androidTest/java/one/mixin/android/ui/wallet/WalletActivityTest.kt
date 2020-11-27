package one.mixin.android.ui.wallet

import android.content.Intent
import androidx.navigation.NavController
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import one.mixin.android.R
import one.mixin.android.mock.mockAsset
import one.mixin.android.ui.wallet.WalletActivity.Companion.ASSET
import one.mixin.android.vo.toAssetItem
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class WalletActivityTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testNavigationStartDestination() {
        var navController: NavController? = null
        val activityScenario = ActivityScenario.launch(WalletActivity::class.java).onActivity {
            navController = it.navController
        }

        assertTrue(navController?.graph?.startDestination?.equals(R.id.transactions_fragment) == false)
        assertTrue(navController?.graph?.startDestination?.equals(R.id.wallet_fragment) == true)

        activityScenario.close()
    }

    @Test
    fun testNavigationStartDestinationWithAsset() {
        val asset = mockAsset().toAssetItem()

        val intent = Intent(ApplicationProvider.getApplicationContext(), WalletActivity::class.java).apply {
            putExtra(ASSET, asset)
        }
        var navController: NavController? = null
        val activityScenario = ActivityScenario.launch<WalletActivity>(intent).onActivity {
            navController = it.navController
        }

        assertTrue(navController?.graph?.startDestination?.equals(R.id.transactions_fragment) == true)
        assertTrue(navController?.graph?.startDestination?.equals(R.id.wallet_fragment) == false)

        activityScenario.close()
    }
}
