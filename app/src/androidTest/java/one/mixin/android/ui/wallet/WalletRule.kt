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
import one.mixin.android.moshi.MoshiHelper.getTypeListAdapter
import one.mixin.android.session.Session
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
                val assets = getTypeListAdapter<List<Asset>>(Asset::class.java).fromJson(MOCK_ASSETS_JSON)?.toTypedArray()
                db.assetDao().insert(*assets)
            }

            fun initAssetExtras() {
                val assetsExtras = getTypeListAdapter<List<AssetsExtra>>(AssetsExtra::class.java).fromJson(MOCK_ASSET_EXTRA_LIST_JSON)?.toTypedArray()
                db.assetsExtraDao().insert(*assetsExtras)
            }

            fun initTopAssets() {
                val topAssets = getTypeListAdapter<List<TopAsset>>(TopAsset::class.java).fromJson(MOCK_TOP_ASSETS_JSON)?.toTypedArray()
                db.topAssetDao().insert(*topAssets)
            }

            fun initSnapshots() {
                val snapshots = getTypeListAdapter<List<Snapshot>>(Snapshot::class.java).fromJson(MOCK_SNAPSHOTS)?.toTypedArray()
                db.snapshotDao().insert(*snapshots)
            }

            fun initAddresses() {
                val addresses = getTypeListAdapter<List<Address>>(Address::class.java).fromJson(MOCK_ADDRESSES_JSON)?.toTypedArray()
                db.addressDao().insert(*addresses)
            }

            fun initAccount() {
                val account = mockAccount()
                Session.storeAccount(account)
            }

            fun initUsers() {
                val users = getTypeListAdapter<List<User>>(User::class.java).fromJson(MOCK_USERS_JSON)?.toTypedArray()
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
