package one.mixin.android.ui.landing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig
import one.mixin.android.Constants.APP_VERSION
import one.mixin.android.R
import one.mixin.android.databinding.ActivityLandingBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putInt
import one.mixin.android.extension.replaceFragment
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.landing.MobileFragment.Companion.FROM_CHANGE_PHONE_ACCOUNT
import one.mixin.android.ui.landing.MobileFragment.Companion.FROM_VERIFY_MOBILE_REMINDER
import one.mixin.android.ui.landing.MobileFragment.Companion.ARGS_PHONE_NUM
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.viewBinding
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LandingActivity : BaseActivity() {
    companion object {
        const val ARGS_PIN = "args_pin"
        const val ARGS_FROM = "args_from"

        fun show(context: Context) {
            val intent =
                Intent(context, LandingActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            context.startActivity(intent)
        }

        fun show(
            context: Context,
            pinCode: String,
        ) {
            val intent =
                Intent(context, LandingActivity::class.java).apply {
                    putExtra(ARGS_PIN, pinCode)
                }
            context.startActivity(intent)
        }
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    private val binding by viewBinding(ActivityLandingBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSystemUi = true
        super.onCreate(savedInstanceState)
        SystemUIManager.setSafePadding(window, color = colorFromAttribute(R.attr.bg_white), imePadding = true)
        checkVersion()
        setContentView(binding.root)
        val pin = intent.getStringExtra(ARGS_PIN)
        val from = intent.getIntExtra(ARGS_FROM, -1)
        val phoneNumber = intent.getStringExtra(ARGS_PHONE_NUM)
        val fragment =
            if (pin != null) {
                MobileFragment.newInstance(pin, FROM_CHANGE_PHONE_ACCOUNT)
            } else if (from == FROM_CHANGE_PHONE_ACCOUNT) {
                MobileFragment.newInstance(from = FROM_CHANGE_PHONE_ACCOUNT)
            } else if (from == FROM_VERIFY_MOBILE_REMINDER) {
                MobileFragment.newInstance(from = FROM_VERIFY_MOBILE_REMINDER, phoneNumber = phoneNumber)
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    jobManager.clear()
                }
                LandingFragment.newInstance()
            }
        replaceFragment(fragment, R.id.container)
    }

    private fun checkVersion(){
        val saveVersion = defaultSharedPreferences.getInt(APP_VERSION, -1)
        if (saveVersion != BuildConfig.VERSION_CODE) {
            if (saveVersion != -1) {
                Timber.e("Old Version: $saveVersion")
            }
            Timber.e("Current Version: Mixin${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})")
            defaultSharedPreferences.putInt(APP_VERSION, BuildConfig.VERSION_CODE)
        }
    }
}
