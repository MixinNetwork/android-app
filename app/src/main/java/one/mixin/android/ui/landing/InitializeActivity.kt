package one.mixin.android.ui.landing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.crypto.PrivacyPreference.putIsLoaded
import one.mixin.android.databinding.ActivityLandingBinding
import one.mixin.android.extension.replaceFragment
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.landing.UpgradeFragment.Companion.TYPE_DB
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class InitializeActivity : BaseActivity() {
    private val binding by viewBinding(ActivityLandingBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val setName = intent.getBooleanExtra(SET_NAME, false)
        val setPin = intent.getBooleanExtra(SET_PIN, false)
        val wrongTime = intent.getBooleanExtra(WRONG_TIME, false)
        val oldVersion = intent.getBooleanExtra(OLD_VERSION, false)
        val dbUpgrade = intent.getBooleanExtra(DB_UPGRADE, false)
        when {
            setName -> replaceFragment(SetupNameFragment.newInstance(), R.id.container)
            setPin -> replaceFragment(SetupPinFragment.newInstance(), R.id.container)
            wrongTime -> replaceFragment(TimeFragment.newInstance(), R.id.container)
            oldVersion -> replaceFragment(OldVersionFragment.newInstance(), R.id.container)
            dbUpgrade -> replaceFragment(UpgradeFragment.newInstance(TYPE_DB), R.id.container)
            else ->
                replaceFragment(
                    LoadingFragment.newInstance(),
                    R.id.container,
                    LoadingFragment.TAG,
                )
        }
    }

    override fun onBackPressed() {
    }

    companion object {
        const val SET_NAME = "set_name"
        const val SET_PIN = "set_pin"
        const val WRONG_TIME = "wrong_time"
        const val OLD_VERSION = "old_version"
        const val DB_UPGRADE = "db_upgrade"

        private fun getIntent(
            context: Context,
            setName: Boolean = false,
            setPin: Boolean = false,
            wrongTime: Boolean = false,
            oldVersion: Boolean = false,
            dbUpgrade: Boolean = false,
        ): Intent {
            return Intent(context, InitializeActivity::class.java).apply {
                this.putExtra(SET_NAME, setName)
                this.putExtra(SET_PIN, setPin)
                this.putExtra(WRONG_TIME, wrongTime)
                this.putExtra(OLD_VERSION, oldVersion)
                this.putExtra(DB_UPGRADE, dbUpgrade)
            }
        }

        fun showWongTime(context: Context) {
            context.startActivity(getIntent(context, wrongTime = true))
        }

        fun showWongTimeTop(context: Context) {
            context.startActivity(
                getIntent(context, wrongTime = true).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                },
            )
        }

        fun showOldVersionAlert(context: Context) {
            context.startActivity(
                getIntent(context, oldVersion = true).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                },
            )
        }

        fun showLoading(
            context: Context,
            load: Boolean = true,
            clear: Boolean = false,
        ) {
            if (load) {
                putIsLoaded(context, false)
            }
            context.startActivity(
                getIntent(context).apply {
                    if (clear) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                },
            )
        }

        fun showSetupName(context: Context) {
            context.startActivity(getIntent(context, setName = true))
        }

        fun showSetupPin(context: Context) {
            context.startActivity(getIntent(context, setPin = true))
        }

        fun showDBUpgrade(context: Context) {
            context.startActivity(getIntent(context, dbUpgrade = true))
        }
    }
}
