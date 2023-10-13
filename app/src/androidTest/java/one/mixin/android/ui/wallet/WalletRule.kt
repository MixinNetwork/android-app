package one.mixin.android.ui.wallet

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import one.mixin.android.AndroidTestApplication_Application
import one.mixin.android.Constants
import one.mixin.android.crypto.PrivacyPreference
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putLong
import one.mixin.android.job.MixinJobManager
import one.mixin.android.mock.MOCK_ADDRESSES_JSON
import one.mixin.android.mock.MOCK_ASSETS_JSON
import one.mixin.android.mock.MOCK_ASSET_EXTRA_LIST_JSON
import one.mixin.android.mock.MOCK_SNAPSHOTS
import one.mixin.android.mock.MOCK_TOP_ASSETS_JSON
import one.mixin.android.mock.MOCK_USERS_JSON
import one.mixin.android.mock.mockAccount
import one.mixin.android.session.Session
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.Address
import one.mixin.android.vo.Asset
import one.mixin.android.vo.AssetsExtra
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.TopAsset
import one.mixin.android.vo.User
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class WalletRule : TestRule {

    lateinit var db: MixinDatabase
    lateinit var activityScenario: ActivityScenario<WalletActivity>

    override fun apply(base: Statement?, description: Description?) = WalletStatement(base)

    inner class WalletStatement(private val base: Statement?) : Statement() {
        override fun evaluate() {
            val ctx = ApplicationProvider.getApplicationContext<AndroidTestApplication_Application>()
            db = MixinDatabase.getDatabase(ctx)

            fun initAssets() {
                val assets = GsonHelper.customGson.fromJson(MOCK_ASSETS_JSON, Array<Asset>::class.java)
                db.assetDao().insert(*assets)
            }

            fun initAssetExtras() {
                val assetsExtras = GsonHelper.customGson.fromJson(MOCK_ASSET_EXTRA_LIST_JSON, Array<AssetsExtra>::class.java)
                db.assetsExtraDao().insert(*assetsExtras)
            }

            fun initTopAssets() {
                val topAssets = GsonHelper.customGson.fromJson(MOCK_TOP_ASSETS_JSON, Array<TopAsset>::class.java)
                db.topAssetDao().insert(*topAssets)
            }

            fun initSnapshots() {
                val snapshots = GsonHelper.customGson.fromJson(MOCK_SNAPSHOTS, Array<Snapshot>::class.java)
                db.snapshotDao().insert(*snapshots)
            }

            fun initAddresses() {
                val addresses = GsonHelper.customGson.fromJson(MOCK_ADDRESSES_JSON, Array<Address>::class.java)
                db.addressDao().insert(*addresses)
            }

            fun initAccount() {
                val account = mockAccount()
                Session.storeAccount(account)
            }

            fun initUsers() {
                val users = GsonHelper.customGson.fromJson(MOCK_USERS_JSON, Array<User>::class.java)
                db.userDao().insert(*users)
            }

            fun updatePin() {
                ctx.defaultSharedPreferences.putLong(Constants.Account.PREF_PIN_CHECK, System.currentTimeMillis())
                PrivacyPreference.putPrefPinInterval(ctx, Constants.INTERVAL_24_HOURS)
            }

            try {
                initAccount()
                initAssets()
                initTopAssets()
                initSnapshots()
                initAddresses()
                initAssetExtras()
                initUsers()
                updatePin()

                base?.evaluate()
            } finally {
                if (::db.isInitialized) {
                    db.clearAllTables()
                }
                if (::activityScenario.isInitialized) {
                    var jobManager: MixinJobManager? = null
                    activityScenario.onActivity {
                        jobManager = it.jobManager
                    }
                    jobManager?.apply {
                        cancelAllJob()
                        clear()
                    }
                    activityScenario.close()
                }
            }
        }
    }
}
