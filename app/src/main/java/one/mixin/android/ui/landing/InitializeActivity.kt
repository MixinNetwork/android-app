package one.mixin.android.ui.landing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.crypto.PrivacyPreference.putIsLoaded
import one.mixin.android.extension.replaceFragment
import one.mixin.android.ui.common.BaseActivity

@AndroidEntryPoint
class InitializeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)
        val setName = intent.getBooleanExtra(SET_NAME, false)
        val wrongTime = intent.getBooleanExtra(WRONG_TIME, false)
        val ftsUpgrade = intent.getBooleanExtra(FTS_UPGRADE, false)
        val oldVersion = intent.getBooleanExtra(OLD_VERSION, false)
        when {
            setName -> replaceFragment(SetupNameFragment.newInstance(), R.id.container)
            wrongTime -> replaceFragment(TimeFragment.newInstance(), R.id.container)
            oldVersion -> replaceFragment(OldVersionFragment.newInstance(), R.id.container)
            ftsUpgrade -> replaceFragment(UpgradeFragment.newInstance(), R.id.container)
            else -> replaceFragment(
                LoadingFragment.newInstance(),
                R.id.container,
                LoadingFragment.TAG
            )
        }
    }

    override fun onBackPressed() {
    }

    companion object {
        const val SET_NAME = "set_name"
        const val WRONG_TIME = "wrong_time"
        const val FTS_UPGRADE = "fts_upgrade"
        const val OLD_VERSION = "old_version"
        private fun getIntent(
            context: Context,
            setName: Boolean,
            wrongTime: Boolean = false,
            ftsUpgrade: Boolean = false,
            oldVersion: Boolean = false
        ): Intent {
            return Intent(context, InitializeActivity::class.java).apply {
                this.putExtra(SET_NAME, setName)
                this.putExtra(WRONG_TIME, wrongTime)
                this.putExtra(FTS_UPGRADE, ftsUpgrade)
                this.putExtra(OLD_VERSION, oldVersion)
            }
        }

        fun showWongTime(context: Context) {
            context.startActivity(getIntent(context, setName = false, wrongTime = true))
        }

        fun showWongTimeTop(context: Context) {
            context.startActivity(
                getIntent(context, setName = false, wrongTime = true).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            )
        }

        fun showOldVersionAlert(context: Context) {
            context.startActivity(
                getIntent(context, setName = false, wrongTime = false, oldVersion = true).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            )
        }

        fun showLoading(context: Context, load: Boolean = true) {
            if (load) {
                putIsLoaded(context, false)
            }
            context.startActivity(getIntent(context, setName = false, wrongTime = false, oldVersion = false))
        }

        fun showSetupName(context: Context) {
            context.startActivity(getIntent(context, setName = true, wrongTime = false, oldVersion = false))
        }

        fun showFts(context: Context) {
            context.startActivity(
                getIntent(
                    context,
                    setName = false,
                    wrongTime = false,
                    ftsUpgrade = true,
                    oldVersion = false
                )
            )
        }
    }
}
