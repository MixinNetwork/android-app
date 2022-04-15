@file:Suppress("DEPRECATION")

package one.mixin.android.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.biometric.BiometricPrompt
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import com.mattprecious.swirl.SwirlView
import one.mixin.android.Constants
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

    private lateinit var fingerprintManager: FingerprintManagerCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fingerprintManager = FingerprintManagerCompat.from(this)

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
    }

    override fun onBackPressed() {
        super.onBackPressed()
        pressHome()
    }

    private fun showPrompt() {
        fingerprintManager.authenticate(null, 0, null, authCallback, null)
    }

    private fun pressHome() {
        try {
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                startActivity(this)
            }
        } catch (e: Exception) {
            reportException("AppAuth pressHome", e)
        }
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
        binding.info.text = getString(R.string.fingerprint_confirm)
        binding.info.setTextColor(colorFromAttribute(R.attr.text_minor))
        binding.swirl.setState(SwirlView.State.ON)
    }

    private val showPromptRunnable = Runnable {
        showPrompt()
    }

    private val authCallback = object : FingerprintManagerCompat.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (errorCode == BiometricPrompt.ERROR_CANCELED ||
                errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
            ) {
                if (errorCode == BiometricPrompt.ERROR_CANCELED) {
                    reportException(IllegalStateException("Unlock app meet $errString"))
                }
                pressHome()
            } else if (errorCode == BiometricPrompt.ERROR_LOCKOUT ||
                errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT
            ) {
                showError(errString)
            } else if (errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS) {
                defaultSharedPreferences.putInt(Constants.Account.PREF_APP_AUTH, -1)
                defaultSharedPreferences.putLong(Constants.Account.PREF_APP_ENTER_BACKGROUND, 0)
                finishAndCheckNeed2GoUrlInterpreter()
            } else {
                refreshSwirl(errString, true)
            }
        }

        override fun onAuthenticationFailed() {
            refreshSwirl(getString(R.string.not_recognized), false)
        }

        override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
            finishAndCheckNeed2GoUrlInterpreter()
        }
    }
}
