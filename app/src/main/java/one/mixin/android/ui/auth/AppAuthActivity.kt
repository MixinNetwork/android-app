@file:Suppress("DEPRECATION")

package one.mixin.android.ui.auth

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import androidx.core.content.getSystemService
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.core.os.CancellationSignal
import com.mattprecious.swirl.SwirlView
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.databinding.ActivityAppAuthBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putInt
import one.mixin.android.extension.putLong
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.util.reportException

class AppAuthActivity : BaseActivity() {
    companion object {
        fun show(activity: Activity) {
            Intent(activity, AppAuthActivity::class.java).apply {
                data = if (activity is UrlInterpreterActivity) activity.intent.data else null
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(this)
            }
        }
    }

    private lateinit var binding: ActivityAppAuthBinding

    private var fingerprintManager: FingerprintManagerCompat? = null

    private var cancellationSignal: CancellationSignal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fingerprintManager = FingerprintManagerCompat.from(MixinApplication.appContext)

        binding.swirl.setState(SwirlView.State.ON)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        binding.swirl.setState(SwirlView.State.ON)
    }

    override fun onStart() {
        super.onStart()
        binding.swirl.postDelayed(showPromptRunnable, 100)
    }

    override fun onStop() {
        super.onStop()
        binding.swirl.removeCallbacks(resetSwirlRunnable)
        binding.swirl.removeCallbacks(showPromptRunnable)
        cancellationSignal?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        fingerprintManager = null
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val am = getSystemService<ActivityManager>()
        if (am != null && am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_PINNED) return

        finishAffinity()
    }

    private fun showPrompt() {
        cancellationSignal?.cancel()
        cancellationSignal = CancellationSignal()
        fingerprintManager?.authenticate(null, 0, cancellationSignal, authCallback, null)
    }

    private fun refreshSwirl(errString: CharSequence, show: Boolean) {
        showError(errString)
        binding.swirl.postDelayed(resetSwirlRunnable, 1000)
        if (show) {
            binding.swirl.postDelayed(showPromptRunnable, 1000)
        }
    }

    private fun showError(errString: CharSequence) {
        binding.info.text = errString
        binding.info.setTextColor(getColor(R.color.colorRed))
        binding.swirl.setState(SwirlView.State.ERROR)
    }

    private fun finishAndCheckNeed2GoUrlInterpreter() {
        val data = intent.data
        if (data != null) {
            UrlInterpreterActivity.show(this, data)
        }
        finish()
    }

    private val resetSwirlRunnable = Runnable {
        binding.info.text = getString(R.string.Confirm_fingerprint)
        binding.info.setTextColor(colorFromAttribute(R.attr.text_minor))
        binding.swirl.setState(SwirlView.State.ON)
    }

    private val showPromptRunnable = Runnable {
        showPrompt()
    }

    private val authCallback = object : FingerprintManagerCompat.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            when (errorCode) {
                FingerprintManager.FINGERPRINT_ERROR_CANCELED, FingerprintManager.FINGERPRINT_ERROR_USER_CANCELED -> {
                    // Left empty
                }
                FingerprintManager.FINGERPRINT_ERROR_LOCKOUT, FingerprintManager.FINGERPRINT_ERROR_LOCKOUT_PERMANENT -> {
                    showError(errString)
                }
                FingerprintManager.FINGERPRINT_ERROR_NO_FINGERPRINTS -> {
                    defaultSharedPreferences.putInt(Constants.Account.PREF_APP_AUTH, -1)
                    defaultSharedPreferences.putLong(Constants.Account.PREF_APP_ENTER_BACKGROUND, 0)
                    finishAndCheckNeed2GoUrlInterpreter()
                }
                else -> {
                    refreshSwirl(errString, true)
                }
            }
            reportException(IllegalStateException("Unlock app meet $errorCode, $errString"))
        }

        override fun onAuthenticationFailed() {
            refreshSwirl(getString(R.string.Not_recognized), false)
        }

        override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
            finishAndCheckNeed2GoUrlInterpreter()
        }

        override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence?) {
            refreshSwirl(helpString.toString(), true)
        }
    }
}
