package one.mixin.android.ui.landing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import one.mixin.android.Constants.Load.IS_LOADED
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.replaceFragment
import one.mixin.android.ui.common.BaseActivity

class InitializeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)
        val setName = intent.getBooleanExtra(SET_NAME, false)
        val wrongTime = intent.getBooleanExtra(WRONG_TIME, false)
        val ftsUpgrade = intent.getBooleanExtra(FTS_UPGRADE, false)
        when {
            setName -> replaceFragment(SetupNameFragment.newInstance(), R.id.container)
            wrongTime -> replaceFragment(TimeFragment.newInstance(), R.id.container)
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
        private fun getIntent(
            context: Context,
            setName: Boolean,
            wrongTime: Boolean = false,
            ftsUpgrade: Boolean = false
        ): Intent {
            return Intent(context, InitializeActivity::class.java).apply {
                this.putExtra(SET_NAME, setName)
                this.putExtra(WRONG_TIME, wrongTime)
                this.putExtra(FTS_UPGRADE, ftsUpgrade)
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

        fun showLoading(context: Context, load: Boolean = true) {
            if (load) {
                context.defaultSharedPreferences.putBoolean(IS_LOADED, false)
            }
            context.startActivity(getIntent(context, setName = false, wrongTime = false))
        }

        fun showSetupName(context: Context) {
            context.startActivity(getIntent(context, setName = true, wrongTime = false))
        }

        fun showFts(context: Context) {
            context.startActivity(
                getIntent(
                    context,
                    setName = false,
                    wrongTime = false,
                    ftsUpgrade = true
                )
            )
        }
    }
}
